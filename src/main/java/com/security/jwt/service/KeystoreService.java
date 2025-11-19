package com.security.jwt.service;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.Certificate;

/**
 * Keystore 관리 서비스
 */
public class KeystoreService {
    private static final String KEYSTORE_ALIAS = "ec256-jwt";
    
    /**
     * Keystore 생성
     */
    public static void createKeystore(String keystorePath, String password) throws Exception {
        Files.deleteIfExists(Paths.get(keystorePath));
        
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null, password.toCharArray());
        
        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keystore.store(fos, password.toCharArray());
        }
    }

    /**
     * Keystore 로드
     */
    public static KeyStore loadKeystore(String keystorePath, String password) throws Exception {
        KeyStore keystore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keystore.load(fis, password.toCharArray());
        }
        return keystore;
    }

    /**
     * 개인키 로드
     */
    public static PrivateKey getPrivateKey(String keystorePath, String keystorePassword, String keyPassword) throws Exception {
        KeyStore keystore = loadKeystore(keystorePath, keystorePassword);
        PrivateKey privateKey = (PrivateKey) keystore.getKey(KEYSTORE_ALIAS, keyPassword.toCharArray());
        if (privateKey == null) {
            throw new Exception("Keystore에서 개인키를 찾을 수 없습니다: " + KEYSTORE_ALIAS);
        }
        return privateKey;
    }

    /**
     * 공개키 로드
     */
    public static PublicKey getPublicKey(String keystorePath, String keystorePassword) throws Exception {
        KeyStore keystore = loadKeystore(keystorePath, keystorePassword);
        Certificate cert = keystore.getCertificate(KEYSTORE_ALIAS);
        if (cert == null) {
            throw new Exception("Keystore에서 인증서를 찾을 수 없습니다: " + KEYSTORE_ALIAS);
        }
        return cert.getPublicKey();
    }

    /**
     * Keystore에 키 저장
     */
    public static void storeKeyEntry(String keystorePath, String keystorePassword, 
                                     PrivateKey privateKey, java.security.cert.Certificate[] chain,
                                     String keyPassword) throws Exception {
        KeyStore keystore = loadKeystore(keystorePath, keystorePassword);
        keystore.setKeyEntry(KEYSTORE_ALIAS, privateKey, keyPassword.toCharArray(), chain);
        
        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keystore.store(fos, keystorePassword.toCharArray());
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
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keystore.load(fis, password.toCharArray());
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
