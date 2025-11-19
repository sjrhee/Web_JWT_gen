package com.security.jwt.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * 파일 관리 서비스
 */
public class FileService {
    private static final String SETUP_FLAG_FILE = "setup-completed.flag";

    /**
     * 초기화 완료 플래그 생성
     */
    public static void createSetupFlag(String webappPath) throws IOException {
        String flagPath = webappPath + SETUP_FLAG_FILE;
        Files.write(Paths.get(flagPath), "setup-completed".getBytes());
    }

    /**
     * 초기화 완료 여부 확인
     */
    public static boolean isSetupCompleted(String webappPath) {
        String flagPath = webappPath + SETUP_FLAG_FILE;
        return Files.exists(Paths.get(flagPath));
    }

    /**
     * 초기화 관련 파일 삭제
     */
    public static void deleteSetupFiles(String webappPath) throws IOException {
        String[] filesToDelete = {
            webappPath + "keystore.jks",
            webappPath + "jwt-config.properties",
            webappPath + SETUP_FLAG_FILE
        };

        for (String filePath : filesToDelete) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 파일 존재 여부 확인
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * 파일 삭제
     */
    public static void deleteFile(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
    }

    public static String getSetupFlagFile() {
        return SETUP_FLAG_FILE;
    }
}
