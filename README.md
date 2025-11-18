# JWT Generator - EC256 기반 JWT 토큰 생성 시스템

EC256 (ECDSA with SHA-256) 기반의 안전한 JWT 토큰 생성 및 관리 시스템입니다.

## 📋 주요 기능

### 1. JWT 토큰 생성
- **알고리즘**: ES256 (ECDSA with SHA-256)
- **곡선**: P-256 (secp256r1)
- **입력 파라미터**:
  - `key`: API Key (초기화 시 설정한 비밀번호)
  - `exp`: 만료 시간 (Unix timestamp)
  - `iss`: 발급자 (Issuer)
  - `sub`: 주제 (Subject)

### 2. 초기화 관리
- 한 번만 실행 가능한 초기 설정
- Keystore 생성 및 EC256 키쌍 자동 생성
- 초기화 시 입력한 비밀번호가 API Key로 설정
- 강제 초기화 기능 (관리자용)

### 3. 관리자 페이지
- 비밀번호로 보호된 관리자 접근
- 시스템 상태 확인
- 강제 초기화 (다단계 확인)

## 🚀 빠른 시작

### 1. 설정 페이지 접속
```
http://192.168.0.234:8080/webjwtgen/setup.jsp
```

### 2. 초기 설정
- 비밀번호 입력 (8글자 이상)
- 비밀번호 확인
- 설정 완료 후 자동으로 JWT 생성기로 이동

### 3. JWT 생성
```
http://192.168.0.234:8080/webjwtgen/index.jsp
```

필드 입력:
- **API Key**: 초기화 시 설정한 비밀번호
- **만료 시간**: YYYY MM DD HH:mm 형식
- **발급자**: JWT 발급 기관
- **주제**: JWT가 관하는 주체 (사용자 ID 등)

## 🔑 보안 정보

### API Key 보관
- **위치**: `/opt/tomcat9/webapps/webjwtgen/jwt-config.properties`
- **특징**: 초기화 시 설정한 비밀번호와 동일
- **보안**: 파일 시스템의 접근 권한으로 보호

### Keystore 정보
- **파일**: `keystore.jks`
- **알고리즘**: EC (P-256 / secp256r1)
- **서명**: SHA256withECDSA
- **유효기간**: 10년
- **암호화**: 초기화 시 설정한 비밀번호로 보호

### 공개키
- **형식**: PEM (Privacy Enhanced Mail)
- **제공**: JWT 생성 후 공개키 반환
- **용도**: 토큰 검증 시 사용

## 🛠️ 기술 스택

- **Java**: OpenJDK 8 (1.8.0_462)
- **빌드**: Maven 3.6.3
- **서버**: Apache Tomcat 9.0.99
- **보안**: Bouncy Castle 1.70
- **JSON**: Google Gson 2.10.1
- **웹**: Servlet 3.1, JSP 2.3, JSTL 1.2
- **암호화**: 
  - Keystore: JKS (Java KeyStore)
  - 키 쌍: EC 256-bit (P-256)
  - 서명: SHA256withECDSA

## 📁 프로젝트 구조

```
webjwtgen/
├── pom.xml                          # Maven 설정
├── src/
│   ├── main/
│   │   ├── java/com/example/jwt/
│   │   │   ├── JwtServlet.java      # JWT 생성 로직
│   │   │   └── SetupServlet.java    # 초기 설정 및 관리
│   │   └── webapp/
│   │       ├── index.jsp            # JWT 생성 UI
│   │       ├── setup.jsp            # 초기 설정 UI
│   │       └── admin.jsp            # 관리자 페이지
│   └── test/                        # (테스트 미포함)
└── target/                          # 빌드 결과물
```

## 🔄 엔드포인트

### JWT 생성
```
GET /webjwtgen/generate?key=<API_KEY>&exp=<TIMESTAMP>&iss=<ISSUER>&sub=<SUBJECT>
```

**응답**:
```json
{
  "success": true,
  "jwt": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "publicKey": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"
}
```

### 초기화 상태 확인
```
GET /webjwtgen/setup
```

