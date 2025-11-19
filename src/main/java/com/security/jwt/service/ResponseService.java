package com.security.jwt.service;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.JsonObject;

/**
 * HTTP 응답 처리 서비스
 */
public class ResponseService {

    /**
     * 성공 응답
     */
    public static void sendSuccess(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(200);
        
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("message", message);
        response.getWriter().write(result.toString());
    }

    /**
     * 성공 응답 (추가 데이터)
     */
    public static void sendSuccessWithData(HttpServletResponse response, String message, String key, String value) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(200);
        
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("message", message);
        result.addProperty(key, value);
        response.getWriter().write(result.toString());
    }

    /**
     * JWT 생성 응답
     */
    public static void sendJWTResponse(HttpServletResponse response, String jwt, String publicKey) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(200);
        
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("jwt", jwt);
        result.addProperty("publicKey", publicKey);
        response.getWriter().write(result.toString());
    }

    /**
     * 에러 응답
     */
    public static void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(status);
        
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        response.getWriter().write(error.toString());
    }

    /**
     * 에러 응답 (추가 정보)
     */
    public static void sendErrorWithInfo(HttpServletResponse response, int status, String message, String key, Object value) throws IOException {
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
    }

    /**
     * Keystore 없음 응답
     */
    public static void sendKeystoreNotFoundError(HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(503);
        
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", "Keystore를 찾을 수 없습니다");
        error.addProperty("needsSetup", true);
        error.addProperty("message", "초기 설정이 필요합니다. 설정 페이지로 이동하거나 Keystore를 복원하세요.");
        response.getWriter().write(error.toString());
    }
}
