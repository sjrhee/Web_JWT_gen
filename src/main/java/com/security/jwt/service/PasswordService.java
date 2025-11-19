package com.security.jwt.service;

import javax.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 비밀번호 검증 및 관리 서비스
 */
public class PasswordService {
    private static final Logger logger = LogManager.getLogger(PasswordService.class);

    /**
     * 비밀번호 유효성 확인 (8자 이상)
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    /**
     * 두 비밀번호 일치 확인
     */
    public static boolean passwordsMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }

    /**
     * 환경 변수에서 Keystore 비밀번호 조회
     */
    public static String getKeystorePasswordFromEnv(String defaultPassword) {
        String envPassword = System.getenv("KEYSTORE_PASSWORD");
        return (envPassword != null && !envPassword.isEmpty()) ? envPassword : defaultPassword;
    }

    /**
     * 세션에서 Keystore 비밀번호 조회
     */
    public static String getKeystorePasswordFromSession(HttpSession session) {
        if (session == null) {
            logger.warn("Session is null");
            return null;
        }
        
        Object passwordObj = session.getAttribute("keystorePassword");
        if (passwordObj != null && passwordObj instanceof String) {
            String password = (String) passwordObj;
            logger.debug("Retrieved keystore password from session, length: {}", password.length());
            return password;
        }
        
        logger.warn("Keystore password not found in session");
        return null;
    }
}
