# JWT 비밀번호 변경 기능 구현 상태

## 현재 문제점
- 비밀번호 변경 시 "현재 비밀번호가 일치하지 않습니다" 메시지 발생
- 원인: Properties 클래스의 이스케이프 처리로 인한 비밀번호 불일치

## 해결 방안
1. Properties.load()/store() 사용 제거
2. 직접 파일 읽기/쓰기로 이스케이프 처리 방지

## 수정 대상 파일 및 메서드

### 1. PasswordService.java
**메서드: verifyPassword(String inputPassword, String configPath)**
```java
// 수정 전: Properties.load() 사용 → 이스케이프 처리 발생
// 수정 후: 직접 파일 읽기로 변경
```
- 파일 내용을 직접 읽어서 "keystore.password=" 라인 찾기
- 입력 비밀번호와 직접 비교

### 2. KeystoreService.java
**메서드 1: saveConfig(String configPath, String apiKey, String keystorePassword)**
```java
// 수정 전: props.store()로 파일 저장 → 이스케이프 처리
// 수정 후: StringBuilder로 직접 문자열 조합하여 저장
// 형식: key=value (줄바꿈으로 구분)
```

**메서드 2: loadConfig(String configPath)**
```java
// 수정 전: props.load()로 파일 읽기 → 이스케이프 해석
// 수정 후: 직접 파일 읽기, 라인 파싱
```

### 3. SetupServlet.java
**파일 저장: storeApiKey() 메서드**
- StringBuilder로 다음 내용 작성:
  ```
  # JWT Configuration
  keystore.path=/path/to/keystore.jks
  api.key=ApiKeyValue
  keystore.password=PasswordValue
  keystore.alias=ec256-jwt
  ```

**비밀번호 검증: 다음 메서드들에서 PasswordService.verifyPassword() 호출**
1. doDelete() - 강제 초기화 (라인 ~160)
2. backupKeystore() - Keystore 백업 (라인 ~315)
3. restoreKeystore() - Keystore 복원 (라인 ~354)
4. handlePasswordChange() - 비밀번호 변경 (라인 ~442) ✅ 이미 수정됨

각 메서드에서:
```java
String webappPath = getServletContext().getRealPath("/");
String configPath = webappPath + "jwt-config.properties";
if (password == null || !PasswordService.verifyPassword(password, configPath)) {
    sendError(response, 401, "비밀번호가 일치하지 않습니다");
    return;
}
```

## 설정 파일 형식
```
# JWT Configuration
keystore.path=/var/lib/tomcat9/webapps/webjwtgen/keystore.jks
api.key=FinalPass123
keystore.password=FinalPass123
keystore.alias=ec256-jwt
```

## 테스트 계획
1. 빌드: `mvn clean package -DskipTests -q`
2. 배포: `sudo rm -rf /var/lib/tomcat9/webapps/webjwtgen*` + `sudo cp target/webjwtgen.war /var/lib/tomcat9/webapps/`
3. 초기 설정: `curl -X POST "http://localhost:8080/webjwtgen/setup" -d "password=Test123&confirmPassword=Test123"`
4. 비밀번호 변경: `curl -X PUT "http://localhost:8080/webjwtgen/setup" -d "currentPassword=Test123&newPassword=New456&confirmNewPassword=New456"`
5. 검증: 성공 응답 {"success":true,...} 확인
