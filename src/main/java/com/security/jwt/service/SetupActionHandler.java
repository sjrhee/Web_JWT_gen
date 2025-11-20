package com.security.jwt.service;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;

/**
 * 초기 설정 액션 처리 서비스
 * backup, restore, changePassword, forceReset, initial setup 등 각 action을 처리
 */
public class SetupActionHandler {
    private static final Logger logger = LogManager.getLogger(SetupActionHandler.class);
    private static final com.google.gson.Gson gson = new com.google.gson.Gson();

    private final String webappPath;

    public SetupActionHandler(String webappPath) {
        this.webappPath = webappPath;
    }

    /**
     * 초기 설정 수행
     */
    public void performInitialSetup(String password, SetupSessionManager sessionManager) throws Exception {
        logger.info("=== performInitialSetup START ===");
        try {
            String keystorePath = getKeystorePath();

            // 1. Keystore 생성
            logger.info("Step 1: Keystore 생성");
            createKeystore(keystorePath, password);

            // 2. EC256 키쌍 생성 및 저장
            logger.info("Step 2: EC256 키쌍 생성 및 저장");
            generateAndStoreEC256Keys(keystorePath, password);

            // 3. 세션에 비밀번호 저장
            logger.info("Step 3: 세션에 비밀번호 저장");
            sessionManager.storePassword(password);

            // 4. 초기화 완료 플래그 생성
            logger.info("Step 4: 초기화 완료 플래그 생성");
            createSetupFlag();

            logger.info("초기 설정 모든 단계 완료");
            logger.info("=== performInitialSetup END ===");
        } catch (Exception e) {
            logger.error("=== performInitialSetup END (ERROR) ===", e);
            throw e;
        }
    }

