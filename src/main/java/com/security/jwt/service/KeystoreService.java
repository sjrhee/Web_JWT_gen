package com.security.jwt.service;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.Certificate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Keystore 관리 서비스
 */
public class KeystoreService {
    private static final Logger logger = LogManager.getLogger(KeystoreService.class);
    private static final String KEYSTORE_ALIAS = "ec256-jwt";

    /**
     * Keystore 생성
     */
    public static void createKeystore(String keystorePath, String password) throws Exception {
        logger.info("=== createKeystore START ===");
        logger.info("Keystore 경로: {}", keystorePath);
        try {
            Files.deleteIfExists(Paths.get(keystorePath));
            logger.info("기존 Keystore 삭제 완료");

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null, password.toCharArray());
            logger.info("새 JKS Keystore 인스턴스 생성");

            try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
                keystore.store(fos, password.toCharArray());
            }
            logger.info("Keystore 저장 완료");
            logger.info("=== createKeystore END ===");
        } catch (Exception e) {
            logger.error("Keystore 생성 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Keystore 로드
     */
    public static KeyStore loadKeystore(String keystorePath, String password) throws Exception {
        logger.info("=== loadKeystore START ===");
        logger.info("Keystore 경로: {}", keystorePath);
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keystore.load(fis, password.toCharArray());
            }
            logger.info("Keystore 로드 완료");
            logger.info("=== loadKeystore END ===");
            return keystore;
        } catch (Exception e) {
            logger.error("Keystore 로드 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 개인키 로드
     */
    public static PrivateKey getPrivateKey(String keystorePath, String keystorePassword, String keyPassword)
            throws Exception {
        logger.info("=== getPrivateKey START ===");
        logger.info("Keystore 경로: {}, KeyAlias: {}", keystorePath, KEYSTORE_ALIAS);
        try {
            KeyStore keystore = loadKeystore(keystorePath, keystorePassword);
            PrivateKey privateKey = (PrivateKey) keystore.getKey(KEYSTORE_ALIAS, keyPassword.toCharArray());
            if (privateKey == null) {
                logger.error("Keystore에서 개인키를 찾을 수 없음: {}", KEYSTORE_ALIAS);
                throw new Exception("Keystore에서 개인키를 찾을 수 없습니다: " + KEYSTORE_ALIAS);
            }
            logger.info("개인키 로드 완료 (KeyType: {})", privateKey.getAlgorithm());
            logger.info("=== getPrivateKey END ===");
            return privateKey;
        } catch (Exception e) {
            logger.error("개인키 로드 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 공개키 로드
     */
    public static PublicKey getPublicKey(String keystorePath, String keystorePassword) throws Exception {
        logger.info("=== getPublicKey START ===");
        logger.info("Keystore 경로: {}, CertAlias: {}", keystorePath, KEYSTORE_ALIAS);
        try {
            KeyStore keystore = loadKeystore(keystorePath, keystorePassword);
            Certificate cert = keystore.getCertificate(KEYSTORE_ALIAS);
            if (cert == null) {
                logger.error("Keystore에서 인증서를 찾을 수 없음: {}", KEYSTORE_ALIAS);
                throw new Exception("Keystore에서 인증서를 찾을 수 없습니다: " + KEYSTORE_ALIAS);
            }
            PublicKey publicKey = cert.getPublicKey();
            logger.info("공개키 로드 완료 (KeyType: {})", publicKey.getAlgorithm());
            logger.info("=== getPublicKey END ===");
            return publicKey;
        } catch (Exception e) {
            logger.error("공개키 로드 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Keystore에 키 저장
     */
    public static void storeKeyEntry(String keystorePath, String keystorePassword,
            PrivateKey privateKey, java.security.cert.Certificate[] chain,
            String keyPassword) throws Exception {
        logger.info("=== storeKeyEntry START ===");
        logger.info("Keystore 경로: {}, KeyAlias: {}, CertChainLength: {}", keystorePath, KEYSTORE_ALIAS, chain.length);
        try {
            KeyStore keystore = loadKeystore(keystorePath, keystorePassword);
            keystore.setKeyEntry(KEYSTORE_ALIAS, privateKey, keyPassword.toCharArray(), chain);
            logger.info("키 엔트리 설정 완료");

            try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
                keystore.store(fos, keystorePassword.toCharArray());
            }
            logger.info("Keystore 저장 완료");
            logger.info("=== storeKeyEntry END ===");
        } catch (Exception e) {
            logger.error("키 저장 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 키 엔트리의 비밀번호 변경
     */
    public static void changeKeyPassword(String keystorePath, String keystorePassword,
            String oldKeyPassword, String newKeyPassword) throws Exception {
        KeyStore keystore = loadKeystore(keystorePath, keystorePassword);

        // 기존 키 엔트리 가져오기
        PrivateKey privateKey = (PrivateKey) keystore.getKey(KEYSTORE_ALIAS, oldKeyPassword.toCharArray());
        if (privateKey == null) {
            throw new Exception("Keystore에서 개인키를 찾을 수 없습니다: " + KEYSTORE_ALIAS);
        }

        // 인증서 가져오기
        java.security.cert.Certificate[] chain = keystore.getCertificateChain(KEYSTORE_ALIAS);
        if (chain == null) {
            throw new Exception("Keystore에서 인증서 체인을 찾을 수 없습니다: " + KEYSTORE_ALIAS);
        }

        // 새 비밀번호로 키 엔트리 다시 저장
        keystore.setKeyEntry(KEYSTORE_ALIAS, privateKey, newKeyPassword.toCharArray(), chain);

        // Keystore 저장
        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keystore.store(fos, keystorePassword.toCharArray());
        }
    }

    /**
     * Keystore 비밀번호 검증 (실제 Keystore 파일로 검증)
     */
    public static boolean verifyKeystorePassword(String keystorePath, String password) {
        logger.info("=== verifyKeystorePassword START ===");
        logger.info("Keystore 경로: {}", keystorePath);
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keystore.load(fis, password.toCharArray());
            }
            logger.info("Keystore 비밀번호 검증 성공");
            logger.info("=== verifyKeystorePassword END ===");
            return true;
        } catch (Exception e) {
            logger.warn("Keystore 비밀번호 검증 실패: {}", e.getMessage());
            return false;
        }
    }
}
