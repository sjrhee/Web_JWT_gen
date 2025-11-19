# JWT Generator - EC256 기반 JWT 토큰 생성 시스템

EC256 (ECDSA with SHA-256) 기반의 안전한 JWT 토큰 생성 및 관리 시스템입니다.

## 📋 주요 기능

### 1. JWT 토큰 생성
- **알고리즘**: ES256 (ECDSA with SHA-256)
- **곡선**: P-256 (secp256r1)
- **입력 파라미터**:
  - `sub`: 주제 (Subject)
  - `iss`: 발급자 (Issuer)
  - `exp`: 만료 시간 (초 단위)

### 2. Keystore 관리
- 한 번만 실행 가능한 초기 설정
- Keystore 생성 및 EC256 키쌍 자동 생성
- 초기화 시 입력한 비밀번호로 Keystore 보호
- **Keystore 백업/복원**: 비밀번호로 보호된 백업 파일 생성/복원
- **비밀번호 변경**: Keystore 및 키 엔트리 비밀번호 동시 변경
- **시스템 리셋**: 강제 초기화 기능 (관리자용)

### 3. 관리자 기능 (admin.jsp)
- Keystore 백업 (비밀번호 입력으로 검증)
- Keystore 복원 (백업 파일 + 비밀번호로 검증)
- Keystore 비밀번호 변경
- 시스템 강제 리셋

## 🚀 빠른 시작

### 1. 초기 설정 페이지 접속
```
http://localhost:8080/webjwtgen/setup.jsp
```

### 2. 초기 설정 수행
- 비밀번호 입력 (8글자 이상)
- 비밀번호 확인
- 설정 완료 후 JWT 생성기로 자동 이동

### 3. JWT 생성
```
http://localhost:8080/webjwtgen/index.jsp
```

**필드 입력:**
- **만료 시간**: YYYY-MM-DD HH:mm:ss 형식
- **발급자**: JWT 발급 기관
- **주제**: JWT가 관하는 주체 (사용자 ID 등)

### 4. 관리 기능 (선택사항)
```
http://localhost:8080/webjwtgen/admin.jsp
```

**기능:**
- Keystore 백업
- Keystore 복원
- 비밀번호 변경
- 시스템 리셋

## 🔑 보안 정보

### 비밀번호 보관
- **저장 위치**: `/var/lib/tomcat9/webapps/webjwtgen/jwt-config.properties`
- **특징**: 초기화 시 설정한 비밀번호
- **사용처**: Keystore 접근, 백업/복원, 비밀번호 변경 검증
- **보안**: 파일 시스템의 접근 권한으로 보호

### Keystore 정보
- **파일**: `keystore.jks`
- **알고리즘**: EC (P-256 / secp256r1)
- **서명**: SHA256withECDSA
- **유효기간**: 10년
- **암호화**: 초기화 시 설정한 비밀번호로 보호
- **위치**: `/var/lib/tomcat9/webapps/webjwtgen/keystore.jks`

### 공개키
- **형식**: PEM (Privacy Enhanced Mail)
- **제공**: JWT 생성 후 함께 반환
- **용도**: 토큰 검증 시 사용

### Keystore 백업 파일
- **파일명**: `keystore.backup`
- **암호화**: 백업 시 입력한 비밀번호와 동일하게 암호화
- **위치**: 브라우저 다운로드 폴더

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
├── pom.xml                                  # Maven 설정
├── README.md                                # 이 파일
├── CURRENT_STATUS.md                        # 현재 상태
├── src/
│   ├── main/
│   │   ├── java/com/security/jwt/
│   │   │   ├── JwtServlet.java              # JWT 생성 서블릿
│   │   │   ├── SetupServlet.java            # 초기 설정 및 관리
│   │   │   └── service/
│   │   │       ├── JWTService.java          # JWT 생성 로직
│   │   │       ├── KeystoreService.java     # Keystore 관리
│   │   │       ├── CertificateService.java  # 인증서 생성
│   │   │       ├── PasswordService.java     # 비밀번호 처리
│   │   │       ├── FileService.java         # 파일 작업
│   │   │       └── ResponseService.java     # HTTP 응답
│   │   ├── resources/
│   │   │   └── log4j2.xml                   # 로깅 설정
│   │   └── webapp/
│   │       ├── index.jsp                    # JWT 생성 UI
│   │       ├── setup.jsp                    # 초기 설정 UI
│   │       ├── admin.jsp                    # 관리자 페이지
│   │       ├── admin.js                     # 관리자 스크립트
│   │       ├── admin.css                    # 관리자 스타일
│   │       ├── WEB-INF/
│   │       │   └── web.xml                  # 웹 설정
│   │       └── keystore.jks                 # Keystore (초기화 후 생성)
│   └── test/                                # 테스트 (미포함)
└── target/                                  # 빌드 결과물
```

## 🔄 엔드포인트

### JWT 생성
```
GET /webjwtgen/generate?sub=<SUBJECT>&iss=<ISSUER>&exp=<SECONDS>
```

**파라미터:**
- `sub`: 주제 (필수)
- `iss`: 발급자 (필수)
- `exp`: 만료 시간 (초 단위, 필수)

**응답:**
```json
{
  "success": true,
  "jwt": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9...",
  "publicKey": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"
}
```

### 초기화 상태 확인
```
GET /webjwtgen/setup
```

**응답:**
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

### Keystore 백업
```
POST /webjwtgen/setup?action=backup
Content-Type: application/x-www-form-urlencoded