    /**
     * Keystore 백업
     */
    public JsonObject backupKeystore(String password) throws Exception {
        logger.info("=== backupKeystore START ===");
        try {
            String keystorePath = getKeystorePath();

            // 비밀번호 검증
            if (!KeystoreService.verifyKeystorePassword(keystorePath, password)) {
                logger.warn("비밀번호 검증 실패");
                throw new Exception("비밀번호가 일치하지 않습니다");
            }

            logger.info("Keystore 백업 진행");

            if (!Files.exists(Paths.get(keystorePath))) {
                throw new Exception("Keystore를 찾을 수 없습니다");
            }

            // Keystore 파일 읽기 및 Base64 인코딩
            byte[] keystoreData = Files.readAllBytes(Paths.get(keystorePath));
            String base64Data = org.bouncycastle.util.encoders.Base64.toBase64String(keystoreData);

            // JSON 응답
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("data", base64Data);
            result.addProperty("filename",
                    "keystore-" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) + ".jks");

            logger.info("=== backupKeystore END (SUCCESS) ===");
            return result;
        } catch (Exception e) {
            logger.error("백업 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Keystore 복원
     */
    public void restoreKeystore(String base64Data, String password, SetupSessionManager sessionManager)
            throws Exception {
        logger.info("=== restoreKeystore START ===");
        try {
            String keystorePath = getKeystorePath();

            // Base64 디코딩
            byte[] keystoreData = org.bouncycastle.util.encoders.Base64.decode(base64Data);

            // 비밀번호 검증
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
            }

            if (!passwordValid) {
                throw new Exception("Keystore 비밀번호가 일치하지 않습니다. 올바른 비밀번호를 입력해주세요");
            }

            // 기존 백업 생성
            if (Files.exists(Paths.get(keystorePath))) {
                String backupPath = keystorePath + ".backup";
                Files.copy(Paths.get(keystorePath), Paths.get(backupPath), StandardCopyOption.REPLACE_EXISTING);
                logger.info("기존 Keystore 백업 완료: {}", backupPath);
            }

            // Keystore 복원
            Files.write(Paths.get(keystorePath), keystoreData);
            logger.info("Keystore 복원 완료");

            // 세션에 비밀번호 저장
            sessionManager.storePassword(password);
            logger.info("복원된 Keystore 비밀번호를 세션에 저장");

            logger.info("=== restoreKeystore END (SUCCESS) ===");
        } catch (Exception e) {
            logger.error("=== restoreKeystore END (ERROR) ===", e);
            throw e;
        }
    }

    /**
     * Keystore 비밀번호 변경
     */
    public void changeKeystorePassword(String currentPassword, String newPassword, SetupSessionManager sessionManager)
            throws Exception {
        logger.info("=== changeKeystorePassword START ===");
        try {
            String keystorePath = getKeystorePath();

            // 현재 비밀번호 검증
            if (!KeystoreService.verifyKeystorePassword(keystorePath, currentPassword)) {
                logger.warn("현재 비밀번호 검증 실패");
                throw new Exception("현재 비밀번호가 일치하지 않습니다");
            }

            // Keystore 로드
            KeyStore keystore = KeystoreService.loadKeystore(keystorePath, currentPassword);
            logger.info("Keystore 로드 완료");

            // 백업 생성
            String backupPath = keystorePath + ".backup";
            Files.copy(Paths.get(keystorePath), Paths.get(backupPath), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Keystore 백업 완료: {}", backupPath);

            // 1. Keystore 비밀번호 변경
            try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
                keystore.store(fos, newPassword.toCharArray());
            }
            logger.info("Keystore 비밀번호 변경 완료");

            // 2. 키 엔트리의 비밀번호도 변경
            KeystoreService.changeKeyPassword(keystorePath, newPassword, currentPassword, newPassword);
            logger.info("키 엔트리 비밀번호 변경 완료");

            // 새 비밀번호를 세션에 저장
            sessionManager.storePassword(newPassword);
            logger.info("새 비밀번호를 세션에 저장");

            logger.info("=== changeKeystorePassword END (SUCCESS) ===");
        } catch (Exception e) {
            logger.error("=== changeKeystorePassword END (ERROR) ===", e);
            throw e;
        }
    }

    /**
     * 강제 초기화
     */
    public void forceReset(String adminPassword, String newPassword, SetupSessionManager sessionManager)
            throws Exception {
        logger.info("=== forceReset START ===");
        try {
            String keystorePath = getKeystorePath();

            // 관리자 권한 확인
            if (!KeystoreService.verifyKeystorePassword(keystorePath, adminPassword)) {
                logger.warn("관리자 비밀번호 검증 실패");
                throw new Exception("관리자 비밀번호가 일치하지 않습니다");
            }

            // 기존 파일 백업
            String backupPath = keystorePath + ".reset-backup";
            if (Files.exists(Paths.get(keystorePath))) {
                Files.copy(Paths.get(keystorePath), Paths.get(backupPath), StandardCopyOption.REPLACE_EXISTING);
                logger.info("Keystore 백업 완료: {}", backupPath);
            }

            // 초기 설정과 동일한 과정 수행
            logger.info("새로운 Keystore 생성");
            createKeystore(keystorePath, newPassword);

            logger.info("EC256 키쌍 생성");
            generateAndStoreEC256Keys(keystorePath, newPassword);

            logger.info("새 비밀번호를 세션에 저장");
            sessionManager.storePassword(newPassword);

            logger.info("=== forceReset END (SUCCESS) ===");
        } catch (Exception e) {
            logger.error("=== forceReset END (ERROR) ===", e);
            throw e;
        }
    }

    /**
     * Keystore 생성
     */
    private void createKeystore(String keystorePath, String password) throws Exception {
        logger.info("=== createKeystore START (keystorePath: {}) ===", keystorePath);
        Files.deleteIfExists(Paths.get(keystorePath));
        logger.info("기존 Keystore 파일 삭제 완료");
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
     * 초기화 완료 플래그 생성
     */
    private void createSetupFlag() throws IOException {
        logger.info("=== createSetupFlag START ===");
        String flagPath = webappPath + "setup-completed.flag";
        logger.info("Setup 플래그 경로: {}", flagPath);
        Files.write(Paths.get(flagPath), "setup-completed".getBytes());
        logger.info("Setup 플래그 생성 완료");
        logger.info("=== createSetupFlag END ===");
    }

    /**
     * Keystore 경로 반환
     */
    private String getKeystorePath() {
        return webappPath + "keystore.jks";
    }
}
