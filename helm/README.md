# Web JWT Generator - Helm Chart

이 디렉토리는 Web JWT Generator 애플리케이션을 Kubernetes에 배포하기 위한 Helm 차트를 포함합니다.

## 사전 요구사항

- Kubernetes 클러스터 (v1.19+)
- Helm 3.x
- kubectl 설정 완료

## 디렉토리 구조

```
helm/
├── webjwtgen/              # Helm 차트
│   ├── Chart.yaml          # 차트 메타데이터
│   ├── values.yaml         # 기본 설정값
│   └── templates/          # Kubernetes 리소스 템플릿
│       ├── deployment.yaml # Deployment 정의
│       ├── service.yaml    # Service 정의
│       └── ingress.yaml    # Ingress 정의 (선택)
├── install.sh              # 설치 스크립트
├── upgrade.sh              # 업그레이드 스크립트
├── uninstall.sh            # 삭제 스크립트
└── README.md               # 이 파일
```

## 빠른 시작

### 1. 설치

기본 namespace(default)에 설치:
```bash
cd helm
chmod +x *.sh
./install.sh
```

특정 namespace에 설치:
```bash
./install.sh my-namespace
```

### 2. 업그레이드

```bash
./upgrade.sh [namespace]
```

### 3. 삭제

```bash
./uninstall.sh [namespace]
```

## 수동 설치

### Helm 명령어로 직접 설치

```bash
# 설치
helm install webjwtgen ./webjwtgen -n default

# 업그레이드
helm upgrade webjwtgen ./webjwtgen -n default

# 삭제
helm uninstall webjwtgen -n default
```

### 커스텀 값 사용

`custom-values.yaml` 파일을 만들어 설정을 오버라이드할 수 있습니다:

```yaml
replicaCount: 2

image:
  tag: "v1.0.0"

service:
  type: LoadBalancer

ingress:
  enabled: true
  hosts:
    - host: webjwtgen.example.com
      paths:
        - path: /
          pathType: Prefix
```

설치 시 적용:
```bash
helm install webjwtgen ./webjwtgen -f custom-values.yaml -n default
```

## 주요 설정 (values.yaml)

| 파라미터 | 설명 | 기본값 |
|---------|------|--------|
| `replicaCount` | Pod 복제 개수 | `1` |
| `image.repository` | Docker 이미지 저장소 | `ghcr.io/sjrhee/web-jwt-gen` |
| `image.tag` | 이미지 태그 | `latest` |
| `service.type` | Service 타입 | `ClusterIP` |
| `service.httpPort` | HTTP 포트 | `8080` |
| `service.httpsPort` | HTTPS 포트 | `8443` |
| `ingress.enabled` | Ingress 활성화 | `false` |
| `resources.limits.cpu` | CPU 제한 | `500m` |
| `resources.limits.memory` | 메모리 제한 | `512Mi` |

## 애플리케이션 접근

### ClusterIP (기본)

Port-forward를 사용하여 로컬에서 접근:
```bash
kubectl port-forward svc/webjwtgen 8080:8080 -n default
```

브라우저에서 `http://localhost:8080/webjwtgen` 접속

### LoadBalancer

`values.yaml`에서 `service.type: LoadBalancer`로 변경 후:
```bash
kubectl get svc webjwtgen -n default
```

EXTERNAL-IP를 확인하여 접근

### Ingress

`values.yaml`에서 `ingress.enabled: true`로 설정하고 호스트 구성

## 상태 확인

```bash
# Helm 릴리스 상태
helm status webjwtgen -n default

# Pod 상태
kubectl get pods -n default

# Service 상태
kubectl get svc -n default

# 로그 확인
kubectl logs -f deployment/webjwtgen -n default
```

## 트러블슈팅

### Pod가 시작되지 않는 경우

```bash
kubectl describe pod <pod-name> -n default
kubectl logs <pod-name> -n default
```

### 이미지를 가져올 수 없는 경우

GitHub Container Registry가 private인 경우 imagePullSecret 설정 필요:
```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=<username> \
  --docker-password=<token> \
  -n default
```

## 참고사항

- 이 차트는 기존 Docker 이미지 `ghcr.io/sjrhee/web-jwt-gen:latest`를 사용합니다
- 애플리케이션은 HTTP(8080)와 HTTPS(8443) 포트를 모두 지원합니다
- 프로덕션 환경에서는 리소스 제한과 replica 수를 적절히 조정하세요
