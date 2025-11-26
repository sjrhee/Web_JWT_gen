#!/bin/bash

# 1. GitHub Container Registry 로그인 (최초 1회 필요)
# GitHub 설정 -> Developer settings -> Personal access tokens (Classic)에서 
# 'write:packages', 'read:packages', 'delete:packages' 권한이 있는 토큰(PAT)을 생성해야 합니다.
#
# 로그인 방법:
# export CR_PAT=YOUR_GITHUB_TOKEN
# echo $CR_PAT | docker login ghcr.io -u sjrhee --password-stdin

# 2. 이미지 푸시
docker push ghcr.io/sjrhee/web-jwt-gen:latest

# 3. 이미지 가져오기 명령
echo "docker pull ghcr.io/sjrhee/web-jwt-gen:latest"
