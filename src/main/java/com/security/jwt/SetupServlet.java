package com.security.jwt;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.security.jwt.service.KeystoreService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

/**
 * JWT 초기 설정 서블릿
 * Keystore 생성, EC256 키쌍 생성, API Key 설정
 */
@WebServlet(name = "SetupServlet", urlPatterns = { "/setup" })
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
        logger.info("=== doOptions START (CORS Preflight) ===");
        try {
            setCorsHeaders(response);
            response.setStatus(HttpServletResponse.SC_OK);
            logger.info("CORS preflight 응답 완료");
            logger.info("=== doOptions END ===");
        } catch (Exception e) {
            logger.error("CORS preflight 처리 실패: {}", e.getMessage(), e);
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        String action = request.getParameter("action");
        logger.info("=== doGet START (action: {}) ===", action);

        if ("backup".equals(action)) {
            logger.info("Backup 요청 처리");
            // Keystore 백업
            try {
                backupKeystore(request, response);
            } catch (Exception e) {
                logger.error("Backup 실패", e);
                try {
                    sendError(response, 500, "백업 실패: " + e.getMessage());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            logger.info("설정 상태 확인 요청");
            // 초기화 상태 확인
            response.setContentType("application/json; charset=UTF-8");

            boolean isSetupCompleted = isSetupCompleted();
            logger.info("Setup 완료 여부: {}", isSetupCompleted);

            JsonObject result = new JsonObject();
            result.addProperty("setupCompleted", isSetupCompleted);
            response.getWriter().write(result.toString());
        }
        logger.info("=== doGet END ===");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.info("=== doPost START ===");
        setCorsHeaders(response);
        response.setContentType("application/json; charset=UTF-8");
        String action = request.getParameter("action");
        logger.info("요청 작업: {}", action);

        try {
            if ("backup".equals(action)) {
                logger.info("Keystore 백업 요청");
                // Keystore 백업
                try {
                    backupKeystore(request, response);
                } catch (Exception e) {
                    logger.error("백업 작업 실패: {}", e.getMessage(), e);
                    try {
                        sendError(response, 500, "백업 실패: " + e.getMessage());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } else if ("restore".equals(action)) {
                logger.info("Keystore 복원 요청");
                // Keystore 복원
                try {
                    restoreKeystore(request, response);
                } catch (Exception e) {
                    logger.error("복원 작업 실패: {}", e.getMessage(), e);
                    try {
                        sendError(response, 500, "복원 실패: " + e.getMessage());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } else if ("changePassword".equals(action)) {
                logger.info("Keystore 비밀번호 변경 요청");
                // Keystore 비밀번호 변경
                try {
                    changeKeystorePassword(request, response);
                } catch (Exception e) {
                    logger.error("비밀번호 변경 실패: {}", e.getMessage(), e);
                    try {
                        sendError(response, 500, "비밀번호 변경 실패: " + e.getMessage());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } else if ("forceReset".equals(action)) {
                logger.info("강제 초기화 요청");
                // 강제 초기화
                try {
                    forceReset(request, response);
                } catch (Exception e) {
                    logger.error("초기화 작업 실패: {}", e.getMessage(), e);
                    try {
                        sendError(response, 500, "초기화 실패: " + e.getMessage());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                logger.info("초기 설정 요청 처리");
                // 초기 설정 수행

                // 이미 초기화되었는지 확인
                if (isSetupCompleted()) {
                    logger.warn("이미 초기화되었음. 요청 거부");
                    sendError(response, 400, "이미 초기화되었습니다");
                    return;
                }

                // 비밀번호 입력 받기
                String password = request.getParameter("password");
                String confirmPassword = request.getParameter("confirmPassword");
                logger.info("초기 설정 비밀번호 검증 시작");

                if (password == null || password.isEmpty()) {
                    logger.warn("비밀번호 미입력");
                    sendError(response, 400, "비밀번호를 입력해주세요");
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    logger.warn("비밀번호 불일치");
                    sendError(response, 400, "비밀번호가 일치하지 않습니다");
                    return;
                }
                logger.info("비밀번호 검증 완료");

                // 1. Keystore 생성
                logger.info("Step 1: Keystore 생성");
                String keystorePath = getServletContext().getRealPath("/") + "keystore.jks";
                createKeystore(keystorePath, password);

                // 2. EC256 키쌍 생성 및 Keystore에 저장
                logger.info("Step 2: EC256 키쌍 생성 및 저장");
                generateAndStoreEC256Keys(keystorePath, password);

                // 3. 비밀번호를 세션에 저장 (파일 저장 안 함)
                logger.info("Step 3: 세션에 비밀번호 저장");
                HttpSession session = request.getSession(true);
                storeKeystorePasswordInSession(session, password);

                // 4. 초기화 완료 플래그 생성
                logger.info("Step 4: 초기화 완료 플래그 생성");
                createSetupFlag();

                logger.info("초기 설정 모든 단계 완료");
                // 응답
                JsonObject result = new JsonObject();
                result.addProperty("success", true);
                result.addProperty("message", "초기 설정이 완료되었습니다");
                response.getWriter().write(result.toString());
            }
        } catch (Exception e) {
            logger.error("doPost 처리 중 예외 발생: {}", e.getMessage(), e);
            throw new ServletException(e);
        } finally {
            logger.info("=== doPost END ===");
        }
    }

    /**
     * DELETE: 강제 초기화는 보안상 비활성화됨
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.info("=== doDelete START ===");
        setCorsHeaders(response);
        response.setContentType("application/json; charset=UTF-8");
        logger.warn("강제 초기화 요청 - 보안상 비활성화됨");
        try {
            sendError(response, 403, "강제 초기화는 더 이상 지원되지 않습니다. 관리자에게 문의하세요.");
        } finally {
            logger.info("=== doDelete END ===");
        }
    }

    /**
     * Keystore 생성
     */
    private void createKeystore(String keystorePath, String password) throws Exception {
        logger.info("=== createKeystore START (keystorePath: {}) ===", keystorePath);
        // 기존 keystore 삭제
        Files.deleteIfExists(Paths.get(keystorePath));
        logger.info("기존 Keystore 파일 삭제 완료");

        // 빈 keystore 생성 (첫 엔트리 추가 시 자동 생성)
        // keytool로 생성하거나 Java API로 생성 가능
        // 여기서는 첫 번째 키 추가 시 자동 생성됨
        logger.info("=== createKeystore END ===");
    }

    /**
     * EC256 키쌍 생성 및 Keystore에 저장
     */
    private void generateAndStoreEC256Keys(String keystorePath, String password) throws Exception {
        logger.info("=== generateAndStoreEC256Keys START ===");
        String keystorePass = password;
        String keyAlias = "ec256-jwt";
        String keyPassword = password;
        logger.info("KeyAlias: {}, KeySize: 256", keyAlias);

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
                "-dname", "CN=JWT-EC256, OU=JWT, O=Dev, L=Seoul, ST=Seoul, C=KR");

        pb.redirectErrorStream(true);
        logger.info("keytool 프로세스 시작");
        Process process = pb.start();
        int exitCode = process.waitFor();
        logger.info("keytool 프로세스 종료 (exitCode: {})", exitCode);

        if (exitCode != 0) {
            logger.error("Keystore 생성 실패 (exitCode: {})", exitCode);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            logger.error("keytool 오류 출력: {}", output.toString());
            throw new Exception("Keystore 생성 실패: " + output.toString());
        }
        logger.info("EC256 키쌍 생성 및 Keystore 저장 완료");
        logger.info("=== generateAndStoreEC256Keys END ===");
    }

    /**
     * Keystore 비밀번호를 세션에 저장
     * 설정 파일에는 저장하지 않음 (보안)
     */
    private void storeKeystorePasswordInSession(HttpSession session, String keystorePassword) throws Exception {
        logger.info("=== storeApiKey START ===");
        logger.info("Storing keystore password in session");

        session.setAttribute("keystorePassword", keystorePassword);
        session.setMaxInactiveInterval(30 * 60); // 30분

        logger.info("Keystore password stored in session");
        logger.info("=== storeApiKey END ===");
    }

    /**
     * 초기화 완료 플래그 생성
     */
    private void createSetupFlag() throws IOException {
        logger.info("=== createSetupFlag START ===");
        String flagPath = getServletContext().getRealPath("/") + SETUP_FLAG_FILE;
        logger.info("Setup 플래그 경로: {}", flagPath);
        Files.write(Paths.get(flagPath), "setup-completed".getBytes());
        logger.info("Setup 플래그 생성 완료");
        logger.info("=== createSetupFlag END ===");
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
     * Keystore 백업 다운로드
     */
    public void backupKeystore(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("application/json; charset=UTF-8");

        String webappPath = getServletContext().getRealPath("/");
        String keystorePath = webappPath + "keystore.jks";
        String password = request.getParameter("password");

        logger.info("=== backupKeystore START ===");
        logger.info("WebappPath: {}", webappPath);

        // 비밀번호 검증
        if (password == null || password.isEmpty()) {
            logger.warn("비밀번호 미제공");
            sendError(response, 400, "비밀번호를 제공해주세요");
            return;
        }

        // Keystore 비밀번호 검증
        try {
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
            result.addProperty("filename",
                    "keystore-" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) + ".jks");

            response.getWriter().write(result.toString());
            logger.info("=== backupKeystore END (SUCCESS) ===");
        } catch (Exception e) {
            logger.error("백업 실패: {}", e.getMessage());
            sendError(response, 500, "백업 실패: " + e.getMessage());
        }
    }

    /**
     * Keystore 복원 업로드
     */
    public void restoreKeystore(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("application/json; charset=UTF-8");

        String webappPath = getServletContext().getRealPath("/");
        String keystorePath = webappPath + "keystore.jks";

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

            // 비밀번호를 세션에 저장 (파일 저장 제거)
            HttpSession session = request.getSession(true);
            storeKeystorePasswordInSession(session, password);
            logger.info("복원된 Keystore 비밀번호를 세션에 저장");

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

            // 새 비밀번호를 세션에 저장 (파일 저장 제거)
            HttpSession session = request.getSession(true);
            storeKeystorePasswordInSession(session, newPassword);
            logger.info("새 비밀번호를 세션에 저장");

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

            // 3. 비밀번호를 세션에 저장
            HttpSession session = request.getSession(true);
            storeKeystorePasswordInSession(session, newPassword);
            logger.info("새 비밀번호를 세션에 저장");

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