package com.security.jwt;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

/**
 * EC256 JWT 생성 서블릿
 * exp, iss, sub를 입력받아 JWT 토큰 생성
 */
@WebServlet(name = "JwtServlet", urlPatterns = {"/generate"})
public class JwtServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String publicKeyPem;
    private String validApiKey; // 동적으로 로드된 API Key
    private boolean keysLoaded = false; // 키 로드 상태
    private static final Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        // Bouncy Castle 보안 제공자 추가
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        // 서블릿 초기화 시에는 키를 로드하지 않음 (지연 로드)
    }

    /**
     * EC256 개인키와 공개키 로드 (Keystore에서)
     */
    private void loadKeys() {
        try {
            String webappPath = getServletContext().getRealPath("/");
            String keystorePath = webappPath + File.separator + "keystore.jks";
            String configPath = webappPath + File.separator + "jwt-config.properties";

            if (!Files.exists(Paths.get(keystorePath))) {
                throw new RuntimeException("Keystore를 찾을 수 없습니다. 초기 설정을 먼저 진행하세요.");
            }

            loadKeysFromKeystore(keystorePath, configPath);
            keysLoaded = true;

        } catch (Exception e) {
            keysLoaded = false;
            throw new RuntimeException("키 로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Keystore에서 키 로드
     */
    private void loadKeysFromKeystore(String keystorePath, String configPath) throws Exception {
        // 설정 파일에서 비밀번호와 API Key 읽기
        Properties config = new Properties();
        if (Files.exists(Paths.get(configPath))) {
            try (FileInputStream fis = new FileInputStream(configPath)) {
                config.load(fis);
            }
        }

        // 비밀번호는 설정 파일에서 읽거나, 환경변수에서 읽음
        // 보안상 환경변수 사용 권장: KEYSTORE_PASSWORD
        String keystorePassword = System.getenv("KEYSTORE_PASSWORD");
        if (keystorePassword == null || keystorePassword.isEmpty()) {
            keystorePassword = config.getProperty("keystore.password", "tomcat123");
        }

        String keyAlias = config.getProperty("keystore.alias", "ec256-jwt");

        // API Key 로드 (config 파일에서)
        validApiKey = config.getProperty("api.key");
        if (validApiKey == null || validApiKey.isEmpty()) {
            throw new RuntimeException("API Key가 설정되지 않았습니다. 초기 설정을 다시 진행하세요.");
        }

        // Keystore 로드
        KeyStore keystore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keystore.load(fis, keystorePassword.toCharArray());
        }

        // 개인키 로드
        privateKey = (PrivateKey) keystore.getKey(keyAlias, keystorePassword.toCharArray());
        if (privateKey == null) {
            throw new RuntimeException("Keystore에서 개인키를 찾을 수 없습니다: " + keyAlias);
        }

        // 공개키 로드 (인증서에서)
        java.security.cert.Certificate cert = keystore.getCertificate(keyAlias);
        if (cert == null) {
            throw new RuntimeException("Keystore에서 인증서를 찾을 수 없습니다: " + keyAlias);
        }

        publicKey = cert.getPublicKey();

        // 공개키를 PEM 형식으로 변환
        publicKeyPem = convertPublicKeyToPem(publicKey);
    }

    /**
     * 공개키를 PEM 형식으로 변환
     */
    private String convertPublicKeyToPem(PublicKey publicKey) {
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=UTF-8");

        try {
            // 강제 초기화 플래그 확인 (관리자가 강제 초기화한 경우)
            Boolean keysLoadedFlag = (Boolean) getServletContext().getAttribute("jwt_keys_loaded");
            if (keysLoadedFlag != null && !keysLoadedFlag) {
                keysLoaded = false;
                getServletContext().setAttribute("jwt_keys_loaded", null);
            }

            // Keystore 상태 확인 (첫 요청 또는 키 로드 실패 시)
            if (!keysLoaded) {
                try {
                    loadKeys();
                } catch (RuntimeException e) {
                    // Keystore가 없으면 초기화 필요 알림
                    response.setStatus(503);
                    JsonObject error = new JsonObject();
                    error.addProperty("success", false);
                    error.addProperty("error", "Keystore를 찾을 수 없습니다");
                    error.addProperty("needsSetup", true);
                    error.addProperty("message", "초기 설정이 필요합니다. 설정 페이지로 이동하거나 Keystore를 복원하세요.");
                    response.getWriter().write(error.toString());
                    return;
                }
            }

            // API Key 인증 확인
            String apiKey = request.getParameter("key");
            if (!isValidApiKey(apiKey)) {
                sendError(response, 401, "API Key가 없거나 유효하지 않습니다");
                return;
            }

            // 파라미터 받기
            String exp = request.getParameter("exp");
            String iss = request.getParameter("iss");
            String sub = request.getParameter("sub");

            // 유효성 검사
            if (exp == null || exp.isEmpty() || iss == null || iss.isEmpty() || sub == null || sub.isEmpty()) {
                sendError(response, 400, "exp, iss, sub 파라미터는 필수입니다");
                return;
            }

            // JWT 생성
            String jwt = generateJWT(exp, iss, sub);

            // 응답 생성
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("jwt", jwt);
            result.addProperty("publicKey", publicKeyPem);

            response.getWriter().write(result.toString());

        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.setContentType("application/json; charset=UTF-8");
                response.setStatus(500);
                JsonObject error = new JsonObject();
                error.addProperty("success", false);
                error.addProperty("error", "JWT 생성 실패");
                error.addProperty("details", e.getMessage());
                response.getWriter().write(error.toString());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * JWT 토큰 생성
     */
    private String generateJWT(String exp, String iss, String sub) throws Exception {
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

        // ECDSA 서명을 JWT 형식으로 변환 (r||s)
        String signatureB64 = ecdsaSignatureToJwt(signatureBytes);

        return signData + "." + signatureB64;
    }

    /**
     * ECDSA DER 서명을 JWT 형식(r||s)으로 변환
     */
    private String ecdsaSignatureToJwt(byte[] derSignature) throws Exception {
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
    private String base64UrlEncode(String data) {
        return base64UrlEncode(data.getBytes());
    }

    private String base64UrlEncode(byte[] data) {
        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(data);
    }

    /**
     * API Key 검증
     */
    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && apiKey.equals(validApiKey);
    }

    /**
     * 에러 응답 전송
     */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        response.getWriter().write(error.toString());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
