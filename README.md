# JWT Generator - ES256 기반 JWT 생성 시스템

ES256 (ECDSA with SHA-256) 기반의 안전한 JWT 토큰 생성 및 관리 시스템입니다.

## 📋 주요 기능

- **JWT 생성**: ES256 알고리즘으로 서명된 JWT 토큰 생성
- **Keystore 관리**: EC256 키쌍 생성 및 보관
- **백업/복원**: 암호화된 Keystore 백업 및 복원
- **비밀번호 변경**: Keystore 비밀번호 변경
- **시스템 리셋**: 강제 초기화 기능

## 🚀 빠른 시작

### 1. 초기 설정
```
http://localhost:8080/webjwtgen/setup.jsp
https://localhost:8443/webjwtgen/setup.jsp
```
- 비밀번호 입력 (8글자 이상)
- Keystore 자동 생성

### 2. JWT 생성
```
http://localhost:8080/webjwtgen/index.jsp
https://localhost:8443/webjwtgen/index.jsp
```
- **Keystore 비밀번호**: 필수 (초기화 시 설정)
- **만료 시간**: 기본값 10년 후
- **발급자**: JWT 발급 기관
- **주제**: 토큰이 관하는 주체 (사용자 ID 등)

### 3. 관리 기능
```
http://localhost:8080/webjwtgen/admin.jsp
https://localhost:8443/webjwtgen/admin.jsp
```
- Keystore 백업/복원
- 비밀번호 변경
- 시스템 리셋

## 🔄 엔드포인트

### JWT 생성
```
GET /webjwtgen/generate?sub=<SUBJECT>&iss=<ISSUER>&exp=<SECONDS>&password=<PASSWORD>
```

### Keystore 백업
```
POST /webjwtgen/setup?action=backup
Content-Type: application/x-www-form-urlencoded

password=<PASSWORD>
```

### Keystore 복원
```
POST /webjwtgen/setup?action=restore
Content-Type: application/json

{
  "data": "<BASE64_ENCODED_KEYSTORE>",
  "password": "<PASSWORD>"
}
```

### 비밀번호 변경
```
POST /webjwtgen/setup?action=changePassword
Content-Type: application/x-www-form-urlencoded

currentPassword=<CURRENT>&newPassword=<NEW>&confirmPassword=<NEW>
```

## 📊 지원 포트

- **HTTP**: 8080
- **HTTPS**: 8443 (자체 서명 SSL 인증서)

## 🔐 보안

- **알고리즘**: ES256 (ECDSA with SHA-256)
- **곡선**: P-256 (secp256r1)
- **키 유효기간**: 10년
- **Keystore 암호화**: 비밀번호로 보호
- **모든 API 요청**: Keystore 비밀번호 인증 필수

## 📁 프로젝트 구조

```
src/main/java/com/security/jwt/
├── JwtServlet.java           # JWT 생성
├── SetupServlet.java         # 초기 설정 및 관리
└── service/
    ├── JWTService.java       # JWT 로직
    ├── KeystoreService.java  # Keystore 관리
    └── ...

src/main/webapp/
├── index.jsp                 # JWT 생성 UI
├── setup.jsp                 # 초기 설정 UI
├── admin.jsp                 # 관리자 페이지
└── ...
```

## 🛠️ 기술 스택

- Java 11, Maven
- Apache Tomcat 9
- BouncyCastle 1.70 (암호화)
- Google Gson 2.10.1 (JSON)

## 📝 라이센스

MIT

---

**버전**: 3.0.0 (November 2025)
**저장소**: https://github.com/sjrhee/Web_JWT_gen
