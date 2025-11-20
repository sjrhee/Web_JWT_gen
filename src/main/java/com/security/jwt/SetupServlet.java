package com.security.jwt;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.security.jwt.service.SetupActionHandler;
import com.security.jwt.service.SetupSessionManager;
import com.security.jwt.service.SetupValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * JWT 초기 설정 서블릿
 * 요청 라우팅, Keystore 관리, EC256 키쌍 생성
 * 
 * 각 기능은 별도 서비스 클래스로 분리:
 * - SetupActionHandler: 각 action 처리 (backup, restore, etc)
 * - SetupValidator: 입력 검증
 * - SetupSessionManager: 세션 관리
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
        logger.info("=== doGet START ===");
        setCorsHeaders(response);
        String action = request.getParameter("action");
        logger.info("GET 요청 - action: {}", action);

        response.setContentType("application/json; charset=UTF-8");

        try {
            if ("backup".equals(action)) {
                handleBackup(request, response);
            } else {
                handleStatusCheck(response);
            }
        } catch (Exception e) {
            logger.error("GET 요청 처리 중 예외: {}", e.getMessage(), e);
            sendError(response, 500, "요청 처리 중 오류 발생: " + e.getMessage());
        } finally {
            logger.info("=== doGet END ===");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.info("=== doPost START ===");
        setCorsHeaders(response);
        response.setContentType("application/json; charset=UTF-8");
        String action = request.getParameter("action");
        logger.info("POST 요청 - action: {}", action);

        try {
            String webappPath = getServletContext().getRealPath("/");
            SetupActionHandler handler = new SetupActionHandler(webappPath);
            HttpSession session = request.getSession(true);
            SetupSessionManager sessionManager = new SetupSessionManager(session);

            if ("backup".equals(action)) {
                handleBackup(request, response);
            } else if ("restore".equals(action)) {
                handleRestore(request, response, handler, sessionManager);
            } else if ("changePassword".equals(action)) {
                handleChangePassword(request, response, handler, sessionManager);
            } else if ("forceReset".equals(action)) {
                handleForceReset(request, response, handler, sessionManager);
            } else {
                handleInitialSetup(request, response, handler, sessionManager);
            }
        } catch (Exception e) {
            logger.error("POST 요청 처리 중 예외: {}", e.getMessage(), e);
            sendError(response, 500, "요청 처리 중 오류 발생: " + e.getMessage());
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
     * 초기화 완료 여부 확인
     */
    private boolean isSetupCompleted() {
        String flagPath = getServletContext().getRealPath("/") + SETUP_FLAG_FILE;
        return Files.exists(Paths.get(flagPath));
    }

    /**
     * 설정 상태 확인 처리
     */
    private void handleStatusCheck(HttpServletResponse response) throws IOException {
        logger.info("설정 상태 확인");
        boolean isSetupCompleted = isSetupCompleted();
        logger.info("Setup 완료 여부: {}", isSetupCompleted);

        JsonObject result = new JsonObject();
        result.addProperty("setupCompleted", isSetupCompleted);
        response.getWriter().write(result.toString());
    }

    /**
     * 백업 처리
     */
    private void handleBackup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.info("백업 요청 처리");
        String password = request.getParameter("password");

        // 비밀번호 검증
        SetupValidator.ValidationResult validationResult = SetupValidator.validateBackupPassword(password);
        if (!validationResult.isSuccess()) {
            sendError(response, 400, validationResult.getMessage());
            return;
        }

        try {
            String webappPath = getServletContext().getRealPath("/");
            SetupActionHandler handler = new SetupActionHandler(webappPath);
            JsonObject result = handler.backupKeystore(password);
            response.getWriter().write(result.toString());
        } catch (Exception e) {
            logger.error("백업 실패: {}", e.getMessage());
            sendError(response, 500, "백업 실패: " + e.getMessage());
        }
    }

    /**
     * 복원 처리
     */
    private void handleRestore(HttpServletRequest request, HttpServletResponse response,
            SetupActionHandler handler, SetupSessionManager sessionManager) throws Exception {
        logger.info("복원 요청 처리");

        // JSON 파싱
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
            JsonObject json = gson.fromJson(sb.toString(), JsonObject.class);
            String base64Data = json.get("data").getAsString();
            String password = json.has("password") ? json.get("password").getAsString() : null;

            // 검증
            SetupValidator.ValidationResult validationResult = SetupValidator.validateRestoreData(base64Data, password);
            if (!validationResult.isSuccess()) {
                sendError(response, 400, validationResult.getMessage());
                return;
            }

            handler.restoreKeystore(base64Data, password, sessionManager);
            sessionManager.resetCache(getServletContext());

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "Keystore가 성공적으로 복원되었습니다");
            response.getWriter().write(result.toString());
        } catch (Exception e) {
            logger.error("복원 실패: {}", e.getMessage());
            sendError(response, 500, "복원 실패: " + e.getMessage());
        }
    }

    /**
     * 비밀번호 변경 처리
     */
    private void handleChangePassword(HttpServletRequest request, HttpServletResponse response,
            SetupActionHandler handler, SetupSessionManager sessionManager) throws Exception {
        logger.info("비밀번호 변경 요청 처리");

        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        // 검증
        SetupValidator.ValidationResult validationResult = SetupValidator.validateCurrentPassword(currentPassword);
        if (!validationResult.isSuccess()) {
            sendError(response, 400, validationResult.getMessage());
            return;
        }

        validationResult = SetupValidator.validateNewPassword(newPassword, confirmPassword);
        if (!validationResult.isSuccess()) {
            sendError(response, 400, validationResult.getMessage());
            return;
        }

        try {
            handler.changeKeystorePassword(currentPassword, newPassword, sessionManager);
            sessionManager.resetCache(getServletContext());

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "비밀번호가 성공적으로 변경되었습니다");
            response.getWriter().write(result.toString());
        } catch (Exception e) {
            logger.error("비밀번호 변경 실패: {}", e.getMessage());
            sendError(response, 500, "비밀번호 변경 실패: " + e.getMessage());
        }
    }

    /**
     * 강제 초기화 처리
     */
    private void handleForceReset(HttpServletRequest request, HttpServletResponse response,
            SetupActionHandler handler, SetupSessionManager sessionManager) throws Exception {
        logger.info("강제 초기화 요청 처리");

        String adminPassword = request.getParameter("adminPassword");
        String newPassword = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        // 검증
        SetupValidator.ValidationResult validationResult = SetupValidator.validateAdminPassword(adminPassword);
        if (!validationResult.isSuccess()) {
            sendError(response, 400, validationResult.getMessage());
            return;
        }

        validationResult = SetupValidator.validateNewPassword(newPassword, confirmPassword);
        if (!validationResult.isSuccess()) {
            sendError(response, 400, validationResult.getMessage());
            return;
        }

        try {
            handler.forceReset(adminPassword, newPassword, sessionManager);
            sessionManager.resetCache(getServletContext());

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "시스템이 성공적으로 초기화되었습니다. 페이지를 새로고침해주세요");
            response.getWriter().write(result.toString());
        } catch (Exception e) {
            logger.error("강제 초기화 실패: {}", e.getMessage());
            sendError(response, 500, "초기화 실패: " + e.getMessage());
        }
    }

    /**
     * 초기 설정 처리
     */
    private void handleInitialSetup(HttpServletRequest request, HttpServletResponse response,
            SetupActionHandler handler, SetupSessionManager sessionManager) throws Exception {
        logger.info("초기 설정 요청 처리");

        // 이미 초기화되었는지 확인
        if (isSetupCompleted()) {
            logger.warn("이미 초기화되었음. 요청 거부");
            sendError(response, 400, "이미 초기화되었습니다");
            return;
        }

        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        // 검증
        SetupValidator.ValidationResult validationResult = SetupValidator.validatePassword(password, confirmPassword);
        if (!validationResult.isSuccess()) {
            sendError(response, 400, validationResult.getMessage());
            return;
        }

        try {
            handler.performInitialSetup(password, sessionManager);
            sessionManager.resetCache(getServletContext());

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "초기 설정이 완료되었습니다");
            response.getWriter().write(result.toString());
        } catch (Exception e) {
            logger.error("초기 설정 실패: {}", e.getMessage());
            sendError(response, 500, "초기 설정 실패: " + e.getMessage());
        }
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
}