package com.security.jwt.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 초기 설정 입력 검증 서비스
 */
public class SetupValidator {
    private static final Logger logger = LogManager.getLogger(SetupValidator.class);

    /**
     * 비밀번호 입력 검증
     */
    public static ValidationResult validatePassword(String password, String confirmPassword) {
        logger.info("=== validatePassword START ===");

        if (password == null || password.isEmpty()) {
            logger.warn("비밀번호 미입력");
            return ValidationResult.error("비밀번호를 입력해주세요");
        }

        if (confirmPassword == null || confirmPassword.isEmpty()) {
            logger.warn("비밀번호 확인 미입력");
            return ValidationResult.error("비밀번호 확인을 입력해주세요");
        }

        if (!password.equals(confirmPassword)) {
            logger.warn("비밀번호 불일치");
            return ValidationResult.error("비밀번호가 일치하지 않습니다");
        }

        logger.info("비밀번호 검증 완료");
        logger.info("=== validatePassword END ===");
        return ValidationResult.success();
    }

    /**
     * 현재 비밀번호 검증
     */
    public static ValidationResult validateCurrentPassword(String currentPassword) {
        logger.info("=== validateCurrentPassword START ===");

        if (currentPassword == null || currentPassword.isEmpty()) {
            logger.warn("현재 비밀번호 미입력");
            return ValidationResult.error("현재 비밀번호를 입력해주세요");
        }

        logger.info("현재 비밀번호 검증 완료");
        logger.info("=== validateCurrentPassword END ===");
        return ValidationResult.success();
    }

    /**
     * 관리자 비밀번호 검증
     */
    public static ValidationResult validateAdminPassword(String adminPassword) {
        logger.info("=== validateAdminPassword START ===");

        if (adminPassword == null || adminPassword.isEmpty()) {
            logger.warn("관리자 비밀번호 미입력");
            return ValidationResult.error("관리자 비밀번호를 입력해주세요");
        }

        logger.info("관리자 비밀번호 검증 완료");
        logger.info("=== validateAdminPassword END ===");
        return ValidationResult.success();
    }

    /**
     * 새 비밀번호 검증
     */
    public static ValidationResult validateNewPassword(String newPassword, String confirmPassword) {
        logger.info("=== validateNewPassword START ===");

        if (newPassword == null || newPassword.isEmpty()) {
            logger.warn("새 비밀번호 미입력");
            return ValidationResult.error("새 비밀번호를 입력해주세요");
        }

        if (!newPassword.equals(confirmPassword)) {
            logger.warn("새 비밀번호 불일치");
            return ValidationResult.error("새 비밀번호가 일치하지 않습니다");
        }

        logger.info("새 비밀번호 검증 완료");
        logger.info("=== validateNewPassword END ===");
        return ValidationResult.success();
    }

    /**
     * 백업 비밀번호 검증
     */
    public static ValidationResult validateBackupPassword(String password) {
        logger.info("=== validateBackupPassword START ===");

        if (password == null || password.isEmpty()) {
            logger.warn("백업 비밀번호 미제공");
            return ValidationResult.error("비밀번호를 제공해주세요");
        }

        logger.info("백업 비밀번호 검증 완료");
        logger.info("=== validateBackupPassword END ===");
        return ValidationResult.success();
    }

    /**
     * 복원 데이터 검증
     */
    public static ValidationResult validateRestoreData(String base64Data, String password) {
        logger.info("=== validateRestoreData START ===");

        if (base64Data == null || base64Data.isEmpty()) {
            logger.warn("복원할 데이터 없음");
            return ValidationResult.error("복원할 데이터가 없습니다");
        }

        if (password == null || password.isEmpty()) {
            logger.warn("복원 비밀번호 미입력");
            return ValidationResult.error("비밀번호를 입력해주세요");
        }

        logger.info("복원 데이터 검증 완료");
        logger.info("=== validateRestoreData END ===");
        return ValidationResult.success();
    }

    /**
     * 검증 결과 클래스
     */
    public static class ValidationResult {
        private final boolean success;
        private final String message;

        private ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
