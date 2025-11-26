#!/bin/bash

# Helm 차트 삭제 스크립트
# 사용법: ./uninstall.sh [namespace]

NAMESPACE=${1:-default}
RELEASE_NAME="webjwtgen"

echo "=== Web JWT Generator Helm 차트 삭제 ==="
echo "Namespace: $NAMESPACE"
echo "Release Name: $RELEASE_NAME"
echo ""

# Helm 차트 삭제
helm uninstall $RELEASE_NAME --namespace $NAMESPACE

echo ""
echo "=== 삭제 완료 ==="
