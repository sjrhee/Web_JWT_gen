# CipherTrust CRDP 정책 핵심 요약

### 1. 보안 정책

*  암호화 키 선택: 사용 권한을 부여한 AES 대칭키(성능우선)를 적용

*  암호화 형식: 형태유지암호화(FPE) 알고리즘 등 다양한 방식 지원

*  버전관리: 적용 여부 선택 가능하며 버전 표시(내/외부) 방법도 선택 가능  
   장점: 정책 변경 시 버전 관리를 지원하여 정책 변경 전 암호화 데이터도 복호화 가능  
   단점: 암호화 데이터에 포함되는 버전관리 데이터로 인한 코드 복잡도 증가

*  마스킹 포멧 선택: 마스킹 범위를 제외한 암호화 (FPE에서 효율적)

*  암호화 적용 문자셋: 아스키 코드 혹은 utf-8 코드 범위 (FPE에서 필요)

*  접근 정책 선택: 접근정책의 추가정책을 적용

### 2. 접근 정책

*  사용자 그룹 선택

*  사용자 그룹 외 접근 시 복호화 표시 방법을 오류, 암호문, 마스킹, 평문 중 선택  

*  Default를 평문으로 선택하면 접근 정책은 무의미

### 3. 사용자 정책

*  사용자 그룹 생성

*  사용자 입력: CRDP 접근 시 사용할 임의의 사용자 (CipherTrust 사용자 계정과는 별개)

*  사용자 전달 방법: JWT 또는 json의 username 항목으로 전달  
   JWT: 관리자가 생성하여 개발자에 전달하여 사용  
   json: 개발자가 임의 사용자값을 넣어 사용

### 4. JWT 적용 방안

*  비대칭키의 개인키로 헤더와 Payload를 서명한 header.payload.signature로 JWT를 생성

*  RESTful API 요청 헤더에 JWT를 포함해 CRDP 서비스로 전달

*  CRDP 서버에 공개키를 등록하면 CRDP는 JWT 서명 검증 후 payload 값을 추출

*  payload의 특정 claim에서 값을 추출하여 사용자를 특정

*  json에 포함한 사용자 데이터 보다 우선함

*  JWT를 CRDP 서버에서 활성화한 경우 JWT 서명검증을 통과해야 API가 작동


### 5. 기타

* RESTful API에 포함되는 요소  
  1 "Authorization: Bearer $JWT_TOKEN"  
  2 "http://crdp-url/v1/protect" or "http://crdp-url/v1/reveal"  
  3 "protection_policy_name":  
  4 "data": or "protected_data":

* Web UI JWT 발생기 제공






