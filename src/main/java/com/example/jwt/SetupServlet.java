package com.example.jwt;

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
import org.bouncycastle.asn1.*;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.util.encoders.Base64;

/**
 * JWT 초기 설정 서블릿
 * Keystore 생성, EC256 키쌍 생성, API Key 설정
 */
@WebServlet(name = "SetupServlet", urlPatterns = {"/setup"})
public class SetupServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson gson = new Gson();
    private static final String SETUP_FLAG_FILE = "setup-completed.flag";

    @Override
    public void init() throws ServletException {
        // Bouncy Castle 보안 제공자 추가
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // GET: 초기화 상태 확인
        response.setContentType("application/json; charset=UTF-8");

        boolean isSetupCompleted = isSetupCompleted();
        
        JsonObject result = new JsonObject();
        result.addProperty("setupCompleted", isSetupCompleted);
        response.getWriter().write(result.toString());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // POST: 초기 설정 수행
        response.setContentType("application/json; charset=UTF-8");

        try {
            // 이미 초기화되었는지 확인
            if (isSetupCompleted()) {
                sendError(response, 400, "이미 초기화되었습니다");
                return;
            }

            // 비밀번호 입력 받기
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");

            if (password == null || password.isEmpty()) {
                sendError(response, 400, "비밀번호를 입력해주세요");
                return;
            }

            if (!password.equals(confirmPassword)) {
                sendError(response, 400, "비밀번호가 일치하지 않습니다");
                return;
            }

            // 1. Keystore 생성
            String keystorePath = getServletContext().getRealPath("/") + "keystore.jks";
            createKeystore(keystorePath, password);

            // 2. EC256 키쌍 생성 및 Keystore에 저장
            generateAndStoreEC256Keys(keystorePath, password);

            // 3. API Key 저장 (입력한 비밀번호를 API Key로 사용)
            storeApiKey(keystorePath, password, password);

            // 4. 초기화 완료 플래그 생성
            createSetupFlag();

            // 응답
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "초기 설정이 완료되었습니다");
            response.getWriter().write(result.toString());

        } catch (Exception e) {
            sendError(response, 500, "초기 설정 실패: " + e.getMessage());
        }
    }

    /**
     * DELETE: 강제 초기화 (관리자용)
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=UTF-8");

        try {
            // 비밀번호 확인
            String password = request.getParameter("password");
            if (password == null || !verifyPassword(password)) {
                sendError(response, 401, "비밀번호가 일치하지 않습니다");
                return;
            }

            // 강제 초기화 확인 코드 확인
            String confirmCode = request.getParameter("confirm");
            if (confirmCode == null || !confirmCode.equals("FORCE_RESET_CONFIRMED")) {
                sendError(response, 400, "강제 초기화 확인 코드가 필요합니다");
                return;
            }

            // 기존 파일 삭제
            String webappPath = getServletContext().getRealPath("/");
            deleteSetupFiles(webappPath);

            // 응답
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "초기화가 완전히 리셋되었습니다. 페이지를 새로고침해주세요.");
            response.getWriter().write(result.toString());

        } catch (Exception e) {
            sendError(response, 500, "강제 초기화 실패: " + e.getMessage());
        }
    }

    /**
     * 비밀번호 검증
     */
    private boolean verifyPassword(String inputPassword) throws Exception {
        String webappPath = getServletContext().getRealPath("/");
        String configFile = webappPath + "jwt-config.properties";
        
        if (!Files.exists(Paths.get(configFile))) {
            return false;
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            Properties props = new Properties();
            props.load(fis);
            String storedPassword = props.getProperty("keystore.password");
            return storedPassword != null && storedPassword.equals(inputPassword);
        }
    }

    /**
     * 초기 설정 파일 삭제
     */
    private void deleteSetupFiles(String webappPath) throws IOException {
        String[] filesToDelete = {
            webappPath + "keystore.jks",
            webappPath + "jwt-config.properties",
            webappPath + "setup-completed.flag"
        };

        for (String filePath : filesToDelete) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (Exception e) {
                // 파일 삭제 실패해도 계속 진행
                e.printStackTrace();
            }
        }
    }

    /**
     * Keystore 생성
     */
    private void createKeystore(String keystorePath, String password) throws Exception {
        // 기존 keystore 삭제
        Files.deleteIfExists(Paths.get(keystorePath));

        // 빈 keystore 생성 (첫 엔트리 추가 시 자동 생성)
        // keytool로 생성하거나 Java API로 생성 가능
        // 여기서는 첫 번째 키 추가 시 자동 생성됨
    }

    /**
     * EC256 키쌍 생성 및 Keystore에 저장
     */
    private void generateAndStoreEC256Keys(String keystorePath, String password) throws Exception {
        String keystorePass = password;
        String keyAlias = "ec256-jwt";
        String keyPassword = password;

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
            "-dname", "CN=JWT-EC256, OU=JWT, O=Dev, L=Seoul, ST=Seoul, C=KR"
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            throw new Exception("Keystore 생성 실패: " + output.toString());
        }
    }

    /**
     * API Key를 Keystore에 저장 (비밀 엔트리로)
     * 주: Java Keystore는 비밀 저장을 위해 SecretKeyEntry를 사용
     */
    private void storeApiKey(String keystorePath, String keystorePassword, String apiKey) throws Exception {
        // keytool을 사용하거나 자동으로 저장 (별도 파일로도 가능)
        // 여기서는 설정 파일로 저장하는 방식 선택
        String configFile = new File(keystorePath).getParent() + File.separator + "jwt-config.properties";
        
        Properties props = new Properties();
        props.setProperty("api.key", apiKey);
        props.setProperty("keystore.path", keystorePath);
        props.setProperty("keystore.password", keystorePassword);  // 비밀번호 저장
        props.setProperty("keystore.alias", "ec256-jwt");
        
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "JWT Configuration");
        }
    }

    /**
     * 초기화 완료 플래그 생성
     */
    private void createSetupFlag() throws IOException {
        String flagPath = getServletContext().getRealPath("/") + SETUP_FLAG_FILE;
        Files.write(Paths.get(flagPath), "setup-completed".getBytes());
    }

    /**
     * 초기화 완료 여부 확인
     */
    private boolean isSetupCompleted() {
        String flagPath = getServletContext().getRealPath("/") + SETUP_FLAG_FILE;
        return Files.exists(Paths.get(flagPath));
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
}
