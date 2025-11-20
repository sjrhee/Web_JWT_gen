# 개발 가이드

## 프로젝트 개요
ES256(ECDSA with SHA-256) 기반 JWT 토큰 생성 및 관리 시스템

| 항목 | 값 |
|------|-----|
| 언어 | Java 11 |
| 빌드 | Maven |
| 웹 서버 | Tomcat 9 |
| 프레임워크 | Java Servlet/JSP |
| 보안 | ES256 (ECDSA with SHA-256) |
| 암호화 라이브러리 | BouncyCastle 1.70 |
| JSON 처리 | Gson 2.10.1 |

## 프로젝트 구조

```
src/main/java/com/security/jwt/
├── JwtServlet.java                  # GET /generate - JWT 생성
├── SetupServlet.java                # POST /setup - Keystore 관리
└── service/
    ├── JWTService.java              # JWT header, payload, signature 생성
    ├── KeystoreService.java         # EC256 키쌍 관리 (생성, 로드, 검증)
    ├── PasswordService.java         # 비밀번호 검증 및 해싱
    ├── ResponseService.java         # JSON 응답 구성
    ├── SetupActionHandler.java      # Setup 요청 처리 (backup, restore, etc)
    ├── SetupValidator.java          # 입력값 검증 및 비즈니스 로직
    └── SetupSessionManager.java     # HttpSession 관리

src/main/webapp/
├── index.jsp                        # JWT 생성 페이지
├── setup.jsp                        # Keystore 초기화 페이지
├── admin.jsp                        # 관리 페이지 (백업, 복원, 비밀번호 변경)
├── admin.js                         # 관리 페이지 AJAX
├── css/
│   ├── style.css                    # index.jsp 스타일
│   ├── setup-admin.css              # setup.jsp 스타일
│   └── admin.css                    # admin.jsp 스타일
└── WEB-INF/
    ├── web.xml                      # 웹 애플리케이션 설정
    ├── lib/                         # 라이브러리 JAR 파일
    └── classes/                     # 컴파일된 클래스 (빌드 시)

src/main/resources/
└── log4j2.xml                       # 로깅 설정 (WARN/DEBUG 모드 전환 가능)

pom.xml                             # Maven 의존성 정의
```

## 핵심 컴포넌트

### 1. JwtServlet (엔드포인트: `/generate`)
GET 요청으로 JWT 토큰 생성

**요청 파라미터:**
- `sub`: Subject (사용자 ID) - 필수
- `iss`: Issuer (발급 기관) - 필수
- `exp`: 만료 시간 (Unix timestamp, 초 단위) - 필수
- `password`: Keystore 비밀번호 - 필수

**응답:**
```json
{
  "status": "success",
  "data": {
    "jwt": "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9...",
    "publicKey": "-----BEGIN PUBLIC KEY-----\nMFkwEwYH..."
  }
}
```

**주요 기능:**
- 세션에서 Keystore 비밀번호 캐시 (재사용)
- EC256 개인키로 JWT 서명
- 공개키를 PEM 형식으로 반환

### 2. SetupServlet (엔드포인트: `/setup`)
Keystore 초기화 및 관리

**지원하는 action (POST 파라미터 `action`):**
- `initialSetup`: Keystore 생성 (처음 한 번만)
- `backup`: Keystore 백업 (Base64 인코딩)
- `restore`: Keystore 복원 (Base64 디코딩)
- `changePassword`: Keystore 비밀번호 변경
- `forceReset`: 시스템 강제 리셋 (모든 데이터 삭제)

**요청 예시:**
```
POST /setup?action=initialSetup
Body: password=MyPassword123&password_confirm=MyPassword123
```

### 3. KeystoreService (Keystore 관리)
**정적 메서드:**
- `createKeystore(keystorePath, password)`: EC256 Keystore 생성
- `loadKeystore(keystorePath, password)`: Keystore 로드
- `getPrivateKey(keystorePath, keystorePassword, keyPassword)`: 개인키 조회 (alias: ec256-jwt)
- `getPublicKey(keystorePath, keystorePassword)`: 공개키 조회
- `verifyKeystorePassword(keystorePath, password)`: 비밀번호 검증
- `storeKeyInKeystore()`: Keystore에 키 저장
- `changeKeyPassword()`: 키의 비밀번호 변경

**Keystore 정보:**
- 위치: `webapp/keystore.jks`
- 타입: JKS (Java KeyStore)
- 개인키 alias: `ec256-jwt`
- 알고리즘: EC256 (P-256 곡선)
- 키 유효기간: 10년

### 4. JWTService (JWT 생성)
**정적 메서드:**
- `generateJWT(exp, iss, sub, privateKey)`: JWT 토큰 생성
  - Base64 인코딩된 header, payload, signature 반환
  - ES256 알고리즘 사용