**응답**:
```json
{
  "setupCompleted": true
}
```

### 초기 설정
```
POST /webjwtgen/setup
Content-Type: application/x-www-form-urlencoded

password=<PASSWORD>&confirmPassword=<PASSWORD>
```

### 강제 초기화 (관리자용)
```
DELETE /webjwtgen/setup?password=<PASSWORD>&confirm=FORCE_RESET_CONFIRMED
```

## 🔐 관리자 접근

### 관리자 페이지 주소
```
http://192.168.0.234:8080/webjwtgen/admin.jsp
```

### 인증
- 초기화 시 설정한 비밀번호 입력 필요
- 비밀번호 불일치 시 접근 불가

### 기능
- 시스템 상태 확인
- 강제 초기화 (3단계 확인)

## 🐛 트러블슈팅

### "API Key가 없거나 유효하지 않습니다"
- 원인: 입력한 API Key가 초기화 시 설정한 비밀번호와 다름
- 해결: 초기화 시 입력한 정확한 비밀번호 사용

### "Keystore를 찾을 수 없습니다"
- 원인: 초기 설정이 완료되지 않음
- 해결: 먼저 setup.jsp에서 초기 설정 진행

### "초기화가 완전히 리셋되었습니다"
- 의미: 강제 초기화 완료 후 시스템이 초기 상태로 복구
- 다음 단계: setup.jsp에서 다시 초기 설정 필요

## 📝 설정 파일 (jwt-config.properties)

초기화 후 자동 생성되는 설정 파일:

```properties
api.key=<USER_PASSWORD>
keystore.password=<USER_PASSWORD>
keystore.alias=ec256-jwt
keystore.path=/opt/tomcat9/webapps/webjwtgen/keystore.jks
```

## 🔄 라이프사이클

### 1단계: 초기 설정 (최초 1회)
```
setup.jsp에서 비밀번호 설정
↓
Keystore + EC256 키쌍 자동 생성
↓
jwt-config.properties 생성
↓
setup-completed.flag 생성
```

### 2단계: JWT 생성 (반복)
```
index.jsp에서 JWT 생성
↓
API Key 검증
↓
EC256으로 서명
↓
공개키와 함께 토큰 반환
```

### 3단계: 관리 (필요시)
```
admin.jsp에서 비밀번호 인증
↓
강제 초기화 확인
↓
모든 파일 삭제 후 초기 상태로 복구
```

## 📊 지원 포트

- **HTTP**: 8080 (index.jsp, setup.jsp, admin.jsp)
- **HTTPS**: 8443 (동일 기능)

## ⚠️ 주의사항

1. **비밀번호 관리**: 초기화 시 설정한 비밀번호는 안전한 곳에 보관
2. **강제 초기화**: 되돌릴 수 없는 작업이므로 신중하게 실행
3. **Keystore 백업**: 중요한 경우 주기적으로 백업 권장
4. **API Key 변경**: 현재는 강제 초기화 후 재설정만 가능

## 🔍 JWT 토큰 구조

생성된 JWT는 3부로 구성:

```
eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9     <- Header (Base64URL)
.eyJleHAiOjE2MDAwMDAwMDAsImlzcyI6ImlzIiwic3ViIjoic3UifQ    <- Payload (Base64URL)
.MEYCIQDBj...                            <- Signature (Base64URL)
```

**Header**:
```json
{
  "typ": "JWT",
  "alg": "ES256"
}
```

**Payload**:
```json
{
  "exp": 1700000000,
  "iss": "issuer",
  "sub": "subject",
  "iat": 1699900000
}
```

## 📈 성능

- **JWT 생성 시간**: < 100ms
- **키 로드 시간**: ~ 500ms (Keystore에서)
- **동시 접근**: Tomcat 기본 설정 (수백 동시 접근 가능)

## 📞 지원

시스템 상태 확인: `admin.jsp` → 시스템 상태 섹션

---

**버전**: 1.0.0 (November 2025)  
**라이선스**: MIT  
**작성자**: JWT Team
