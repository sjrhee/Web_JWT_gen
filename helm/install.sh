#!/bin/bash

# Helm 차트 설치 스크립트
# 사용법: ./install.sh [namespace]

NAMESPACE=${1:-default}
RELEASE_NAME="webjwtgen"
CHART_PATH="./webjwtgen"

echo "=== Web JWT Generator Helm 차트 설치 ==="
echo "Namespace: $NAMESPACE"
echo "Release Name: $RELEASE_NAME"
echo ""

# Namespace 생성 (이미 존재하면 무시됨)
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Helm 차트 설치
helm install $RELEASE_NAME $CHART_PATH \
  --namespace $NAMESPACE \
  --create-namespace

echo ""
echo "=== 설치 완료 ==="
echo "상태 확인: helm status $RELEASE_NAME -n $NAMESPACE"
echo "Pod 확인: kubectl get pods -n $NAMESPACE"
echo "Service 확인: kubectl get svc -n $NAMESPACE"
