<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>JWT 생성기 - 초기 설정</title>
    <link rel="stylesheet" href="css/setup-admin.css">
</head>
<body>
    <div class="container">
        <h1 class="title">🔐 JWT 생성기</h1>
        <p class="subtitle">초기 설정</p>

        <!-- 초기 설정 섹션 (초기화되지 않은 경우) -->
        <div id="setupSection">
            <div class="info-box">
                <strong>첫 시작입니다!</strong><br>
                초기 설정을 위해 비밀번호를 입력해주세요. 이 비밀번호는 다음 용도로 사용됩니다:
                <br><br>
                ✅ <strong>Keystore 보호:</strong> 암호화된 키를 보호하는 비밀번호<br>
                ✅ <strong>힌트:</strong> 초기화 이후 관리자 기능에서 <strong>백업 Keystore를 복원</strong>할 수 있습니다.<br>
            </div>

            <form id="setupForm">
                <div class="form-group">
                    <label for="password">비밀번호</label>
                    <input type="password" id="password" name="password" placeholder="강력한 비밀번호 입력" required>
                </div>

                <div class="form-group">
                    <label for="confirmPassword">비밀번호 확인</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" placeholder="비밀번호 재입력" required>
                </div>

                <div class="button-group">
                    <button type="submit" class="btn-setup" id="setupBtn">초기 설정</button>
                </div>

                <div class="loading" id="loading">
                    <div class="spinner"></div>
                    <p style="margin-top: 10px; color: #666; font-size: 14px;">초기화 중...</p>
                </div>

                <div class="message" id="message"></div>

                <div class="redirect-info" id="redirectInfo">
                    <p>✅ 초기 설정이 완료되었습니다!</p>
                    <p style="margin-top: 10px;">
                        <a href="index.jsp">JWT 생성기로 이동 →</a>
                    </p>
                </div>
            </form>
        </div>

    <script>
        // 초기화 상태
        let setupInProgress = false;

        // 페이지 로드 시 초기화 상태 확인
        window.addEventListener('load', async () => {
            try {
                const response = await fetch('/webjwtgen/setup');
                if (!response.ok) {
                    throw new Error(`Status: ${response.status}`);
                }
                const data = await response.json();
                console.log('Setup status check:', data);
                
                if (data.setupCompleted) {
                    // 이미 초기화됨 - admin.jsp로 리다이렉트
                    alert('이미 초기화되었습니다. 관리 페이지로 이동합니다.');
                    window.location.href = 'admin.jsp';
                    return;
                }
            } catch (error) {
                console.log('초기화 상태 확인 실패:', error);
            }
        });

        // 초기 설정 폼 제출
        document.getElementById('setupForm').addEventListener('submit', async (e) => {
            e.preventDefault();

            // 이미 설정 중이면 중단
            if (setupInProgress) {
                showMessage('처리 중입니다', 'error');
                return;
            }

            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            const messageEl = document.getElementById('message');
            const loadingEl = document.getElementById('loading');
            const setupBtn = document.getElementById('setupBtn');
            const redirectInfo = document.getElementById('redirectInfo');

            // 검증
            if (!password || !confirmPassword) {
                showMessage('비밀번호를 입력해주세요', 'error');
                return;
            }

            if (password.length < 8) {
                showMessage('비밀번호는 8자 이상이어야 합니다', 'error');
                return;
            }

            if (password !== confirmPassword) {
                showMessage('비밀번호가 일치하지 않습니다', 'error');
                return;
            }

            // 로딩 상태
            setupInProgress = true;
            loadingEl.style.display = 'block';
            messageEl.style.display = 'none';
            setupBtn.disabled = true;

            try {
                const response = await fetch('/webjwtgen/setup', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: `password=${encodeURIComponent(password)}&confirmPassword=${encodeURIComponent(confirmPassword)}`
                });

                console.log('Response status:', response.status);

                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const text = await response.text();
                console.log('Raw response:', text);

                const data = JSON.parse(text);
                console.log('Parsed JSON:', data);
                
                loadingEl.style.display = 'none';

                if (data.success) {
                    setupBtn.style.display = 'none';
                    redirectInfo.style.display = 'block';
                    // 3초 후 자동 이동
                    setTimeout(() => {
                        window.location.href = 'index.jsp';
                    }, 3000);
                } else {
                    showMessage(data.error || '초기 설정 실패', 'error');
                    setupBtn.disabled = false;
                    setupInProgress = false;
                }
            } catch (error) {
                loadingEl.style.display = 'none';
                console.error('Setup error:', error);
                showMessage('오류: ' + error.message, 'error');
                setupBtn.disabled = false;
                setupInProgress = false;
            }
        });

        function showMessage(message, type) {
            const messageEl = document.getElementById('message');
            messageEl.textContent = message;
            messageEl.className = 'message ' + type;
            messageEl.style.display = 'block';
        }
    </script>
</body>
</html>
