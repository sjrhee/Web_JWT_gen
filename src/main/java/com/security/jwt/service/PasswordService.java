package com.security.jwt.service;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 비밀번호 검증 및 관리 서비스
 */
public class PasswordService {
    private static final Logger logger = LogManager.getLogger(PasswordService.class);

    /**
     * 비밀번호 검증
     */
    public static boolean verifyPassword(String inputPassword, String configPath) throws Exception {
        logger.debug("=== verifyPassword START ===");
        logger.debug("ConfigPath: {}", configPath);
        logger.debug("InputPassword length: {}", inputPassword != null ? inputPassword.length() : "null");
        
        if (!Files.exists(Paths.get(configPath))) {
            logger.error("Config file not found: {}", configPath);
            logger.debug("=== verifyPassword END (file not found) ===");
            return false;
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(configPath)), "UTF-8");
            logger.debug("Config file content length: {}", content.length());
            logger.debug("Config file content:\n{}", content);
            
            String[] lines = content.split("\n");
            logger.debug("Number of lines in config: {}", lines.length);
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                logger.debug("Line {}: [{}]", i, line);
                
                // \r, \n 모두 제거
                line = line.replaceAll("[\r\n]", "").trim();
                logger.debug("Line {} after cleanup: [{}]", i, line);
                
                if (line.startsWith("keystore.password=")) {
                    String storedPassword = line.substring("keystore.password=".length()).trim();
                    logger.debug("Found keystore.password line");
                    logger.debug("Stored password: [{}]", storedPassword);
                    logger.debug("Stored password length: {}", storedPassword.length());
                    logger.debug("Input password: [{}]", inputPassword);
                    logger.debug("Input password length: {}", inputPassword.length());
                    logger.debug("Passwords match: {}", storedPassword.equals(inputPassword));
                    
                    if (storedPassword.equals(inputPassword)) {
                        logger.info("Password verification SUCCESS");
                        logger.debug("=== verifyPassword END (match) ===");
                        return true;
                    }
                }
            }
            logger.warn("keystore.password line not found in config file");
        } catch (Exception e) {
            logger.error("Exception in verifyPassword", e);
            e.printStackTrace();
        }
        logger.debug("=== verifyPassword END (no match) ===");
        return false;
    }

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
     * API Key 검증
     */
    public static boolean isValidApiKey(String apiKey, String configPath) throws Exception {
        if (!Files.exists(Paths.get(configPath))) {
            return false;
        }

        try (FileInputStream fis = new FileInputStream(configPath)) {
            Properties props = new Properties();
            props.load(fis);
            String storedApiKey = props.getProperty("api.key");
            return storedApiKey != null && storedApiKey.equals(apiKey);
        }
    }

    /**
     * 저장된 비밀번호 조회
     */
    public static String getStoredPassword(String configPath) throws Exception {
        if (!Files.exists(Paths.get(configPath))) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(configPath)) {
            Properties props = new Properties();
            props.load(fis);
            return props.getProperty("keystore.password");
        }
    }

    /**
     * 환경 변수에서 Keystore 비밀번호 조회
     */
    public static String getKeystorePasswordFromEnv(String defaultPassword) {
        String envPassword = System.getenv("KEYSTORE_PASSWORD");
        return (envPassword != null && !envPassword.isEmpty()) ? envPassword : defaultPassword;
    }
}