- `convertPublicKeyToPem(publicKey)`: 공개키를 PEM 형식으로 변환
  - "-----BEGIN PUBLIC KEY-----" 형식
  - Base64 인코딩된 키 데이터

**JWT 구조:**
```
Header (Base64):
{
  "alg": "ES256",
  "typ": "JWT"
}

Payload (Base64):
{
  "exp": <Unix timestamp (seconds)>,
  "iss": "<issuer>",
  "sub": "<subject>"
}

Signature (Base64):
ECDSA_SHA256(Header || "." || Payload, privateKey)

완성된 JWT:
Header.Payload.Signature
```

### 5. SetupValidator (입력 검증)
**정적 메서드:**
- `validatePassword(password, confirmPassword)`: 비밀번호 검증
  - null/빈 문자열 체크
  - 두 비밀번호 일치 확인
  - 반환: `ValidationResult`
- `validateCurrentPassword(currentPassword)`: 현재 비밀번호 검증
  - null/빈 문자열 체크
  - 반환: `ValidationResult`
- `validateNewPassword(newPassword, confirmPassword)`: 새 비밀번호 검증
  - null/빈 문자열 체크
  - 두 비밀번호 일치 확인
  - 반환: `ValidationResult`
- `validateAdminPassword(adminPassword)`: 관리자 비밀번호 검증
  - null/빈 문자열 체크
  - 반환: `ValidationResult`

**ValidationResult 클래스:**
```java
public static class ValidationResult {
    private boolean success;
    private String message;
    
    // static factory methods
    public static ValidationResult success()
    public static ValidationResult error(String message)
    
    // getters
    public boolean isSuccess()
    public String getMessage()
}
```

### 6. SetupActionHandler (Setup 액션 처리)
생성자: `SetupActionHandler(webappPath)`

**메서드:**
- `performInitialSetup(password, sessionManager)`: Keystore 생성
  - Keystore 파일 생성
  - EC256 키쌍 생성 및 저장
  - 초기화 완료 플래그 생성
- `backupKeystore(password)`: Keystore Base64 백업
  - 반환: `{"success": true, "data": "BASE64_STRING", "filename": "keystore-YYYY-MM-DD.jks"}`
- `restoreKeystore(base64Data, password, sessionManager)`: Keystore 복원
  - Base64 디코딩 및 검증
  - 기존 Keystore 백업 생성
  - 복원된 Keystore 로드
- `changeKeystorePassword(currentPassword, newPassword, sessionManager)`: 비밀번호 변경
  - Keystore 비밀번호 변경
  - 키 엔트리 비밀번호도 변경
  - 백업 생성
- `forceReset(adminPassword, newPassword, sessionManager)`: 시스템 리셋
  - 모든 데이터 삭제
  - 새 비밀번호로 새 Keystore 생성
  - 초기화 완료 플래그 재생성

### 7. SetupSessionManager (세션 관리)
생성자: `SetupSessionManager(HttpSession)`

**메서드:**
- `storePassword(keystorePassword)`: 비밀번호 세션 저장
  - 세션 타임아웃: 30분 (1800초)
- `getPassword()`: 저장된 비밀번호 조회
  - 반환: `String` (null 가능)
- `hasPassword()`: 세션에 비밀번호가 있는지 확인
  - 반환: `boolean`
- `removePassword()`: 세션에서 비밀번호 제거
- `resetCache(ServletContext)`: 캐시 리셋
  - JWT 키 로드 캐시 초기화

### 8. ResponseService (HTTP 응답)
**정적 메서드:**
- `sendSuccess(response, message)`: 성공 응답
  ```json
  {
    "success": true,
    "message": "메시지"
  }
  ```

- `sendSuccessWithData(response, message, key, value)`: 데이터 포함 성공 응답
  ```json
  {
    "success": true,
    "message": "메시지",
    "<key>": "<value>"
  }
  ```

- `sendJWTResponse(response, jwt, publicKey)`: JWT 응답
  ```json
  {
    "success": true,
    "jwt": "eyJhbGciOiJFUzI1NiIs...",
    "publicKey": "-----BEGIN PUBLIC KEY-----\n..."
  }
  ```

- `sendError(response, statusCode, message)`: 에러 응답
  ```json
  {
    "success": false,
    "message": "에러 메시지"
  }
  ```

### 9. PasswordService (비밀번호 검증)
**정적 메서드:**
- `isValidPassword(password)`: 비밀번호 유효성 확인
  - 최소 8글자 필수
  - 반환: `boolean`
- `passwordsMatch(password, confirmPassword)`: 두 비밀번호 일치 확인
  - 반환: `boolean`
