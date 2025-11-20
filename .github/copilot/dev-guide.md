# 개발 환경 설정 가이드

## 개발 환경 정보

### Tomcat 서버
- **경로**: `/var/lib/tomcat9/`
- **포트**: 8080
- **시스템 서비스**: Tomcat은 systemctl에 등록되어 있음
- **WebApps**: `/var/lib/tomcat9/webapps`

### Tomcat 명령어
```bash
# Tomcat 상태 확인
sudo systemctl status tomcat9

# Tomcat 시작
sudo systemctl start tomcat9

# Tomcat 중지
sudo systemctl stop tomcat9

# Tomcat 재시작
sudo systemctl restart tomcat9

# Tomcat 자동시작 활성화
sudo systemctl enable tomcat9

# Tomcat 로그 확인 (systemctl)
sudo journalctl -u tomcat9 -n 100
sudo journalctl -u tomcat9 -f  # 실시간 모니터링

# 또는 파일 로그 확인
tail -f /var/lib/tomcat9/logs/catalina.out
```

## VS Code 설정 (`.vscode/settings.json`)

```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "maven.executable.preferMavenFromPath": true,
  "[java]": {
    "editor.defaultFormatter": "redhat.java",
    "editor.formatOnSave": true,
    "editor.rulers": [100, 120]
  },
  "files.exclude": {
    "**/.classpath": true,
    "**/.project": true
  },
  "search.exclude": {
    "**/node_modules": true,
    "**/target": true
  }
}
```

## Maven 설정 (`.mvn/extensions.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
  <!-- Maven 확장 설정 -->
</extensions>
```

## GitHub Copilot 활용 팁

### 1. 자동 완성 활용
```java
// 타이핑 시 Copilot이 제안합니다
public static String generateJWT(String exp, String iss, String sub, PrivateKey privateKey) throws Exception {
    // Copilot이 표준 패턴 제안
}
```

### 2. 주석으로 함수 생성
```java
// AES-GCM을 사용하여 평문을 암호화합니다. IV는 Base64로 인코딩되어 앞에 붙습니다.
// 함수명 생성 후 Tab으로 구현 수락

// 전체 함수 작성 제안 확인
```

### 3. 테스트 코드 생성
```java
// JWTService의 테스트 클래스 작성
// 테스트 메서드 자동 제안 활용
```

## 일반적인 작업 패턴

### KeyStore 작업 (KeystoreService)
```java
// KeyStore 생성
KeystoreService.createKeystore(keystorePath, password);

// KeyStore 로드
KeyStore keystore = KeystoreService.loadKeystore(keystorePath, password);

// 개인키 로드
PrivateKey privateKey = KeystoreService.getPrivateKey(keystorePath, keystorePassword, keyPassword);

// 공개키 로드
PublicKey publicKey = KeystoreService.getPublicKey(keystorePath, keystorePassword);
```

### JWT 생성 (JWTService)
```java
// JWT 토큰 생성 (ES256 방식)
String token = JWTService.generateJWT(exp, iss, sub, privateKey);

// 공개키를 PEM 형식으로 변환
String publicKeyPem = JWTService.convertPublicKeyToPem(publicKey);
```

### HTTP 응답 처리 (ResponseService)
```java
// 성공 응답
ResponseService.sendSuccess(response, "작업 완료");

// 데이터와 함께 성공 응답
ResponseService.sendSuccessWithData(response, "메시지", "key", "value");

// 에러 응답
ResponseService.sendError(response, 400, "잘못된 요청");
```

### 파일 관리 (FileService)
```java
// 초기화 완료 플래그 생성
FileService.createSetupFlag(webappPath);

// 초기화 상태 확인
boolean isSetup = FileService.isSetupCompleted(webappPath);
```

### 비밀번호 검증 (PasswordService)
```java
// 비밀번호 유효성 확인 (최소 8자)
boolean isValid = PasswordService.isValidPassword(password);

