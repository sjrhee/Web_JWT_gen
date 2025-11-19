package com.security.jwt.service;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Properties;

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
     * 설정 파일 저장
     */
    public static void saveConfig(String configPath, String apiKey, String keystorePassword) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# JWT Configuration\n");
        sb.append("api.key=").append(apiKey).append("\n");
        sb.append("keystore.password=").append(keystorePassword).append("\n");
        sb.append("keystore.alias=").append(KEYSTORE_ALIAS).append("\n");
        sb.append("keystore.path=").append(new File(configPath).getParent() + File.separator + "keystore.jks").append("\n");
        
        Files.write(Paths.get(configPath), sb.toString().getBytes());
    }

    /**
     * 설정 파일 로드
     */
    public static Properties loadConfig(String configPath) throws IOException {
        Properties props = new Properties();
        if (Files.exists(Paths.get(configPath))) {
            String content = new String(Files.readAllBytes(Paths.get(configPath)));
            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx);
                    String value = line.substring(idx + 1);
                    props.setProperty(key, value);
                }
            }
        }
        return props;
    }
}
