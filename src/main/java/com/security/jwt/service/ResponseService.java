package com.security.jwt.service;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HTTP 응답 처리 서비스
 */
public class ResponseService {
    private static final Logger logger = LogManager.getLogger(ResponseService.class);

    /**
     * 성공 응답
     */
    public static void sendSuccess(HttpServletResponse response, String message) throws IOException {
        logger.info("=== sendSuccess START ===");
        logger.info("응답 메시지: {}", message);
        try {
            response.setContentType("application/json; charset=UTF-8");
            response.setStatus(200);

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", message);
            response.getWriter().write(result.toString());
            logger.info("성공 응답 전송 완료");
            logger.info("=== sendSuccess END ===");
        } catch (IOException e) {
            logger.error("성공 응답 전송 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 성공 응답 (추가 데이터)
     */
    public static void sendSuccessWithData(HttpServletResponse response, String message, String key, String value)
            throws IOException {
        logger.info("=== sendSuccessWithData START ===");
        logger.info("응답 메시지: {}, 추가 데이터: {} = {}", message, key,
                value != null ? value.substring(0, Math.min(value.length(), 50)) + "..." : "null");
        try {
            response.setContentType("application/json; charset=UTF-8");
            response.setStatus(200);

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", message);
            result.addProperty(key, value);
            response.getWriter().write(result.toString());
            logger.info("추가 데이터 응답 전송 완료");
            logger.info("=== sendSuccessWithData END ===");
        } catch (IOException e) {
            logger.error("추가 데이터 응답 전송 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * JWT 생성 응답
     */
    public static void sendJWTResponse(HttpServletResponse response, String jwt, String publicKey) throws IOException {
        logger.info("=== sendJWTResponse START ===");
        logger.info("JWT 응답 전송 (JWT 크기: {} bytes, PublicKey 크기: {} bytes)",
                jwt != null ? jwt.length() : 0,
                publicKey != null ? publicKey.length() : 0);
        try {
            response.setContentType("application/json; charset=UTF-8");
            response.setStatus(200);

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("jwt", jwt);
            result.addProperty("publicKey", publicKey);
            response.getWriter().write(result.toString());
            logger.info("JWT 응답 전송 완료");
            logger.info("=== sendJWTResponse END ===");
        } catch (IOException e) {
            logger.error("JWT 응답 전송 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 에러 응답
     */
    public static void sendError(HttpServletResponse response, int status, String message) throws IOException {
        logger.info("=== sendError START ===");
        logger.warn("에러 응답 (Status: {}, Message: {})", status, message);
        try {
            response.setContentType("application/json; charset=UTF-8");
            response.setStatus(status);

            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", message);
            response.getWriter().write(error.toString());
            logger.info("에러 응답 전송 완료");
            logger.info("=== sendError END ===");
        } catch (IOException e) {
            logger.error("에러 응답 전송 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 에러 응답 (추가 정보)
     */
    public static void sendErrorWithInfo(HttpServletResponse response, int status, String message, String key,
            Object value) throws IOException {
        logger.info("=== sendErrorWithInfo START ===");
        logger.warn("에러 응답 (Status: {}, Message: {}, 추가정보: {} = {})", status, message, key, value);
        try {
            response.setContentType("application/json; charset=UTF-8");
            response.setStatus(status);

            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", message);
            if (value instanceof String) {
                error.addProperty(key, (String) value);
            } else if (value instanceof Boolean) {
                error.addProperty(key, (Boolean) value);
            }
            response.getWriter().write(error.toString());
            logger.info("추가 정보 에러 응답 전송 완료");
            logger.info("=== sendErrorWithInfo END ===");
        } catch (IOException e) {
            logger.error("추가 정보 에러 응답 전송 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Keystore 없음 응답 (setup.jsp로 리다이렉트)
     */
    public static void sendKeystoreNotFoundError(HttpServletResponse response) throws IOException {
        logger.info("=== sendKeystoreNotFoundError START ===");
        logger.warn("Keystore 없음 - setup.jsp로 리다이렉트");
        try {
            response.sendRedirect("/webjwtgen/setup.jsp");
            logger.info("리다이렉트 완료");
            logger.info("=== sendKeystoreNotFoundError END ===");
        } catch (IOException e) {
            logger.error("리다이렉트 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}