// 세션에서 관리자 여부 확인
boolean isAdmin = PasswordService.isAdminLoggedIn(session);
```

## 로깅 및 디버깅

### Log4j2 설정
- 설정 파일: `src/main/resources/log4j2.xml`
- 모든 서블릿과 서비스에 Logger 구현됨
- 로깅 레벨: INFO, WARN, ERROR

## 주요 서블릿 및 엔드포인트

### JwtServlet (`/generate`)
**목적**: EC256을 사용하여 JWT 토큰 생성

**기능**:
- `init()`: Bouncy Castle 보안 제공자 등록
- `loadKeys()`: KeyStore에서 개인키/공개키 로드
- `doGet()`: GET 요청으로 JWT 생성

**요청 파라미터**:
- `exp`: 만료 시간 (초 단위, 예: 3600)
- `iss`: Issuer (발급자, 예: "test")
- `sub`: Subject (주제, 예: "user")
- `apiKey`: API 키 (검증용)

**응답 형식**:
```json
{
  "success": true,
  "token": "eyJ...",
  "publicKey": "-----BEGIN PUBLIC KEY-----\n..."
}
```

**중요 사항**:
- 모든 파라미터는 필수
- API 키는 암호화되어 검증됨
- JWT는 ES256 (ECDSA-SHA256) 방식으로 서명

### SetupServlet (`/setup`)
**목적**: 초기 설정, KeyStore 생성, 관리 기능

**기능**:
- `init()`: Bouncy Castle 보안 제공자 등록
- `setCorsHeaders()`: CORS 헤더 설정
- `doPost()`: 초기 설정 (KeyStore 생성, EC256 키 생성)
- `doGet()`: 설정 상태 확인
- `doDelete()`: 전체 초기화

**POST 요청 파라미터** (초기 설정):
- `password`: KeyStore 비밀번호 (최소 8자)
- `apiKey`: API 키

**응답 형식**:
```json
{
  "success": true,
  "message": "설정이 완료되었습니다",
  "setupCompleted": true
}
```

**중요 사항**:
- 초기 설정은 한 번만 실행 가능 (플래그 체크)
- KeyStore는 `keystore.jks`로 WebApp 루트에 생성
- 설정 완료 후 플래그 파일 생성

## 서비스 계층 (Service Classes)

### KeystoreService
KeyStore 생성/로드, 키 관리

| 메서드 | 설명 |
|--------|------|
| `createKeystore()` | 새 KeyStore 생성 |
| `loadKeystore()` | 기존 KeyStore 로드 |
| `getPrivateKey()` | 개인키 로드 (별도 암호) |
| `getPublicKey()` | 공개키 로드 |

### JWTService
JWT 토큰 생성 및 변환

| 메서드 | 설명 |
|--------|------|
| `generateJWT()` | JWT 토큰 생성 (ES256) |
| `convertPublicKeyToPem()` | 공개키를 PEM 형식으로 변환 |

### CertificateService
X509 자체 서명 인증서 생성

### ResponseService
HTTP 응답 처리 (JSON 포맷)

### PasswordService
비밀번호 검증 및 세션 관리

### FileService
파일 작업 (플래그 생성/확인)

### 설정 파일 확인
```bash
# 설정 파일 경로
cat ~/.jwt-config/jwt-config.properties

# KeyStore 내용 확인
keytool -list -v -keystore ~/.jwt-config/keystore.jks -storepass {password}
```

### 디버깅 팁

#### 로그 확인
```bash
# Tomcat 로그 실시간 모니터링 (systemctl)
sudo journalctl -u tomcat9 -f

# 또는 파일 로그 모니터링
tail -f /var/lib/tomcat9/logs/catalina.out

# 에러 로그만 확인
tail -100 /var/lib/tomcat9/logs/catalina.out | grep ERROR

# 특정 클래스 로그 확인
tail -f /var/lib/tomcat9/logs/catalina.out | grep "JwtServlet\|SetupServlet"
```

#### 설정 파일 확인
```bash
# 설정 파일 경로 (WebApp 루트)
ls -la /var/lib/tomcat9/webapps/webjwtgen/

# KeyStore 내용 확인
keytool -list -v -keystore /var/lib/tomcat9/webapps/webjwtgen/keystore.jks -storepass {password}

