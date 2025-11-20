package com.security.jwt.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpSession;

/**
 * 초기 설정 세션 관리자
 * Keystore 비밀번호 저장/관리
 */
public class SetupSessionManager {
    private static final Logger logger = LogManager.getLogger(SetupSessionManager.class);
    private final HttpSession session;

    public SetupSessionManager(HttpSession session) {
        this.session = session;
    }

    /**
     * 비밀번호를 세션에 저장
     */
    public void storePassword(String keystorePassword) {
        logger.info("=== storePassword START ===");
        logger.info("비밀번호를 세션에 저장 중");

        session.setAttribute("keystorePassword", keystorePassword);
        session.setMaxInactiveInterval(30 * 60); // 30분

        logger.info("비밀번호 저장 완료");
        logger.info("=== storePassword END ===");
    }

    /**
     * 세션에서 비밀번호 반환
     */
    public String getPassword() {
        return (String) session.getAttribute("keystorePassword");
    }

    /**
     * 세션에 비밀번호가 존재하는지 확인
     */
    public boolean hasPassword() {
        return session.getAttribute("keystorePassword") != null;
    }

    /**
     * 세션에서 비밀번호 제거
     */
    public void removePassword() {
        logger.info("비밀번호 세션에서 제거");
        session.removeAttribute("keystorePassword");
    }

    /**
     * 캐시 리셋 (Servlet Context)
     */
    public void resetCache(javax.servlet.ServletContext context) {
        logger.info("JWT 키 로드 캐시 리셋");
        context.setAttribute("jwt_keys_loaded", false);
    }
}
