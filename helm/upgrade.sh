#!/bin/bash

# Helm 차트 업그레이드 스크립트
# 사용법: ./upgrade.sh [namespace]

NAMESPACE=${1:-default}
RELEASE_NAME="webjwtgen"
CHART_PATH="./webjwtgen"

echo "=== Web JWT Generator Helm 차트 업그레이드 ==="
echo "Namespace: $NAMESPACE"
echo "Release Name: $RELEASE_NAME"
echo ""

# Helm 차트 업그레이드
helm upgrade $RELEASE_NAME $CHART_PATH \
  --namespace $NAMESPACE \
  --install

echo ""
echo "=== 업그레이드 완료 ==="
echo "상태 확인: helm status $RELEASE_NAME -n $NAMESPACE"
echo "Pod 확인: kubectl get pods -n $NAMESPACE"
