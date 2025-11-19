package com.security.jwt.service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import com.google.gson.JsonObject;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.util.encoders.Base64;

/**
 * JWT 생성 및 검증 서비스
 */
public class JWTService {

    /**
     * JWT 토큰 생성
     */
    public static String generateJWT(String exp, String iss, String sub, PrivateKey privateKey) throws Exception {
        // Header
        JsonObject header = new JsonObject();
        header.addProperty("alg", "ES256");
        header.addProperty("typ", "JWT");

        // Payload
        JsonObject payload = new JsonObject();
        payload.addProperty("exp", Long.parseLong(exp));
        payload.addProperty("iss", iss);
        payload.addProperty("sub", sub);
        payload.addProperty("iat", System.currentTimeMillis() / 1000);

        // Base64 URL 인코딩
        String headerB64 = base64UrlEncode(header.toString());
        String payloadB64 = base64UrlEncode(payload.toString());

        // 서명할 데이터
        String signData = headerB64 + "." + payloadB64;

        // EC256으로 서명
        Signature signature = Signature.getInstance("SHA256withECDSA", "BC");
        signature.initSign(privateKey);
        signature.update(signData.getBytes());
        byte[] signatureBytes = signature.sign();

        // ECDSA 서명을 JWT 형식으로 변환
        String signatureB64 = ecdsaSignatureToJwt(signatureBytes);

        return signData + "." + signatureB64;
    }

    /**
     * 공개키를 PEM 형식으로 변환
     */
    public static String convertPublicKeyToPem(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        String base64Encoded = Base64.toBase64String(encoded);

        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN PUBLIC KEY-----\n");

        // 64자씩 줄바꿈
        for (int i = 0; i < base64Encoded.length(); i += 64) {
            int end = Math.min(i + 64, base64Encoded.length());
            pem.append(base64Encoded.substring(i, end)).append("\n");
        }

        pem.append("-----END PUBLIC KEY-----");
        return pem.toString();
    }

    /**
     * ECDSA DER 서명을 JWT 형식(r||s)으로 변환
     */
    private static String ecdsaSignatureToJwt(byte[] derSignature) throws Exception {
        ASN1Sequence sequence = ASN1Sequence.getInstance(derSignature);
        ASN1Integer r = ASN1Integer.getInstance(sequence.getObjectAt(0));
        ASN1Integer s = ASN1Integer.getInstance(sequence.getObjectAt(1));

        byte[] rBytes = r.getValue().toByteArray();
        byte[] sBytes = s.getValue().toByteArray();

        // r과 s를 각각 32 바이트로 정규화 (P-256의 경우)
        byte[] rPadded = new byte[32];
        byte[] sPadded = new byte[32];

        System.arraycopy(rBytes, Math.max(0, rBytes.length - 32), rPadded,
                        Math.max(0, 32 - rBytes.length), Math.min(32, rBytes.length));
        System.arraycopy(sBytes, Math.max(0, sBytes.length - 32), sPadded,
                        Math.max(0, 32 - sBytes.length), Math.min(32, sBytes.length));

        // r||s 연결
        byte[] jwtSignature = new byte[64];
        System.arraycopy(rPadded, 0, jwtSignature, 0, 32);
        System.arraycopy(sPadded, 0, jwtSignature, 32, 32);

        return base64UrlEncode(jwtSignature);
    }

    /**
     * Base64 URL 인코딩
     */
    public static String base64UrlEncode(String data) {
        return base64UrlEncode(data.getBytes());
    }

    public static String base64UrlEncode(byte[] data) {
        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(data);
    }

    /**
     * 파라미터 유효성 검사
     */
    public static boolean validateJWTParams(String exp, String iss, String sub) {
        if (exp == null || exp.isEmpty()) return false;
        if (iss == null || iss.isEmpty()) return false;
        if (sub == null || sub.isEmpty()) return false;

        try {
            Long.parseLong(exp);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
