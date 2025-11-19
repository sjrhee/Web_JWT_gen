<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>JWT 생성기 - 초기 설정</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }

        .container {
            background: white;
            border-radius: 10px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            padding: 40px;
            max-width: 400px;
            width: 100%;
        }

        .title {
            text-align: center;
            margin-bottom: 10px;
            color: #333;
            font-size: 28px;
            font-weight: 600;
        }

        .subtitle {
            text-align: center;
            margin-bottom: 30px;
            color: #666;
            font-size: 14px;
        }

        .form-group {
            margin-bottom: 20px;
        }

        label {
            display: block;
            margin-bottom: 8px;
            color: #333;
            font-weight: 500;
            font-size: 14px;
        }

        input[type="password"],
        input[type="text"] {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 14px;
            transition: border-color 0.3s, box-shadow 0.3s;
        }

        input[type="password"]:focus,
        input[type="text"]:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }

        .button-group {
            display: flex;
            gap: 10px;
            margin-top: 30px;
        }

        button {
            flex: 1;
            padding: 12px;
            border: none;
            border-radius: 5px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
        }

        .btn-setup {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
        }

        .btn-setup:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
        }

        .btn-setup:disabled {
            opacity: 0.6;
            cursor: not-allowed;
            transform: none;
        }

        .message {
            margin-top: 20px;
            padding: 12px;
            border-radius: 5px;
            font-size: 14px;
            display: none;
        }

        .message.success {
            background: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
            display: block;
        }

        .message.error {
            background: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
            display: block;
        }

        .info-box {
            background: #f8f9fa;
            border-left: 4px solid #667eea;
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 5px;
            font-size: 13px;
            color: #666;
            line-height: 1.6;
        }

        .loading {
            display: none;
            text-align: center;
            margin-top: 20px;
        }

        .spinner {
            border: 3px solid #f3f3f3;
            border-top: 3px solid #667eea;
            border-radius: 50%;
            width: 40px;
            height: 40px;
            animation: spin 1s linear infinite;
            margin: 0 auto;
        }

        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }

        .redirect-info {
            margin-top: 20px;
            padding: 15px;
            background: #d4edda;
            border: 1px solid #c3e6cb;
            border-radius: 5px;
            color: #155724;
            font-size: 14px;
            display: none;
            text-align: center;
        }

        .redirect-info a {
            color: #155724;
            font-weight: 600;
            text-decoration: none;
        }

        .redirect-info a:hover {
            text-decoration: underline;
        }
    </style>
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

        <!-- 강제 초기화 섹션 (이미 초기화된 경우) -->
        <div id="forceResetSection" style="display: none;">
            <div class="info-box" style="background: #fff3cd; border-color: #ffc107; color: #856404;">
                <strong>⚠️ 주의사항</strong><br>
                이미 초기화되었습니다. 강제 초기화를 하면 모든 설정과 키가 삭제됩니다.
                <br><br>
                이 작업은 되돌릴 수 없습니다. 계속하시겠습니까?
            </div>

            <form id="forceResetForm">
                <div class="form-group">
                    <label for="resetPassword">현재 비밀번호</label>
                    <input type="password" id="resetPassword" name="resetPassword" placeholder="현재 비밀번호 입력" required>
                </div>

                <div class="button-group">
                    <button type="submit" class="btn-setup" id="forceResetBtn" style="background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);">강제 초기화</button>
                    <button type="button" class="btn-setup" onclick="location.href='index.jsp'" style="background: #6c757d;">취소</button>
                </div>

                <div class="loading" id="forceResetLoading">
                    <div class="spinner"></div>
                    <p style="margin-top: 10px; color: #666; font-size: 14px;">초기화 중...</p>
                </div>

                <div class="message" id="forceResetMessage"></div>
            </form>
        </div>

    <script>
        // 초기화 상태
        let setupInProgress = false;
        let isAlreadySetup = false;

        // 초기 설정 폼 제출
        document.getElementById('setupForm').addEventListener('submit', async (e) => {
            e.preventDefault();

            // 이미 설정 중이거나 이미 설정되었으면 중단
            if (setupInProgress || isAlreadySetup) {
                showMessage('이미 초기화되었거나 처리 중입니다', 'error');
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
                    isAlreadySetup = true;
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

        // 강제 초기화 폼 제출
        document.getElementById('forceResetForm').addEventListener('submit', async (e) => {
            e.preventDefault();

            const password = document.getElementById('resetPassword').value;
            const messageEl = document.getElementById('forceResetMessage');
            const loadingEl = document.getElementById('forceResetLoading');
            const resetBtn = document.getElementById('forceResetBtn');

            if (!password) {
                showForceResetMessage('비밀번호를 입력해주세요', 'error');
                return;
            }

            // 로딩 상태
            loadingEl.style.display = 'block';
            messageEl.style.display = 'none';
            resetBtn.disabled = true;

            try {
                const response = await fetch('/webjwtgen/setup?password=' + encodeURIComponent(password) + '&confirm=FORCE_RESET_CONFIRMED', {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    }
                });

                console.log('Force reset response status:', response.status);

                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const data = await response.json();
                console.log('Force reset response:', data);
                
                loadingEl.style.display = 'none';

                if (data.success) {
                    showForceResetMessage('✅ ' + data.message, 'success');
                    // 2초 후 setup 페이지 새로고침
                    setTimeout(() => {
                        location.reload();
                    }, 2000);
                } else {
                    showForceResetMessage(data.error || '강제 초기화 실패', 'error');
                    resetBtn.disabled = false;
                }
            } catch (error) {
                loadingEl.style.display = 'none';
                console.error('Force reset error:', error);
                showForceResetMessage('오류: ' + error.message, 'error');
                resetBtn.disabled = false;
            }
        });

        function showMessage(message, type) {
            const messageEl = document.getElementById('message');
            messageEl.textContent = message;
            messageEl.className = 'message ' + type;
            messageEl.style.display = 'block';
        }

        function showForceResetMessage(message, type) {
            const messageEl = document.getElementById('forceResetMessage');
            messageEl.textContent = message;
            messageEl.className = 'message ' + type;
            messageEl.style.display = 'block';
        }

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
                    // 이미 초기화됨 - 강제 초기화 섹션 표시
                    isAlreadySetup = true;
                    document.getElementById('setupSection').style.display = 'none';
                    document.getElementById('forceResetSection').style.display = 'block';
                } else {
                    // 초기화되지 않음 - 초기 설정 섹션 표시
                    document.getElementById('setupSection').style.display = 'block';
                    document.getElementById('forceResetSection').style.display = 'none';
                }
            } catch (error) {
                console.log('초기화 상태 확인 실패:', error);
                // 에러가 발생했을 경우 초기 설정 폼 표시
                document.getElementById('setupSection').style.display = 'block';
                document.getElementById('forceResetSection').style.display = 'none';
            }
        });
    </script>
</body>
</html>
