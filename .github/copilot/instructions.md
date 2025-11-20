# GitHub Copilot 개발 가이드

## 프로젝트 개요
JWT 토큰 생성 및 관리 웹 애플리케이션
- **언어**: Java 11
- **빌드**: Maven
- **웹 서버**: Tomcat 9
- **프레임워크**: Java Servlet/JSP
- **보안**: EC256(ECDSA-SHA256) + AES-256-GCM 암호화

## 프로젝트 구조

```
webjwtgen/
├── src/main/
│   ├── java/com/security/jwt/
│   │   ├── JwtServlet.java           # JWT 생성 엔드포인트 (/generate)
│   │   ├── SetupServlet.java         # 초기 설정 엔드포인트 (/setup)
│   │   └── service/
│   │       ├── JWTService.java       # JWT 생성/검증 로직
│   │       ├── KeystoreService.java  # KeyStore 관리
│   │       ├── ConfigCrypto.java     # AES-GCM 암호화
│   │       ├── ResponseService.java  # HTTP 응답 처리
│   │       └── PasswordService.java  # 비밀번호 검증
│   ├── webapp/
│   │   ├── setup.jsp                 # 초기 설정 페이지
│   │   ├── index.jsp                 # JWT 생성 페이지
│   │   ├── admin.jsp                 # 관리자 페이지
│   │   └── WEB-INF/web.xml          # 웹 설정
│   └── resources/
│       └── log4j2.xml               # 로깅 설정
└── pom.xml                          # Maven 의존성

```

## 핵심 컴포넌트 설명

### 1. KeyStore 관리 (`jwtaes256`, `jwtec256private`)
- **`jwtaes256`**: AES-256 대칭키 (설정값 암호화용)
- **`jwtec256private`**: EC256 개인키 (JWT 서명용)
- **경로**: `~/.jwt-config/keystore.jks`
- **암호**: 사용자 지정 비밀번호 (최소 8자)

### 2. 설정 파일
- **경로**: `~/.jwt-config/jwt-config.properties`
- **내용**: 암호화된 API 키, KeyStore 경로, KeyStore 비밀번호

### 3. 암호화 방식
```
AES-256-GCM with:
- 256-bit key (jwtaes256 alias)
- 128-bit GCM tag length
- 12-byte random IV (prepended to ciphertext)
- Base64 encoding: Base64(IV || Ciphertext)
```

## 코딩 규칙

### 1. 네이밍 컨벤션
- **클래스**: PascalCase (예: `JwtServlet`, `KeystoreService`)
- **메서드**: camelCase (예: `generateJWT()`, `ensureConfigKey()`)
- **상수**: UPPER_SNAKE_CASE (예: `CONFIG_KEY_ALIAS`)
- **패키지**: com.security.jwt.*

### 2. 예외 처리
```java
// ✅ 권장: 명시적 예외 처리
try {
    // 코드
} catch (IOException | KeyStoreException e) {
    logger.error("Failed to load keystore", e);
    throw new RuntimeException("KeyStore 로드 실패", e);
}

// ❌ 지양: 예외 무시
catch (Exception e) {
    // 무시
}
```

### 3. 로깅
```java
// ✅ 권장: 명확한 로그 메시지
logger.info("API key encrypted successfully");
logger.error("Failed to decrypt config: {}", e.getMessage());

// ❌ 지양: 로그 없이 조용히 실패
// (공백)
```

### 4. 보안 주의사항
- **비밀번호**: 절대 소스 코드에 하드코딩하지 마세요
- **API 키**: 항상 암호화하여 저장하세요
- **PrivateKey**: 메모리에서 사용 후 반드시 clear() 호출
- **예외 메시지**: 민감한 정보(경로, 키값) 노출 금지

### 5. 메서드 작성 가이드
```java
// ✅ 권장: 명확한 시그니처와 JavaDoc
/**
 * JWT 토큰 생성
 * @param exp 만료 시간 (초 단위)
 * @param iss Issuer (발급자)
 * @param sub Subject (주제)
 * @param privateKey 서명용 개인키
 * @return Base64 인코딩된 JWT 토큰
 * @throws Exception 서명 실패 시
 */
public static String generateJWT(String exp, String iss, String sub, PrivateKey privateKey) throws Exception {
    // 구현
}
```

## API 엔드포인트

### 1. 초기 설정: `/setup`
- **방식**: POST (설정 생성)
- **요청**: `password`, `apiKey`
- **응답**: `{"success": true, "message": "..."}` (JSON)

### 2. JWT 생성: `/generate`
- **방식**: GET
- **파라미터**: `exp`, `iss`, `sub`, `apiKey`
- **응답**: `{"success": true, "token": "..."}`

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