# KeyStore 별도 별도 확인
keytool -list -keystore /var/lib/tomcat9/webapps/webjwtgen/keystore.jks -storepass {password}
```

#### HTTP 요청 테스트
```bash
# 설정 상태 확인
curl -s http://localhost:8080/webjwtgen/setup | jq

# JWT 생성 테스트 (기본 요청)
curl -s "http://localhost:8080/webjwtgen/generate?exp=3600&iss=test&sub=user&apiKey=test-key" | jq

# 초기 설정 (POST)
curl -X POST "http://localhost:8080/webjwtgen/setup" \
  -d "password=testpass123&apiKey=sk-test-key" | jq

# 전체 초기화 (DELETE)
curl -X DELETE "http://localhost:8080/webjwtgen/setup" | jq
```

#### 일반적인 에러 메시지
| 에러 | 원인 | 해결 |
|------|------|------|
| `Keystore에서 개인키를 찾을 수 없습니다` | KeyStore 파일 손상 또는 별도 별도 실패 | 초기화 후 재설정 |
| `비밀번호 길이가 8자 이상이어야 합니다` | 약한 비밀번호 | 8자 이상 입력 |
| `setup-completed.flag 파일이 없습니다` | 초기 설정 미완료 | `/setup` POST 요청 필수 |

## 전체 테스트 흐름

### 1단계: 빌드 및 배포
```bash
# 1. 빌드 (test 스킵)
mvn clean package -DskipTests

# 2. Tomcat에 배포
sudo cp target/webjwtgen.war /var/lib/tomcat9/webapps/

# 3. Tomcat 재시작
sudo systemctl restart tomcat9

# 4. 배포 확인
sleep 3
curl -s http://localhost:8080/webjwtgen/setup | jq
```

### 2단계: 초기 설정
```bash
# 1. 설정 상태 확인
curl -s http://localhost:8080/webjwtgen/setup | jq

# 2. 초기 설정 실행
curl -X POST "http://localhost:8080/webjwtgen/setup" \
  -d "password=testpass123&apiKey=sk-test-key123" \
  -H "Content-Type: application/x-www-form-urlencoded" | jq

# 3. 설정 완료 확인
curl -s http://localhost:8080/webjwtgen/setup | jq

# 4. KeyStore 생성 확인
keytool -list -keystore /var/lib/tomcat9/webapps/webjwtgen/keystore.jks -storepass testpass123
```

### 3단계: JWT 생성
```bash
# 1. 기본 JWT 생성
curl -s "http://localhost:8080/webjwtgen/generate?exp=3600&iss=test-issuer&sub=test-subject&apiKey=sk-test-key123" | jq

# 2. JWT 토큰 구조 확인 (Base64 디코드)
# 응답에서 "token" 값을 복사하여 https://jwt.io에서 검증

# 3. 공개키 확인
curl -s "http://localhost:8080/webjwtgen/generate?exp=3600&iss=test&sub=user&apiKey=sk-test-key123" | jq '.publicKey'
```

### 4단계: 초기화 (테스트 후)
```bash
# 전체 재설정 (KeyStore, 플래그 모두 삭제)
curl -X DELETE "http://localhost:8080/webjwtgen/setup" | jq

# 설정 상태 확인 (setupCompleted: false 확인)
curl -s http://localhost:8080/webjwtgen/setup | jq
```

## 개발 시 빠른 테스트 스크립트

```bash
#!/bin/bash
# test-webjwtgen.sh

BASE_URL="http://localhost:8080/webjwtgen"
PASSWORD="testpass123"
API_KEY="sk-test-key123"

echo "=== 설정 상태 확인 ==="
curl -s "$BASE_URL/setup" | jq

echo -e "\n=== 초기 설정 실행 ==="
curl -X POST "$BASE_URL/setup" \
  -d "password=$PASSWORD&apiKey=$API_KEY" \
  -H "Content-Type: application/x-www-form-urlencoded" | jq

echo -e "\n=== JWT 생성 ==="
curl -s "$BASE_URL/generate?exp=3600&iss=dev&sub=test&apiKey=$API_KEY" | jq

echo -e "\n=== 테스트 완료 ==="
```

사용 방법:
```bash
chmod +x test-webjwtgen.sh
./test-webjwtgen.sh
```