password=<PASSWORD>
```

### Keystore 복원
```
POST /webjwtgen/setup?action=restore
Content-Type: multipart/form-data

password=<PASSWORD>
file=<BACKUP_FILE>
```

### 비밀번호 변경
```
POST /webjwtgen/setup?action=changePassword
Content-Type: application/x-www-form-urlencoded

currentPassword=<CURRENT_PASSWORD>&newPassword=<NEW_PASSWORD>&confirmPassword=<NEW_PASSWORD>
```

### 시스템 리셋 (강제 초기화)
```
POST /webjwtgen/setup?action=forceReset
Content-Type: application/x-www-form-urlencoded

password=<PASSWORD>&confirm=FORCE_RESET_CONFIRMED
```

## 🔐 관리자 접근

### 관리자 페이지 주소
```
http://localhost:8080/webjwtgen/admin.jsp
```

### 기능

#### 1. Keystore 백업
- 비밀번호 입력으로 검증
- 암호화된 백업 파일 다운로드
- 복원 시 동일한 비밀번호 필요

#### 2. Keystore 복원
- 백업 파일 선택
- 백업 시 사용한 비밀번호 입력
- 검증 후 복원

#### 3. 비밀번호 변경
- 현재 비밀번호 검증
- 새 비밀번호 입력 및 확인
- Keystore 및 키 엔트리 비밀번호 동시 변경

#### 4. 시스템 리셋
- 강제 초기화 (되돌릴 수 없는 작업)
- 모든 파일 삭제
- 초기 상태로 복구

## 🐛 트러블슈팅

### "비밀번호를 입력해주세요"
- 원인: JWT 생성/백업/복원 시 비밀번호 미제공
- 해결: 초기화 시 설정한 비밀번호 입력

### "비밀번호가 일치하지 않습니다"
- 원인: 입력한 비밀번호가 Keystore 비밀번호와 다름
- 해결: 초기화 시 입력한 정확한 비밀번호 사용

### "Keystore를 찾을 수 없습니다"
- 원인: 초기 설정이 완료되지 않음
- 해결: setup.jsp에서 초기 설정 진행

### "초기화가 완전히 리셋되었습니다"
- 의미: 강제 초기화 완료 후 시스템이 초기 상태로 복구
- 다음 단계: setup.jsp에서 다시 초기 설정 필요

### "이미 초기화되었습니다"
- 원인: 초기화가 이미 완료된 상태에서 재초기화 시도
- 해결: 강제 초기화(admin.jsp)를 통해 시스템 리셋 필요

### JWT 생성 실패 ("JWT 생성 실패")
- 원인: Keystore 비밀번호 오류 또는 파일 손상
- 해결: 백업 파일이 있다면 복원, 없다면 강제 초기화

## 📝 설정 파일 (jwt-config.properties)

초기화 후 자동 생성되는 설정 파일:

```properties
keystore.password=<USER_PASSWORD>
keystore.alias=ec256-jwt
keystore.path=/var/lib/tomcat9/webapps/webjwtgen/keystore.jks
```

**저장 위치:** `/var/lib/tomcat9/webapps/webjwtgen/jwt-config.properties`

## 🔄 라이프사이클

### 1단계: 초기 설정 (최초 1회)
```
setup.jsp 접근
↓
비밀번호 입력 및 확인
↓
Keystore + EC256 키쌍 자동 생성
↓
jwt-config.properties 생성 (비밀번호 저장)
↓
초기화 완료 플래그 생성
```

### 2단계: JWT 생성 (반복)
```
index.jsp에서 파라미터 입력 (sub, iss, exp)
↓
Keystore에서 개인키 로드
↓
EC256으로 서명
↓
공개키와 함께 JWT 반환
```

### 3단계: 관리 (필요시)
```
admin.jsp 접근
↓
비밀번호 입력으로 인증
↓
백업/복원/비밀번호변경/리셋 선택
↓
해당 작업 수행
```

## 📊 지원 포트

- **HTTP**: 8080 (모든 기능)
- **HTTPS**: 8443 (모든 기능, 설정 필요)

## ⚠️ 주의사항

1. **비밀번호 관리**: 초기화 시 설정한 비밀번호는 안전한 곳에 보관
2. **Keystore 백업**: 중요한 경우 주기적으로 백업 권장
3. **강제 초기화**: 되돌릴 수 없는 작업이므로 신중하게 실행
4. **비밀번호 변경**: Keystore 및 키 엔트리 비밀번호가 모두 변경됨
5. **복원 시 주의**: 다른 시스템에서 백업한 파일로 복원 시 모든 이전 JWT가 무효화됨

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

**버전**: 2.0.0 (November 2025)
**주요 변경사항**: 토큰 기반 인증 제거, 비밀번호 직접 검증 방식으로 전환, Keystore 백업/복원/비밀번호변경 기능 추가
**라이선스**: MIT
**저장소**: https://github.com/sjrhee/Web_JWT_gen