- `getKeystorePasswordFromEnv(defaultPassword)`: 환경 변수에서 비밀번호 조회
  - 환경 변수: `KEYSTORE_PASSWORD`
- `getKeystorePasswordFromSession(session)`: 세션에서 비밀번호 조회
  - 반환: `String` (null 가능)

### 9. PasswordService (비밀번호 검증)
**정적 메서드:**
- `verifyPassword(password, keystorePath)`: 비밀번호 검증

## 코딩 규칙

### 네이밍 컨벤션
- **클래스**: PascalCase (예: `JwtServlet`, `KeystoreService`)
- **메서드**: camelCase (예: `generateJWT()`, `loadKeystore()`)
- **상수**: UPPER_SNAKE_CASE (예: `JWT_ALGORITHM`, `DEFAULT_KEYSIZE`)
- **패키지**: `com.security.jwt.*`
- **JSP 파일**: lowercase (예: `index.jsp`, `setup.jsp`)

### 코딩 스타일
- 모든 메서드에 JavaDoc 주석 작성
- 메서드 시작/종료 로그 작성 (`logger.info("=== methodName START/END ===")`)
- 파라미터 검증 (null 체크, 빈 문자열 체크)
- 명시적 예외 처리 (catch-all 금지)

### 예외 처리
```java
// ✅ 권장
try {
    // 코드
} catch (IOException | KeyStoreException e) {
    logger.error("Failed to load keystore", e);
    throw new RuntimeException("KeyStore 로드 실패", e);
}

// ❌ 금지
catch (Exception e) {
    // 무시
}
```

### 로깅
```java
// ✅ 권장
logger.info("=== generateJWT START ===");
logger.debug("exp: {}, iss: {}, sub: {}", exp, iss, sub);
logger.info("JWT generated successfully");
logger.info("=== generateJWT END ===");

// ❌ 금지
System.out.println("JWT generated");
logger.debug("error: " + e); // 문자열 연결 대신 {} 사용
```

### 보안 주의사항
- **비밀번호**: 절대 소스 코드에 하드코딩하지 마세요
- **로그**: 민감한 정보(전체 비밀번호, 개인키) 로깅 금지
- **PrivateKey**: 메모리에서 사용 후 `privateKey.destroy()` 호출
- **입력 검증**: 모든 사용자 입력값 검증
- **파일 권한**: Keystore 파일 644 권한 유지

## 개발 팁

### 로컬 테스트 흐름

**1. 빌드**
```bash
cd /home/ubuntu/Work/webjwtgen
mvn clean package -DskipTests
```

**2. 배포**
```bash
sudo cp target/webjwtgen.war /var/lib/tomcat9/webapps/
```

**3. Tomcat 재시작 및 배포 대기**
```bash
sudo systemctl restart tomcat9
# WebApp 배포 대기 (약 5초)
sleep 5
```

**4. 초기화 (첫 실행만)**
```bash
curl -X POST "http://localhost:8080/webjwtgen/setup?action=initialSetup" \
  -d "password=TestPassword123&password_confirm=TestPassword123"
```

**5. JWT 생성 테스트**
```bash
# 현재 시간 + 10년 (Unix timestamp 계산)
TIMESTAMP=$(($(date +%s) + 315360000))

curl "http://localhost:8080/webjwtgen/generate?sub=user-123&iss=test-issuer&exp=${TIMESTAMP}&password=TestPassword123"
```

**응답 예시:**
```json
{
  "success": true,
  "jwt": "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjI3MzU2ODk2MDAsImlzcyI6InRlc3QtaXNzdWVyIiwic3ViIjoidXNlci0xMjMifQ.SIGNATURE...",
  "publicKey": "-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...\n-----END PUBLIC KEY-----"
}
```

### 디버그 모드 활성화

`src/main/resources/log4j2.xml` 수정:

```xml
<!-- PRODUCTION (기본) -->
<Logger name="com.security.jwt" level="WARN" />

<!-- DEVELOPMENT (변경) -->
<Logger name="com.security.jwt" level="DEBUG" />
```

로그 위치: `/var/lib/tomcat9/logs/webjwtgen.log`

### 세션 비밀번호 캐싱

```java
// SetupSessionManager 사용 (최대 30분 유지)
SetupSessionManager sessionManager = new SetupSessionManager(session);

// 한 번 저장
sessionManager.storePassword("MyPassword123");

// 이후 요청에서 재사용
String cachedPassword = sessionManager.getPassword();
if (cachedPassword != null) {
    // 세션에서 비밀번호 사용
}

// 캐시 초기화
sessionManager.removePassword();
```

### 비밀번호 검증 패턴

