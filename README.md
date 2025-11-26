# JWT Generator - ES256

ES256(ECDSA with SHA-256) 기반 JWT 토큰 생성 및 관리 시스템입니다.

## 🎯 주요 기능

- JWT 생성 (ES256 서명)
- Keystore 관리 (EC256 키쌍)
- Keystore 백업/복원
- 비밀번호 변경
- 시스템 리셋

## 🚀 시작하기

### 1단계: 초기 설정
```
https://localhost:8443/webjwtgen/setup.jsp
```
비밀번호를 입력하면 Keystore가 자동 생성됩니다 (8글자 이상).

### 2단계: JWT 생성
```
https://localhost:8443/webjwtgen/
```
다음 항목을 입력하여 JWT 토큰을 생성합니다:
- Keystore 비밀번호 (초기화 시 설정한 값)
- 만료 시간 (기본값: 10년 후)
- 발급자 (jwt-issuer)
- 주제 (user-123)

### 3단계: 관리 (선택사항)
```
https://localhost:8443/webjwtgen/admin.jsp
```
백업, 복원, 비밀번호 변경

## 📡 API 엔드포인트

### JWT 생성
```
GET /webjwtgen/generate?sub=USER_ID&iss=ISSUER&exp=TIMESTAMP&password=PASSWORD
```

### Keystore 백업
```
POST /webjwtgen/setup?action=backup
Body: password=PASSWORD
```

### Keystore 복원
```
POST /webjwtgen/setup?action=restore
Body: { "data": "BASE64_KEYSTORE", "password": "PASSWORD" }
```

### 비밀번호 변경
```
POST /webjwtgen/setup?action=changePassword
Body: currentPassword=OLD&newPassword=NEW&confirmPassword=NEW
```

## 📁 프로젝트 구조

```
src/main/java/com/security/jwt/
├── JwtServlet.java              # JWT 생성 엔드포인트
├── SetupServlet.java            # 초기화 및 관리
└── service/
    ├── KeystoreService.java     # Keystore 관리
    ├── JWTService.java          # JWT 로직
    ├── ResponseService.java     # HTTP 응답
    ├── SetupActionHandler.java  # Setup 액션 처리
    ├── SetupValidator.java      # 입력 검증
    └── SetupSessionManager.java # 세션 관리

src/main/webapp/
├── css/                         # 스타일시트
├── index.jsp                    # JWT 생성 UI
├── setup.jsp                    # 초기화 UI
└── admin.jsp                    # 관리자 UI
```

## 🔐 보안

- **암호화**: ES256 (ECDSA with SHA-256)
- **곡선**: P-256
- **키 유효기간**: 10년
- **Keystore 암호화**: 비밀번호로 보호
- **모든 요청**: Keystore 비밀번호 인증 필수

## 🛠️ 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 11 |
| Tomcat | 9 |
| Maven | 3.x |
| BouncyCastle | 1.70 |
| Log4j2 | 2.21.1 |

## 🐳 Docker 배포

### 이미지 가져오기
```bash
docker pull ghcr.io/sjrhee/web-jwt-gen:latest
```

### 컨테이너 실행
```bash
docker run -d -p 8443:8443 --name webjwtgen ghcr.io/sjrhee/web-jwt-gen:latest
```

### 접속
```
https://localhost:8443/webjwtgen/
```

## 📊 포트

- HTTPS: 8443 (권장)
- HTTP: 8080 (HTTPS로 자동 리다이렉트)

---

**버전**: 3.0.0 | **저장소**: https://github.com/sjrhee/Web_JWT_gen
