package com.security.jwt;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

import com.security.jwt.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * EC256 JWT 생성 서블릿
 * exp, iss, sub를 입력받아 JWT 토큰 생성
 */
@WebServlet(name = "JwtServlet", urlPatterns = {"/generate"})
public class JwtServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LogManager.getLogger(JwtServlet.class);
    private PrivateKey privateKey;
    private String publicKeyPem;
    private boolean keysLoaded = false;
    
    static {
        // 클래스 로드 시 BouncyCastle Security Provider 등록
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public void init() throws ServletException {
        // init에서도 한번 더 확인
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * 키 로드 (Keystore에서)
     */
    private void loadKeys(HttpSession session) throws Exception {
        logger.info("=== loadKeys START ===");
        String webappPath = getServletContext().getRealPath("/");
        String keystorePath = webappPath + "keystore.jks";

        logger.info("webappPath: " + webappPath);
        logger.info("keystorePath: " + keystorePath);

        if (!FileService.fileExists(keystorePath)) {
            logger.error("Keystore 파일 없음: " + keystorePath);
            throw new RuntimeException("Keystore를 찾을 수 없습니다. 초기 설정을 먼저 진행하세요.");
        }

        try {
            logger.info("Getting keystore password from session");
            String keystorePassword = PasswordService.getKeystorePasswordFromSession(session);
            
            if (keystorePassword == null) {
                logger.error("Keystore password not found in session");
                throw new RuntimeException("세션에서 Keystore 비밀번호를 찾을 수 없습니다. 초기 설정을 다시 진행하세요.");
            }
            
            logger.info("keystorePassword 길이: " + keystorePassword.length());

            logger.info("KeystoreService.getPrivateKey 호출");
            privateKey = KeystoreService.getPrivateKey(keystorePath, keystorePassword, keystorePassword);
            logger.info("Private Key 로드 성공");
            
            logger.info("KeystoreService.getPublicKey 호출");
            PublicKey publicKey = KeystoreService.getPublicKey(keystorePath, keystorePassword);
            logger.info("Public Key 로드 성공");
            
            logger.info("convertPublicKeyToPem 호출");
            publicKeyPem = JWTService.convertPublicKeyToPem(publicKey);
            logger.info("Public Key PEM 변환 성공");

            keysLoaded = true;
            logger.info("=== loadKeys END (SUCCESS) ===");
        } catch (Exception e) {
            logger.error("=== loadKeys END (ERROR) ===", e);
            keysLoaded = false;
            throw new RuntimeException("키 로드 실패: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.info("=== JWT 생성 요청 START ===");
        try {
            logger.info("Step 1: 키 로드 상태 확인");
            HttpSession session = request.getSession(false);
            if (session == null) {
                logger.error("Session not found");
                ResponseService.sendError(response, 400, "세션이 없습니다. 초기 설정을 진행하세요.");
                return;
            }
            
            // 키 로드 상태 확인
            Boolean keysLoadedFlag = (Boolean) getServletContext().getAttribute("jwt_keys_loaded");
            if (keysLoadedFlag != null && !keysLoadedFlag) {
                logger.info("Step 1.1: 캐시된 키 상태가 false, 초기화");
                keysLoaded = false;
                getServletContext().setAttribute("jwt_keys_loaded", null);
            }

            if (!keysLoaded) {
                logger.info("Step 2: 키 로드 시작");
                try {
                    loadKeys(session);
                    logger.info("Step 2.1: 키 로드 성공");
                } catch (RuntimeException e) {
                    logger.error("Step 2.2: 키 로드 실패", e);
                    ResponseService.sendKeystoreNotFoundError(response);
                    return;
                }
            }

            // Keystore 비밀번호 검증
            String password = request.getParameter("password");
            logger.info("Step 3: Keystore 비밀번호 검증");
            
            if (password == null || password.isEmpty()) {
                logger.warn("Step 3.1: 비밀번호 미제공");
                ResponseService.sendError(response, 400, "Keystore 비밀번호를 입력해주세요");
                return;
            }
            
            try {
                String webappPath = getServletContext().getRealPath("/");
                String keystorePath = webappPath + "keystore.jks";
                if (!KeystoreService.verifyKeystorePassword(keystorePath, password)) {
                    logger.warn("Step 3.2: 비밀번호 검증 실패");
                    ResponseService.sendError(response, 401, "Keystore 비밀번호가 일치하지 않습니다");
                    return;
                }
                logger.info("Step 3.3: 비밀번호 검증 성공");
            } catch (Exception e) {
                logger.error("Step 3.4: 비밀번호 검증 중 오류", e);
                ResponseService.sendError(response, 401, "비밀번호 검증 실패");
                return;
            }

            // 파라미터 검증
            String exp = request.getParameter("exp");
            String iss = request.getParameter("iss");
            String sub = request.getParameter("sub");
            logger.info("Step 4: JWT 파라미터 검증 - exp: " + exp + ", iss: " + iss + ", sub: " + sub);

            if (!JWTService.validateJWTParams(exp, iss, sub)) {
                logger.warn("Step 4.1: JWT 파라미터 검증 실패");
                ResponseService.sendError(response, 400, "exp, iss, sub 파라미터는 필수입니다");
                return;
            }

            // JWT 생성
            logger.info("Step 5: JWT 생성 시작");
            String jwt = JWTService.generateJWT(exp, iss, sub, privateKey);
            logger.info("Step 5.1: JWT 생성 성공");
            ResponseService.sendJWTResponse(response, jwt, publicKeyPem);
            logger.info("=== JWT 생성 요청 END (SUCCESS) ===");

        } catch (Exception e) {
            logger.error("=== JWT 생성 요청 END (ERROR) ===", e);
            e.printStackTrace();
            ResponseService.sendError(response, 500, "JWT 생성 실패: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}