```java
// SetupValidator 사용
ValidationResult result = SetupValidator.validatePassword(password, confirmPassword);
if (!result.isSuccess()) {
    ResponseService.sendError(response, 400, result.getMessage());
    return;
}

// PasswordService 사용
if (!PasswordService.isValidPassword(password)) {
    throw new Exception("비밀번호는 8글자 이상이어야 합니다");
}
```

### Keystore 작업 패턴

```java
// 1. Keystore 생성
KeystoreService.createKeystore(keystorePath, password);

// 2. Keystore 로드
KeyStore keystore = KeystoreService.loadKeystore(keystorePath, password);

// 3. 개인키 조회
PrivateKey privateKey = KeystoreService.getPrivateKey(keystorePath, keystorePassword, keyPassword);

// 4. 공개키 조회
PublicKey publicKey = KeystoreService.getPublicKey(keystorePath, keystorePassword);

// 5. PEM 형식 변환
String publicKeyPem = JWTService.convertPublicKeyToPem(publicKey);

// 6. 비밀번호 검증
boolean isValid = KeystoreService.verifyKeystorePassword(keystorePath, password);
```

### Backup/Restore 패턴

```java
// Backup
SetupActionHandler handler = new SetupActionHandler(webappPath);
JsonObject backupResult = handler.backupKeystore(password);
String base64Data = backupResult.get("data").getAsString();
String filename = backupResult.get("filename").getAsString();

// Restore
handler.restoreKeystore(base64Data, password, sessionManager);

// Change Password
handler.changeKeystorePassword(oldPassword, newPassword, sessionManager);

// Force Reset
handler.forceReset(adminPassword, newPassword, sessionManager);
```

## 참고 링크

- [JWT RFC 7519](https://tools.ietf.org/html/rfc7519)
- [ES256 RFC 7518](https://tools.ietf.org/html/rfc7518)
- [BouncyCastle API](https://www.bouncycastle.org/docs/docs.html)
- [Java KeyStore Documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)
- [EC Cryptography](https://en.wikipedia.org/wiki/Elliptic-curve_cryptography)
- [Tomcat 공식 문서](https://tomcat.apache.org/)

### 3. 관리 기능: `/setup`
- **DELETE**: 전체 초기화
- **GET**: 설정 상태 확인

## 의존성

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| Bouncy Castle | 1.70 | EC256, Base64 인코딩 |
| Gson | 2.10.1 | JSON 처리 |
| Log4j2 | 2.21.1 | 로깅 |
| JUnit | 4.13.2 | 테스트 |

## 주요 주의사항

### ⚠️ KeyStore 관리
- KeyStore 파일 절대 삭제하지 마세요 (키 복구 불가)
- 백업 기능을 통해 정기적으로 백업하세요

### ⚠️ 암호화 데이터
- 암호 변경 시 기존 암호화된 데이터는 복호화 불가
- 새로운 API 키 재설정 필요

### ⚠️ 보안 정책
- 모든 민감한 작업은 HTTPS를 통해서만 수행
- 관리자 페이지는 인증 필수 (현재: 비밀번호만 사용)
- API 키는 쿼리 문자열이 아닌 POST 본문으로 전송

## 코드 리뷰 체크리스트

새 기능 추가 시 다음을 확인하세요:

- [ ] 예외 처리 적절한가?
- [ ] 로그 메시지는 명확한가?
- [ ] 민감한 정보가 노출되지는 않나?
- [ ] KeyStore/설정 파일 접근은 안전한가?
- [ ] 암호화된 데이터는 올바르게 처리되나?
- [ ] JavaDoc/주석이 작성되었나?
- [ ] 보안 정책을 준수하나?

## 테스트 방법

```bash
# 1. 빌드
mvn clean package

# 2. Tomcat 배포
cp target/webjwtgen.war /opt/tomcat9/webapps/

# 3. Tomcat 재시작
sudo /opt/tomcat9/bin/catalina.sh stop
sudo /opt/tomcat9/bin/catalina.sh start

# 4. 초기 설정 테스트
curl -X POST http://localhost:8080/webjwtgen/setup \
  -d "password=mypassword&apiKey=sk-test-key"

# 5. JWT 생성 테스트
curl "http://localhost:8080/webjwtgen/generate?exp=3600&iss=test&sub=user&apiKey=sk-test-key"
```

## 추가 리소스

- 프로젝트 아키텍처: `jwt 생성 프로그램 구조.md`
- 보안 분석: `PASSWORD_MANAGEMENT.md`
- Bouncy Castle 문서: https://www.bouncycastle.org/
- Java Servlet 문서: https://docs.oracle.com/javaee/7/tutorial/

## 문의 및 피드백

코드 작성 중 의문사항이 있으면 이 가이드를 참고하세요.
가이드에 없는 내용은 기존 코드의 패턴을 따르세요.
