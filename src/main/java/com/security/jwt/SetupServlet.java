package com.security.jwt;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.security.jwt.service.PasswordService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.*;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.util.encoders.Base64;

/**
 * JWT 초기 설정 서블릿
 * Keystore 생성, EC256 키쌍 생성, API Key 설정
 */
@WebServlet(name = "SetupServlet", urlPatterns = {"/setup"})
public class SetupServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson gson = new Gson();
    private static final String SETUP_FLAG_FILE = "setup-completed.flag";
    private static final Logger logger = LogManager.getLogger(SetupServlet.class);

    @Override
    public void init() throws ServletException {
        // Bouncy Castle 보안 제공자 추가
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * CORS 헤더 설정
     */
    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    /**
     * OPTIONS 요청 처리 (CORS preflight)
     */
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        String action = request.getParameter("action");

        if ("backup".equals(action)) {
            // Keystore 백업
            try {
                backupKeystore(request, response);
            } catch (Exception e) {
                try {
                    sendError(response, 500, "백업 실패: " + e.getMessage());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else if ("currentPassword".equals(action)) {
            // 설정 파일에서 현재 비밀번호 읽기
            try {
                getCurrentPassword(request, response);
            } catch (Exception e) {
                try {
                    sendError(response, 500, "비밀번호 조회 실패: " + e.getMessage());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            // 초기화 상태 확인
            response.setContentType("application/json; charset=UTF-8");

            boolean isSetupCompleted = isSetupCompleted();
            
            JsonObject result = new JsonObject();
            result.addProperty("setupCompleted", isSetupCompleted);
            response.getWriter().write(result.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        String action = request.getParameter("action");

        if ("restore".equals(action)) {
            // Keystore 복원
            try {
                restoreKeystore(request, response);
            } catch (Exception e) {
                try {
                    sendError(response, 500, "복원 실패: " + e.getMessage());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            // 초기 설정 수행
            response.setContentType("application/json; charset=UTF-8");

            try {
                // 이미 초기화되었는지 확인
                if (isSetupCompleted()) {
                sendError(response, 400, "이미 초기화되었습니다");
                return;
            }

            // 비밀번호 입력 받기
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");

            if (password == null || password.isEmpty()) {
                sendError(response, 400, "비밀번호를 입력해주세요");
                return;
            }

            if (!password.equals(confirmPassword)) {
                sendError(response, 400, "비밀번호가 일치하지 않습니다");
                return;
            }

            // 1. Keystore 생성
            String keystorePath = getServletContext().getRealPath("/") + "keystore.jks";
            createKeystore(keystorePath, password);

            // 2. EC256 키쌍 생성 및 Keystore에 저장
            generateAndStoreEC256Keys(keystorePath, password);

            // 3. API Key 저장 (입력한 비밀번호를 API Key로 사용)
            storeApiKey(keystorePath, password, password);

            // 4. 초기화 완료 플래그 생성
            createSetupFlag();

            // 응답
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "초기 설정이 완료되었습니다");
            response.getWriter().write(result.toString());

        } catch (Exception e) {
            sendError(response, 500, "초기 설정 실패: " + e.getMessage());
        }
        }
    }

    /**
     * PUT: 비밀번호 변경
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        logger.info("=== doPut START ===");
        response.setContentType("application/json; charset=UTF-8");

        try {
            // PUT 요청의 body를 파싱하기 위해 HttpServletRequest를 래핑
            PutRequestWrapper wrappedRequest = new PutRequestWrapper(request);
            logger.debug("Request method: {}", wrappedRequest.getMethod());
            logger.debug("Request content type: {}", wrappedRequest.getContentType());
            
            handlePasswordChange(wrappedRequest, response);
            logger.info("=== doPut END (SUCCESS) ===");
        } catch (Exception e) {
            logger.error("Exception in doPut", e);
            sendError(response, 500, "비밀번호 변경 실패: " + e.getMessage());
            logger.info("=== doPut END (ERROR) ===");
        }
    }

    /**
     * PUT 요청 래퍼 - 요청 body를 파라미터로 파싱
     */
    private static class PutRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {
        private java.util.Map<String, String[]> parameterMap;

        public PutRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            parameterMap = new java.util.HashMap<>();
            
            // 요청 body 읽기
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            
            String body = sb.toString();
            LogManager.getLogger().debug("Request body: {}", body);
            
            // URL 디코딩된 파라미터 파싱
            if (body != null && !body.isEmpty()) {
                String[] pairs = body.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf('=');
                    if (idx > 0) {
                        String key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                        String value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                        parameterMap.put(key, new String[]{value});
                        LogManager.getLogger().debug("Parsed parameter: {} = {}", key, value);
                    }
                }
            }
        }

        @Override
        public String getParameter(String name) {
            String[] values = parameterMap.get(name);
            if (values != null && values.length > 0) {
                return values[0];
            }
            return null;
        }

        @Override
        public java.util.Map<String, String[]> getParameterMap() {
            return parameterMap;
        }
    }

    /**
     * DELETE: 강제 초기화 (관리자용)
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setContentType("application/json; charset=UTF-8");

        try {
            String webappPath = getServletContext().getRealPath("/");
            String configPath = webappPath + "jwt-config.properties";
            // 비밀번호 확인
            String password = request.getParameter("password");
            if (password == null || !PasswordService.verifyPassword(password, configPath)) {
                sendError(response, 401, "비밀번호가 일치하지 않습니다");
                return;
            }

            // 강제 초기화 확인 코드 확인
            String confirmCode = request.getParameter("confirm");
            if (confirmCode == null || !confirmCode.equals("FORCE_RESET_CONFIRMED")) {
                sendError(response, 400, "강제 초기화 확인 코드가 필요합니다");
                return;
            }

            // 기존 파일 삭제
            deleteSetupFiles(webappPath);

            // JwtServlet 서블릿 컨텍스트에 keysLoaded 플래그 리셋 요청
            // (강제 초기화 후 다시 초기화될 때까지 JWT 생성 불가)
            getServletContext().setAttribute("jwt_keys_loaded", false);

            // 응답
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "초기화가 완전히 리셋되었습니다. 페이지를 새로고침해주세요.");
            response.getWriter().write(result.toString());

        } catch (Exception e) {
            sendError(response, 500, "강제 초기화 실패: " + e.getMessage());
        }
    }

    /**
     * 비밀번호 검증
     */
    /**
     * 초기 설정 파일 삭제
     */
    private void deleteSetupFiles(String webappPath) throws IOException {
        String[] filesToDelete = {
            webappPath + "keystore.jks",
            webappPath + "jwt-config.properties",
            webappPath + "setup-completed.flag"
        };

        for (String filePath : filesToDelete) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (Exception e) {
                // 파일 삭제 실패해도 계속 진행
                e.printStackTrace();
            }
        }
    }

    /**
     * Keystore 생성
     */
    private void createKeystore(String keystorePath, String password) throws Exception {
        // 기존 keystore 삭제
        Files.deleteIfExists(Paths.get(keystorePath));

        // 빈 keystore 생성 (첫 엔트리 추가 시 자동 생성)
        // keytool로 생성하거나 Java API로 생성 가능
        // 여기서는 첫 번째 키 추가 시 자동 생성됨
    }

    /**
     * EC256 키쌍 생성 및 Keystore에 저장
     */
    private void generateAndStoreEC256Keys(String keystorePath, String password) throws Exception {
        String keystorePass = password;
        String keyAlias = "ec256-jwt";
        String keyPassword = password;

        // keytool 명령어로 EC256 키쌍 생성
        ProcessBuilder pb = new ProcessBuilder(
            "keytool",
            "-genkeypair",
            "-alias", keyAlias,
            "-keyalg", "EC",
            "-keysize", "256",
            "-keystore", keystorePath,
            "-storepass", keystorePass,
            "-keypass", keyPassword,
            "-validity", "3650",
            "-dname", "CN=JWT-EC256, OU=JWT, O=Dev, L=Seoul, ST=Seoul, C=KR"
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            throw new Exception("Keystore 생성 실패: " + output.toString());
        }
    }

    /**
     * API Key를 Keystore에 저장 (비밀 엔트리로)
     * 주: Java Keystore는 비밀 저장을 위해 SecretKeyEntry를 사용
     */
    private void storeApiKey(String keystorePath, String keystorePassword, String apiKey) throws Exception {
        // 설정 파일로 저장
        String configFile = new File(keystorePath).getParent() + File.separator + "jwt-config.properties";
        
        StringBuilder sb = new StringBuilder();
        sb.append("# JWT Configuration\n");
        sb.append("keystore.path=").append(keystorePath).append("\n");
        sb.append("api.key=").append(apiKey).append("\n");
        sb.append("keystore.password=").append(keystorePassword).append("\n");
        sb.append("keystore.alias=ec256-jwt\n");
        
        logger.info("=== storeApiKey START ===");
        logger.info("Config file path: {}", configFile);
        logger.info("Keystore password: {}", keystorePassword);
        logger.info("API key: {}", apiKey);
        logger.info("Config file content:\n{}", sb.toString());
        
        try {
            Files.write(Paths.get(configFile), sb.toString().getBytes(), 
                java.nio.file.StandardOpenOption.CREATE, 
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE,
                java.nio.file.StandardOpenOption.SYNC);
            logger.info("File written successfully to: {}", configFile);
            
            // 파일 검증
            String verifyContent = new String(Files.readAllBytes(Paths.get(configFile)));
            logger.info("Verification - Config file content after write:\n{}", verifyContent);
            
            if (verifyContent.contains("keystore.password=" + keystorePassword)) {
                logger.info("✓ Password update verified in file");
            } else {
                logger.warn("✗ Password update NOT found in file after write!");
            }
        } catch (Exception e) {
            logger.error("Error writing to config file: {}", configFile, e);
            logger.error("Exception details: {}", e.getClass().getName());
            throw e;
        }
        
        logger.info("=== storeApiKey END ===");
    }

    /**
     * 초기화 완료 플래그 생성
     */
    private void createSetupFlag() throws IOException {
        String flagPath = getServletContext().getRealPath("/") + SETUP_FLAG_FILE;
        Files.write(Paths.get(flagPath), "setup-completed".getBytes());
    }

    /**
     * 초기화 완료 여부 확인
     */
    private boolean isSetupCompleted() {
        String flagPath = getServletContext().getRealPath("/") + SETUP_FLAG_FILE;
        return Files.exists(Paths.get(flagPath));
    }

    /**
     * 에러 응답 전송
     */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        response.getWriter().write(error.toString());
    }

    /**
     * 설정 파일에서 현재 비밀번호 읽기
     * admin.js에서 백업 시 사용할 현재 비밀번호를 조회합니다
     */
    public void getCurrentPassword(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("application/json; charset=UTF-8");

        String webappPath = getServletContext().getRealPath("/");
        String configPath = webappPath + "jwt-config.properties";
        
        logger.info("=== getCurrentPassword START ===");
        logger.info("ConfigPath: {}", configPath);
        
        try {
            String password = readPasswordFromConfig(configPath);
            
            if (password == null) {
                sendError(response, 404, "설정 파일에서 비밀번호를 찾을 수 없습니다");
                return;
            }
            
            logger.info("Password read successfully from config file, length: {}", password.length());
            
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("password", password);
            
            response.getWriter().write(result.toString());
        } catch (Exception e) {
            logger.error("Error reading password from config", e);
            sendError(response, 500, "비밀번호 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 설정 파일에서 비밀번호 읽기
     */
    private String readPasswordFromConfig(String configPath) throws Exception {
        try {
            if (!Files.exists(Paths.get(configPath))) {
                logger.warn("Config file not found: {}", configPath);
                return null;
            }
            
            List<String> lines = Files.readAllLines(Paths.get(configPath));
            
            for (String line : lines) {
                if (line.startsWith("keystore.password=")) {
                    String password = line.substring("keystore.password=".length());
                    logger.debug("Password read from config file");
                    return password;
                }
            }
            
            logger.warn("keystore.password property not found in config file");
            return null;
        } catch (Exception e) {
            logger.error("Error reading config file", e);
            throw e;
        }
    }

    /**
     * Keystore 백업 다운로드
     */
    public void backupKeystore(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("application/json; charset=UTF-8");

        String webappPath = getServletContext().getRealPath("/");
        String configPath = webappPath + "jwt-config.properties";
        String password = request.getParameter("password");
        
        logger.info("=== backupKeystore START ===");
        logger.info("WebappPath: {}", webappPath);
        logger.info("ConfigPath: {}", configPath);
        logger.info("Received password length: {}", password != null ? password.length() : "null");
        
        if (password == null || !PasswordService.verifyPassword(password, configPath)) {
            logger.warn("Password verification FAILED for backup");
            sendError(response, 401, "비밀번호가 일치하지 않습니다");
            return;
        }
        
        logger.info("Password verification SUCCESS for backup");

        String keystorePath = webappPath + "keystore.jks";

        if (!Files.exists(Paths.get(keystorePath))) {
            sendError(response, 404, "Keystore를 찾을 수 없습니다");
            return;
        }

        try {
            // Keystore 파일 읽기
            byte[] keystoreData = Files.readAllBytes(Paths.get(keystorePath));

            // Base64 인코딩
            String base64Data = Base64.toBase64String(keystoreData);

            // JSON 응답
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("data", base64Data);
            result.addProperty("filename", "keystore-" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) + ".jks");
            
            response.getWriter().write(result.toString());
        } catch (Exception e) {
            sendError(response, 500, "백업 실패: " + e.getMessage());
        }
    }

    /**
     * Keystore 복원 업로드
     */
    public void restoreKeystore(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("application/json; charset=UTF-8");

        String webappPath = getServletContext().getRealPath("/");
        String configPath = webappPath + "jwt-config.properties";
        String password = request.getParameter("password");
        if (password == null || !PasswordService.verifyPassword(password, configPath)) {
            sendError(response, 401, "비밀번호가 일치하지 않습니다");
            return;
        }

        // Base64 인코딩된 데이터 받기
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        if (sb.length() == 0) {
            sendError(response, 400, "복원할 데이터가 없습니다");
            return;
        }

        try {
            // JSON 파싱
            JsonObject json = gson.fromJson(sb.toString(), JsonObject.class);
            String base64Data = json.get("data").getAsString();

            // Base64 디코딩
            byte[] keystoreData = Base64.decode(base64Data);

            String keystorePath = webappPath + "keystore.jks";

            // 기존 백업 생성
            if (Files.exists(Paths.get(keystorePath))) {
                String backupPath = keystorePath + ".backup";
                Files.copy(Paths.get(keystorePath), Paths.get(backupPath), 
                          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // Keystore 복원
            Files.write(Paths.get(keystorePath), keystoreData);

            // JwtServlet 서블릿 컨텍스트에 keysLoaded 플래그 리셋 요청
            // (복원 후 새로운 키 로드 필요)
            getServletContext().setAttribute("jwt_keys_loaded", false);

            // 응답
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "Keystore가 성공적으로 복원되었습니다");
            response.getWriter().write(result.toString());

        } catch (Exception e) {
            sendError(response, 500, "복원 실패: " + e.getMessage());
        }
    }

    /**
     * 비밀번호 변경 처리
     */
    private void handlePasswordChange(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.info("=== handlePasswordChange START ===");
        
        String webappPath = getServletContext().getRealPath("/");
        String configPath = webappPath + "jwt-config.properties";
        
        logger.debug("WebappPath: {}", webappPath);
        logger.debug("ConfigPath: {}", configPath);
        
        // 현재 비밀번호 검증
        String currentPassword = request.getParameter("currentPassword");
        logger.debug("Received currentPassword parameter, length: {}", currentPassword != null ? currentPassword.length() : "null");
        
        if (currentPassword == null) {
            logger.warn("currentPassword is null");
            sendError(response, 400, "현재 비밀번호를 입력해주세요");
            return;
        }
        
        logger.debug("Calling PasswordService.verifyPassword()");
        boolean passwordMatch = PasswordService.verifyPassword(currentPassword, configPath);
        logger.info("Password verification result: {}", passwordMatch);
        
        if (!passwordMatch) {
            logger.warn("Password verification FAILED");
            sendError(response, 401, "현재 비밀번호가 일치하지 않습니다");
            logger.debug("=== handlePasswordChange END (password mismatch) ===");
            return;
        }
        
        logger.info("Password verification SUCCESS");

        // 새 비밀번호 입력
        String newPassword = request.getParameter("newPassword");
        String confirmNewPassword = request.getParameter("confirmNewPassword");
        
        logger.debug("Received newPassword, length: {}", newPassword != null ? newPassword.length() : "null");
        logger.debug("Received confirmNewPassword, length: {}", confirmNewPassword != null ? confirmNewPassword.length() : "null");

        // 새 비밀번호 검증 (8자 이상)
        if (newPassword == null || newPassword.length() < 8) {
            logger.warn("New password validation failed: length < 8");
            sendError(response, 400, "새 비밀번호는 8자 이상이어야 합니다");
            logger.debug("=== handlePasswordChange END (validation failed) ===");
            return;
        }

        // 새 비밀번호 일치 확인
        if (!newPassword.equals(confirmNewPassword)) {
            logger.warn("New password confirmation mismatch");
            sendError(response, 400, "새 비밀번호가 일치하지 않습니다");
            logger.debug("=== handlePasswordChange END (confirmation mismatch) ===");
            return;
        }

        String keystorePath = webappPath + "keystore.jks";
        logger.debug("KeystorePath: {}", keystorePath);

        try {
            // Keystore 비밀번호 변경
            logger.info("Starting keystore password change");
            changeKeystorePassword(keystorePath, currentPassword, newPassword);
            logger.info("Keystore password changed successfully");

            // 설정 파일의 비밀번호 업데이트
            logger.info("Updating config file with new password");
            storeApiKey(keystorePath, newPassword, newPassword);
            logger.info("Config file updated successfully");

            // JWT 키 캐시 초기화
            getServletContext().setAttribute("jwt_keys_loaded", false);
            logger.info("JWT keys cache cleared");

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "비밀번호가 성공적으로 변경되었습니다");
            response.getWriter().write(result.toString());
            logger.info("=== handlePasswordChange END (SUCCESS) ===");
        } catch (Exception e) {
            logger.error("Exception during password change", e);
            throw e;
        }
    }

    /**
     * Keystore의 비밀번호 변경
     */
    private void changeKeystorePassword(String keystorePath, String oldPassword, String newPassword) throws Exception {
        KeyStore oldKeystore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            oldKeystore.load(fis, oldPassword.toCharArray());
        }

        KeyStore newKeystore = KeyStore.getInstance("JKS");
        newKeystore.load(null, newPassword.toCharArray());

        // 기존 Keystore의 모든 항목을 새 Keystore에 복사
        Enumeration<String> aliases = oldKeystore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (oldKeystore.isKeyEntry(alias)) {
                // Private Key 및 Certificate Chain 복사
                PrivateKey privateKey = (PrivateKey) oldKeystore.getKey(alias, oldPassword.toCharArray());
                java.security.cert.Certificate[] certChain = oldKeystore.getCertificateChain(alias);
                newKeystore.setKeyEntry(alias, privateKey, newPassword.toCharArray(), certChain);
            } else {
                // Certificate만 복사
                java.security.cert.Certificate cert = oldKeystore.getCertificate(alias);
                newKeystore.setCertificateEntry(alias, cert);
            }
        }

        // 새 Keystore 저장
        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            newKeystore.store(fos, newPassword.toCharArray());
        }
    }
}
