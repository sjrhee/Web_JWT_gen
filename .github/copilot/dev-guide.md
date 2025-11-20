# 개발 환경 설정

## 🖥️ Tomcat 설정

### 위치
- 설치 경로: `/var/lib/tomcat9/`
- WebApps: `/var/lib/tomcat9/webapps`
- 로그: `/var/lib/tomcat9/logs/`

### 포트
- HTTP: 8080
- HTTPS: 8443

### 명령어

```bash
# Tomcat 상태 확인
sudo systemctl status tomcat9

# Tomcat 시작
sudo systemctl start tomcat9

# Tomcat 중지
sudo systemctl stop tomcat9

# Tomcat 재시작
sudo systemctl restart tomcat9

# 실시간 로그 확인
tail -f /var/lib/tomcat9/logs/catalina.out

# systemctl 로그 확인
sudo journalctl -u tomcat9 -f
```

## 🏗️ 빌드 및 배포

### 1. 빌드
```bash
cd /home/ubuntu/Work/webjwtgen
mvn clean package -DskipTests
```

### 2. 배포
```bash
sudo cp target/webjwtgen.war /var/lib/tomcat9/webapps/
```

### 3. Tomcat 재시작
```bash
sudo systemctl restart tomcat9
```

## 📝 로깅 설정

### log4j2.xml 로깅 모드

**PRODUCTION 모드** (기본값 - 권장)
```xml
<Logger name="com.security.jwt" level="WARN" />
```
성능 최적화, WARN 이상 메시지만 기록

**DEBUG 모드** (개발/문제 해결)
```xml
<Logger name="com.security.jwt" level="DEBUG" />
```
모든 DEBUG 메시지 기록, 상세 추적

### 로그 위치
```
/var/lib/tomcat9/logs/webjwtgen.log
```

## 📂 주요 파일

| 파일 | 설명 |
|------|------|
| src/main/java/com/security/jwt/ | Java 소스 |
| src/main/webapp/ | JSP 및 정적 파일 |
| src/main/webapp/css/ | 스타일시트 |
| src/main/resources/log4j2.xml | 로깅 설정 |
| pom.xml | Maven 의존성 |

## 🔧 유용한 Maven 명령어

```bash
# 빌드 (테스트 제외)
mvn clean package -DskipTests

# 빌드 (테스트 포함)
mvn clean package

# 의존성 확인
mvn dependency:tree

# 클린
mvn clean
```

## 💻 VS Code 설정 권장사항

**extensions.json**
```json
{
  "recommendations": [
    "redhat.java",
    "vscjava.vscode-maven",
    "vscjava.vscode-spring-boot"
  ]
}
```

**settings.json**
```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "maven.executable.preferMavenFromPath": true,
  "[java]": {
    "editor.defaultFormatter": "redhat.java",
    "editor.formatOnSave": true
  }
}
```

## 🐛 문제 해결

### Tomcat 재배포 안 됨
```bash
# 1. Tomcat 중지
sudo systemctl stop tomcat9

# 2. 기존 WAR 및 폴더 제거
sudo rm /var/lib/tomcat9/webapps/webjwtgen.war
sudo rm -rf /var/lib/tomcat9/webapps/webjwtgen

# 3. 새 WAR 배포
sudo cp target/webjwtgen.war /var/lib/tomcat9/webapps/

# 4. Tomcat 시작
sudo systemctl start tomcat9
```

### 로그 확인
```bash
# 최근 로그 100줄
tail -n 100 /var/lib/tomcat9/logs/catalina.out

# 에러 로그만 필터
grep ERROR /var/lib/tomcat9/logs/catalina.out

# 실시간 모니터링
tail -f /var/lib/tomcat9/logs/webjwtgen.log
```

## 📊 빌드 과정

```
mvn clean
  ↓
컴파일 (src/main/java → target/classes)
  ↓
리소스 복사 (src/main/resources → target/classes)
  ↓
패키징 (target/webjwtgen.war 생성)
  ↓
배포 (Tomcat webapps 폴더)
```

## 🔐 보안 체크리스트

- [ ] 비밀번호는 코드에 하드코딩하지 않음
- [ ] 모든 입력값 검증
- [ ] 에러 메시지에서 민감 정보 노출 금지
- [ ] HTTPS 사용 (프로덕션)
- [ ] Keystore 파일 권한 관리 (644)

---

**최종 업데이트**: November 2025