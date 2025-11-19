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
import com.security.jwt.service.KeystoreService;
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
        response.setContentType("application/json; charset=UTF-8");
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
        } else if ("restore".equals(action)) {
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
        } else if ("changePassword".equals(action)) {
            // Keystore 비밀번호 변경
            try {
                changeKeystorePassword(request, response);
            } catch (Exception e) {
                try {
                    sendError(response, 500, "비밀번호 변경 실패: " + e.getMessage());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else if ("forceReset".equals(action)) {
            // 강제 초기화
            try {
                forceReset(request, response);
            } catch (Exception e) {
                try {
                    sendError(response, 500, "초기화 실패: " + e.getMessage());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            // 초기 설정 수행

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
     * DELETE: 강제 초기화는 보안상 비활성화됨
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setContentType("application/json; charset=UTF-8");
        sendError(response, 403, "강제 초기화는 더 이상 지원되지 않습니다. 관리자에게 문의하세요.");
    }    /**
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
        // 설정 파일로 저장 (비밀번호만 저장)
        String configFile = new File(keystorePath).getParent() + File.separator + "jwt-config.properties";
        
        StringBuilder sb = new StringBuilder();
        sb.append("# JWT Configuration\n");
        sb.append("keystore.path=").append(keystorePath).append("\n");
        sb.append("keystore.password=").append(keystorePassword).append("\n");
        sb.append("keystore.alias=ec256-jwt\n");
        
        logger.info("=== storeApiKey START ===");
        logger.info("Config file path: {}", configFile);
        logger.info("Keystore password: {}", keystorePassword);
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
     * Keystore 비밀번호 검증 및 인증 토큰 발급
     */

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
        
        // 비밀번호 검증
        if (password == null || password.isEmpty()) {
            logger.warn("비밀번호 미제공");
            sendError(response, 400, "비밀번호를 제공해주세요");
            return;
        }
        
        // Keystore 비밀번호 검증
        try {
            String keystorePath = webappPath + "keystore.jks";
            if (!KeystoreService.verifyKeystorePassword(keystorePath, password)) {
                logger.warn("비밀번호 검증 실패");
                sendError(response, 401, "비밀번호가 일치하지 않습니다");
                return;
            }
        } catch (Exception e) {
            logger.error("비밀번호 검증 중 오류: {}", e.getMessage());
            sendError(response, 401, "비밀번호 검증 실패: " + e.getMessage());
            return;
        }
        
        logger.info("Keystore 백업 진행");

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
            logger.info("=== backupKeystore END (SUCCESS) ===");
        } catch (Exception e) {
            logger.error("백업 실패: {}", e.getMessage());
            sendError(response, 500, "백업 실패: " + e.getMessage());
        }
    }    /**
     * Keystore 복원 업로드
     */
    public void restoreKeystore(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("application/json; charset=UTF-8");

        String webappPath = getServletContext().getRealPath("/");
        String keystorePath = webappPath + "keystore.jks";
        String configPath = webappPath + "jwt-config.properties";
        
        logger.info("=== restoreKeystore START ===");

        // Base64 인코딩된 데이터와 비밀번호 받기
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
            String password = json.has("password") ? json.get("password").getAsString() : null;

            if (password == null || password.isEmpty()) {
                sendError(response, 400, "비밀번호를 입력해주세요");
                return;
            }

            logger.info("복원할 Keystore 비밀번호 검증 시작");

            // Base64 디코딩
            byte[] keystoreData = Base64.decode(base64Data);
            
            // 임시 파일로 복원할 Keystore를 테스트
            // (실제로 복원 전에 비밀번호가 맞는지 확인)
            boolean passwordValid = false;
            try {
                KeyStore tempKeystore = KeyStore.getInstance("JKS");
                try (ByteArrayInputStream bais = new ByteArrayInputStream(keystoreData)) {
                    tempKeystore.load(bais, password.toCharArray());
                    passwordValid = true;
                    logger.info("복원할 Keystore 비밀번호 검증 성공");
                }
            } catch (Exception e) {
                logger.warn("복원할 Keystore 비밀번호 검증 실패: {}", e.getMessage());
                passwordValid = false;
            }

            if (!passwordValid) {
                sendError(response, 401, "Keystore 비밀번호가 일치하지 않습니다. 올바른 비밀번호를 입력해주세요");
                return;
            }

            // 기존 백업 생성
            if (Files.exists(Paths.get(keystorePath))) {
                String backupPath = keystorePath + ".backup";
                Files.copy(Paths.get(keystorePath), Paths.get(backupPath), 
                          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                logger.info("기존 Keystore 백업 완료: {}", backupPath);
            }

            // Keystore 복원
            Files.write(Paths.get(keystorePath), keystoreData);
            logger.info("Keystore 복원 완료");
            
            // 설정 파일 업데이트 (복원된 Keystore의 비밀번호로)
            KeystoreService.saveConfig(configPath, password);
            logger.info("설정 파일 업데이트 완료 (복원된 Keystore 비밀번호)");

            // JwtServlet 서블릿 컨텍스트에 keysLoaded 플래그 리셋 요청
            getServletContext().setAttribute("jwt_keys_loaded", false);

            // 응답
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "Keystore가 성공적으로 복원되었습니다");
            response.getWriter().write(result.toString());
            logger.info("=== restoreKeystore END (SUCCESS) ===");

        } catch (Exception e) {
            logger.error("=== restoreKeystore END (ERROR) ===", e);
            sendError(response, 500, "복원 실패: " + e.getMessage());
        }
    }

    /**
     * Keystore 비밀번호 변경
     */
    private void changeKeystorePassword(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String webappPath = getServletContext().getRealPath("/");
        String keystorePath = webappPath + "keystore.jks";
        String configPath = webappPath + "jwt-config.properties";
        
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        
        if (currentPassword == null || currentPassword.isEmpty()) {
            sendError(response, 400, "현재 비밀번호를 입력해주세요");
            return;
        }
        
        if (newPassword == null || newPassword.isEmpty()) {
            sendError(response, 400, "새 비밀번호를 입력해주세요");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            sendError(response, 400, "새 비밀번호가 일치하지 않습니다");
            return;
        }
        
        // 현재 비밀번호 검증 (Keystore로)
        if (!KeystoreService.verifyKeystorePassword(keystorePath, currentPassword)) {
            sendError(response, 401, "현재 비밀번호가 일치하지 않습니다");
            return;
        }
        
        try {
            logger.info("=== changeKeystorePassword START ===");
            
            // Keystore 로드
            KeyStore keystore = KeystoreService.loadKeystore(keystorePath, currentPassword);
            logger.info("Keystore 로드 완료");
            
            // 백업 생성
            String backupPath = keystorePath + ".backup";
            Files.copy(Paths.get(keystorePath), Paths.get(backupPath), 
                      java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            logger.info("Keystore 백업 완료: {}", backupPath);
            
            // 1. Keystore 비밀번호 변경
            try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
                keystore.store(fos, newPassword.toCharArray());
            }
            logger.info("Keystore 비밀번호 변경 완료");
            
            // 2. 키 엔트리의 비밀번호도 변경 (Keystore 비밀번호와 동일하게)
            KeystoreService.changeKeyPassword(keystorePath, newPassword, currentPassword, newPassword);
            logger.info("키 엔트리 비밀번호 변경 완료");
            
            // 설정 파일 업데이트
            KeystoreService.saveConfig(configPath, newPassword);
            logger.info("설정 파일 업데이트 완료");
            
            // 캐시 리셋
            getServletContext().setAttribute("jwt_keys_loaded", false);
            
            // 응답
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "비밀번호가 성공적으로 변경되었습니다");
            response.getWriter().write(result.toString());
            logger.info("=== changeKeystorePassword END (SUCCESS) ===");
            
        } catch (Exception e) {
            logger.error("=== changeKeystorePassword END (ERROR) ===", e);
            sendError(response, 500, "비밀번호 변경 실패: " + e.getMessage());
        }
    }

    /**
     * 강제 초기화 (관리자만)
     */
    private void forceReset(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String adminPassword = request.getParameter("adminPassword");
        String newPassword = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        
        if (adminPassword == null || adminPassword.isEmpty()) {
            sendError(response, 400, "관리자 비밀번호를 입력해주세요");
            return;
        }
        
        if (newPassword == null || newPassword.isEmpty()) {
            sendError(response, 400, "새 비밀번호를 입력해주세요");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            sendError(response, 400, "새 비밀번호가 일치하지 않습니다");
            return;
        }
        
        String webappPath = getServletContext().getRealPath("/");
        String keystorePath = webappPath + "keystore.jks";
        String configPath = webappPath + "jwt-config.properties";
        
        // 현재 비밀번호로 관리자 권한 확인
        if (!KeystoreService.verifyKeystorePassword(keystorePath, adminPassword)) {
            sendError(response, 401, "관리자 비밀번호가 일치하지 않습니다");
            return;
        }
        
        try {
            logger.info("=== forceReset START ===");
            
            // 기존 파일 백업
            String backupPath = keystorePath + ".reset-backup";
            if (Files.exists(Paths.get(keystorePath))) {
                Files.copy(Paths.get(keystorePath), Paths.get(backupPath), 
                          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                logger.info("Keystore 백업 완료: {}", backupPath);
            }
            
            // 초기 설정과 동일한 과정 수행
            // 1. Keystore 생성
            createKeystore(keystorePath, newPassword);
            logger.info("새로운 Keystore 생성");
            
            // 2. EC256 키쌍 생성 및 Keystore에 저장
            generateAndStoreEC256Keys(keystorePath, newPassword);
            logger.info("EC256 키쌍 생성");
            
            // 3. 설정 저장
            storeApiKey(keystorePath, newPassword, newPassword);
            logger.info("설정 저장");
            
            // 4. 캐시 리셋
            getServletContext().setAttribute("jwt_keys_loaded", false);
            
            // 응답
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "시스템이 성공적으로 초기화되었습니다. 페이지를 새로고침해주세요");
            response.getWriter().write(result.toString());
            logger.info("=== forceReset END (SUCCESS) ===");
            
        } catch (Exception e) {
            logger.error("=== forceReset END (ERROR) ===", e);
            sendError(response, 500, "초기화 실패: " + e.getMessage());
        }
    }
}