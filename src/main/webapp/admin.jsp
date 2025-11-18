<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>JWT 생성기 - 관리자 페이지</title>
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
            padding: 20px;
        }

        .container {
            max-width: 800px;
            margin: 0 auto;
            background: white;
            border-radius: 10px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            overflow: hidden;
        }

        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }

        .header h1 {
            margin-bottom: 10px;
            font-size: 28px;
        }

        .header p {
            opacity: 0.9;
            font-size: 14px;
        }

        .content {
            padding: 30px;
        }

        .section {
            margin-bottom: 30px;
            padding: 20px;
            background: #f8f9fa;
            border-left: 4px solid #667eea;
            border-radius: 5px;
        }

        .section h2 {
            color: #333;
            margin-bottom: 15px;
            font-size: 20px;
        }

        .warning-box {
            background: #fff3cd;
            border: 2px solid #ffc107;
            border-radius: 5px;
            padding: 15px;
            margin-bottom: 15px;
        }

        .warning-title {
            color: #856404;
            font-weight: 600;
            margin-bottom: 10px;
            display: flex;
            align-items: center;
        }

        .warning-icon {
            font-size: 20px;
            margin-right: 10px;
        }

        .warning-text {
            color: #856404;
            font-size: 14px;
            line-height: 1.6;
        }

        .danger-warning {
            background: #f8d7da;
            border: 2px solid #dc3545;
            border-radius: 5px;
            padding: 15px;
            margin-bottom: 15px;
        }

        .danger-title {
            color: #721c24;
            font-weight: 600;
            margin-bottom: 10px;
            display: flex;
            align-items: center;
        }

        .danger-icon {
            font-size: 22px;
            margin-right: 10px;
        }

        .danger-text {
            color: #721c24;
            font-size: 14px;
            line-height: 1.6;
            margin-bottom: 10px;
        }

        .info-list {
            background: white;
            border-radius: 5px;
            padding: 15px;
            margin-bottom: 15px;
        }

        .info-item {
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid #e9ecef;
        }

        .info-item:last-child {
            border-bottom: none;
        }

        .info-label {
            font-weight: 600;
            color: #666;
        }

        .info-value {
            color: #333;
            word-break: break-all;
        }

        .button-group {
            display: flex;
            gap: 10px;
            margin-top: 20px;
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

        .btn-reset {
            background: #dc3545;
            color: white;
        }

        .btn-reset:hover {
            background: #c82333;
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(220, 53, 69, 0.3);
        }

        .btn-backup {
            background: #28a745;
            color: white;
        }

        .btn-backup:hover {
            background: #218838;
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(40, 167, 69, 0.3);
        }

        .btn-restore {
            background: #007bff;
            color: white;
        }

        .btn-restore:hover {
            background: #0056b3;
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(0, 86, 179, 0.3);
        }

        .btn-cancel {
            background: #6c757d;
            color: white;
        }

        .btn-cancel:hover {
            background: #5a6268;
        }

        .modal {
            display: none;
            position: fixed;
            z-index: 1000;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0, 0, 0, 0.5);
        }

        .modal.active {
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .modal-content {
            background: white;
            border-radius: 10px;
            padding: 30px;
            max-width: 500px;
            width: 90%;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        }

        .modal-header {
            font-size: 24px;
            font-weight: 600;
            color: #dc3545;
            margin-bottom: 20px;
            display: flex;
            align-items: center;
        }

        .modal-icon {
            font-size: 32px;
            margin-right: 15px;
        }

        .modal-body {
            color: #666;
            line-height: 1.8;
            margin-bottom: 20px;
        }

        .risk-list {
            background: #f8d7da;
            border-left: 4px solid #dc3545;
            padding: 15px;
            margin: 15px 0;
            border-radius: 3px;
        }

        .risk-list li {
            color: #721c24;
            margin-bottom: 8px;
            margin-left: 20px;
        }

        .risk-list li:last-child {
            margin-bottom: 0;
        }

        .confirm-section {
            background: #f0f0f0;
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
        }

        .confirm-checkbox {
            display: flex;
            align-items: center;
            margin-bottom: 10px;
        }

        .confirm-checkbox input {
            width: 20px;
            height: 20px;
            margin-right: 10px;
            cursor: pointer;
        }

        .confirm-checkbox label {
            cursor: pointer;
            color: #333;
            font-size: 14px;
        }

        .modal-buttons {
            display: flex;
            gap: 10px;
        }

        .btn-confirm {
            flex: 1;
            background: #dc3545;
            color: white;
            padding: 12px;
            border: none;
            border-radius: 5px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
            disabled: opacity 0.5;
        }

        .btn-confirm:hover:not(:disabled) {
            background: #c82333;
            transform: translateY(-2px);
        }

        .btn-confirm:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }

        .btn-close-modal {
            flex: 1;
            background: #6c757d;
            color: white;
            padding: 12px;
            border: none;
            border-radius: 5px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
        }

        .btn-close-modal:hover {
            background: #5a6268;
        }

        .message {
            margin-bottom: 20px;
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

        .info-status {
            display: inline-block;
            padding: 5px 10px;
            border-radius: 3px;
            font-size: 12px;
            font-weight: 600;
        }

        .status-completed {
            background: #d4edda;
            color: #155724;
        }

        .status-pending {
            background: #d1ecf1;
            color: #0c5460;
        }

        .back-link {
            display: inline-block;
            margin-bottom: 20px;
            color: #667eea;
            text-decoration: none;
            font-weight: 600;
        }

        .back-link:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔧 관리자 페이지</h1>
            <p>JWT 생성기 시스템 관리</p>
        </div>

        <div class="content">
            <!-- 비밀번호 입력 화면 -->
            <div id="loginSection" style="display: none;">
                <div class="section" style="text-align: center; margin-bottom: 0;">
                    <h2 style="margin-bottom: 30px;">🔐 관리자 인증</h2>
                    
                    <div style="max-width: 400px; margin: 0 auto;">
                        <div class="form-group" style="margin-bottom: 20px;">
                            <input type="password" id="adminPassword" placeholder="관리자 비밀번호 입력" 
                                   style="width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 5px; font-size: 14px;">
                            <div class="help-text" style="font-size: 12px; color: #999; margin-top: 5px;">
                                초기화할 때 설정한 비밀번호를 입력하세요
                            </div>
                        </div>
                        
                        <div style="display: flex; gap: 10px;">
                            <button onclick="authenticateAdmin()" style="flex: 1; background: #667eea; color: white; padding: 12px; border: none; border-radius: 5px; cursor: pointer; font-weight: 600;">
                                인증
                            </button>
                            <button onclick="goBack()" style="flex: 1; background: #6c757d; color: white; padding: 12px; border: none; border-radius: 5px; cursor: pointer; font-weight: 600;">
                                돌아가기
                            </button>
                        </div>

                        <div id="authError" class="error" style="margin-top: 20px; display: none; color: #dc3545; background: #f8d7da; border: 1px solid #f5c6cb; padding: 12px; border-radius: 5px;"></div>
                    </div>
                </div>
            </div>

            <!-- 관리자 기능 화면 -->
            <div id="adminSection" style="display: none;">
                <a href="index.jsp" class="back-link">← JWT 생성기로 돌아가기</a>

            <div class="message" id="message"></div>

            <!-- 시스템 상태 섹션 -->
            <div class="section">
                <h2>📊 시스템 상태</h2>
                <div class="info-list">
                    <div class="info-item">
                        <span class="info-label">초기화 상태:</span>
                        <span class="info-value">
                            <span id="setupStatus" class="info-status status-pending">확인 중...</span>
                        </span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">서버 시간:</span>
                        <span class="info-value" id="serverTime">-</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">애플리케이션:</span>
                        <span class="info-value">JWT 생성기 (EC256)</span>
                    </div>
                </div>
            </div>

            <!-- 강제 초기화 섹션 -->
            <div class="section">
                <h2>🔄 강제 초기화</h2>
                
                <div class="warning-box">
                    <div class="warning-title">
                        <span class="warning-icon">⚠️</span>
                        주의사항
                    </div>
                    <div class="warning-text">
                        강제 초기화 기능은 관리자만 사용할 수 있습니다. 
                        이 작업은 되돌릴 수 없습니다.
                    </div>
                </div>

                <div class="info-list">
                    <div class="info-item">
                        <span class="info-label">기능:</span>
                        <span class="info-value">모든 설정 및 키 초기화</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">영향 범위:</span>
                        <span class="info-value">
                            • Keystore 삭제<br>
                            • 설정 파일 삭제<br>
                            • 모든 JWT 생성 중단
                        </span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">복구 방법:</span>
                        <span class="info-value">초기 설정 페이지에서 다시 설정</span>
                    </div>
                </div>

                <div class="button-group">
                    <button class="btn-reset" onclick="openResetModal()">🔴 강제 초기화</button>
                </div>
            </div>

            <!-- Keystore 백업/복원 섹션 -->
            <div class="section">
                <h2>💾 Keystore 관리</h2>
                
                <div class="warning-box">
                    <div class="warning-title">
                        <span class="warning-icon">💡</span>
                        Keystore 백업
                    </div>
                    <div class="warning-text">
                        Keystore를 백업받아 안전하게 보관하세요. 
                        필요 시 복원하여 동일한 키를 재사용할 수 있습니다.
                    </div>
                </div>

                <div class="info-list">
                    <div class="info-item">
                        <span class="info-label">백업 기능:</span>
                        <span class="info-value">현재 Keystore를 파일로 다운로드</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">복원 기능:</span>
                        <span class="info-value">백업된 Keystore 파일 업로드</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">복원 시 효과:</span>
                        <span class="info-value">
                            • 이전 EC256 키쌍 재사용<br>
                            • 발급한 기존 JWT와 호환성 유지<br>
                            • 현재 Keystore 자동 백업
                        </span>
                    </div>
                </div>

                <div class="button-group">
                    <button class="btn-backup" onclick="backupKeystore()">📥 Keystore 다운로드</button>
                    <button class="btn-restore" onclick="document.getElementById('keystoreFile').click()">📤 Keystore 복원</button>
                </div>
                <input type="file" id="keystoreFile" style="display: none;" accept=".jks" onchange="restoreKeystore()">
                
                <div id="backupMessage" class="message" style="margin-top: 20px;"></div>
            </div>

            <!-- 추가 정보 -->
            <div class="section">
                <h2>ℹ️ 정보</h2>
                <div class="info-list">
                    <div class="info-item">
                        <span class="info-label">JWT 생성기 URL:</span>
                        <span class="info-value" id="generatorUrl">-</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">초기 설정 URL:</span>
                        <span class="info-value" id="setupUrl">-</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">API 엔드포인트:</span>
                        <span class="info-value">/webjwtgen/generate</span>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 강제 초기화 확인 모달 -->
    <div class="modal" id="resetModal">
        <div class="modal-content">
            <div class="modal-header">
                <span class="modal-icon">⚠️</span>
                강제 초기화 확인
            </div>

            <div class="modal-body">
                <strong>정말로 초기화하시겠습니까?</strong><br><br>
                이 작업은 <strong>되돌릴 수 없습니다</strong>. 
                아래 항목들이 삭제됩니다:
                
                <ul class="risk-list">
                    <li>모든 암호화 키 (keystore.jks)</li>
                    <li>시스템 설정 (jwt-config.properties)</li>
                    <li>초기화 상태 플래그</li>
                    <li>생성된 모든 JWT는 유효하지 않음</li>
                </ul>

                <strong style="color: #dc3545;">⚡ 경고:</strong>
                <div style="color: #721c24; font-size: 13px; margin-top: 10px; line-height: 1.6;">
                    강제 초기화 후 재초기화 전까지 JWT 생성이 불가능합니다.
                    시스템이 완전히 재설정될 때까지 사용 중단됩니다.
                </div>
            </div>

            <div class="confirm-section">
                <div class="confirm-checkbox">
                    <input type="checkbox" id="confirmCheck1" onchange="updateConfirmButton()">
                    <label for="confirmCheck1">
                        이 작업이 되돌릴 수 없음을 이해합니다
                    </label>
                </div>
                <div class="confirm-checkbox">
                    <input type="checkbox" id="confirmCheck2" onchange="updateConfirmButton()">
                    <label for="confirmCheck2">
                        모든 설정 및 키가 삭제됨을 이해합니다
                    </label>
                </div>
                <div class="confirm-checkbox">
                    <input type="checkbox" id="confirmCheck3" onchange="updateConfirmButton()">
                    <label for="confirmCheck3">
                        <strong>강제 초기화에 동의합니다</strong>
                    </label>
                </div>
            </div>

            <div class="modal-buttons">
                <button class="btn-confirm" id="confirmResetBtn" onclick="forceReset()" disabled>
                    🔴 강제 초기화 실행
                </button>
                <button class="btn-close-modal" onclick="closeResetModal()">
                    취소
                </button>
            </div>
        </div>
    </div>

    <script>
        // 저장된 토큰 확인
        let adminToken = null;

        // 페이지 로드 시 초기화
        window.addEventListener('load', () => {
            adminToken = sessionStorage.getItem('adminToken');
            if (adminToken) {
                showAdminSection();
            } else {
                showLoginSection();
            }
        });

        // 로그인 섹션 표시
        function showLoginSection() {
            document.getElementById('loginSection').style.display = 'block';
            document.getElementById('adminSection').style.display = 'none';
            adminToken = null;
        }

        // 관리자 섹션 표시
        function showAdminSection() {
            document.getElementById('loginSection').style.display = 'none';
            document.getElementById('adminSection').style.display = 'block';
            checkSetupStatus();
            setUrls();
            updateServerTime();
            setInterval(updateServerTime, 1000);
        }

        // 관리자 인증
        async function authenticateAdmin() {
            const password = document.getElementById('adminPassword').value;
            
            if (!password) {
                showAuthError('비밀번호를 입력해주세요');
                return;
            }

            try {
                // 간단한 검증: 비밀번호를 POST 요청으로 검증
                const response = await fetch('/webjwtgen/setup?password=' + encodeURIComponent(password) + '&confirm=AUTH_TEST', {
                    method: 'DELETE'
                });

                const data = await response.json();

                if (data.error && data.error.includes('일치하지')) {
                    showAuthError('비밀번호가 일치하지 않습니다');
                    return;
                }

                // 실제로는 서버에서 검증해야 하지만, 여기서는 클라이언트 토큰으로 관리
                // 더 안전한 방식: 서버에서 발급한 토큰 사용
                adminToken = btoa(password + ':' + new Date().getTime());
                sessionStorage.setItem('adminToken', adminToken);
                showAdminSection();
            } catch (error) {
                showAuthError('인증 오류: ' + error.message);
            }
        }

        // 인증 오류 표시
        function showAuthError(message) {
            document.getElementById('authError').textContent = message;
            document.getElementById('authError').style.display = 'block';
        }

        // 돌아가기
        function goBack() {
            window.location.href = 'index.jsp';
        }

        // 초기화 상태 확인
        async function checkSetupStatus() {
            try {
                const response = await fetch('/webjwtgen/setup');
                const data = await response.json();
                
                const statusEl = document.getElementById('setupStatus');
                if (data.setupCompleted) {
                    statusEl.textContent = '초기화 완료';
                    statusEl.className = 'info-status status-completed';
                } else {
                    statusEl.textContent = '미초기화';
                    statusEl.className = 'info-status status-pending';
                }
            } catch (error) {
                console.error('상태 확인 실패:', error);
            }
        }

        // URL 설정
        function setUrls() {
            const baseUrl = window.location.protocol + '//' + window.location.host + '/webjwtgen';
            document.getElementById('generatorUrl').textContent = baseUrl + '/index.jsp';
            document.getElementById('setupUrl').textContent = baseUrl + '/setup.jsp';
        }

        // 서버 시간 설정
        function updateServerTime() {
            const now = new Date();
            document.getElementById('serverTime').textContent = now.toLocaleString('ko-KR');
        }

        // 강제 초기화 모달 열기
        function openResetModal() {
            document.getElementById('resetModal').classList.add('active');
            document.getElementById('confirmCheck1').checked = false;
            document.getElementById('confirmCheck2').checked = false;
            document.getElementById('confirmCheck3').checked = false;
            updateConfirmButton();
        }

        // 강제 초기화 모달 닫기
        function closeResetModal() {
            document.getElementById('resetModal').classList.remove('active');
        }

        // 확인 버튼 업데이트
        function updateConfirmButton() {
            const check1 = document.getElementById('confirmCheck1').checked;
            const check2 = document.getElementById('confirmCheck2').checked;
            const check3 = document.getElementById('confirmCheck3').checked;
            
            const btn = document.getElementById('confirmResetBtn');
            btn.disabled = !(check1 && check2 && check3);
        }

        // 강제 초기화 실행
        async function forceReset() {
            const btn = document.getElementById('confirmResetBtn');
            btn.disabled = true;
            btn.textContent = '초기화 중...';

            try {
                const password = atob(adminToken).split(':')[0];
                const response = await fetch('/webjwtgen/setup?password=' + encodeURIComponent(password) + '&confirm=FORCE_RESET_CONFIRMED', {
                    method: 'DELETE'
                });

                const data = await response.json();
                closeResetModal();

                if (data.success) {
                    showMessage(data.message, 'success');
                    setTimeout(() => {
                        sessionStorage.removeItem('adminToken');
                        window.location.href = 'setup.jsp';
                    }, 2000);
                } else {
                    showMessage(data.error || '초기화 실패', 'error');
                    btn.disabled = false;
                    btn.textContent = '🔴 강제 초기화 실행';
                }
            } catch (error) {
                showMessage('오류: ' + error.message, 'error');
                btn.disabled = false;
                btn.textContent = '🔴 강제 초기화 실행';
            }
        }

        // 메시지 표시
        function showMessage(message, type) {
            const msgEl = document.getElementById('message');
            msgEl.textContent = message;
            msgEl.className = 'message ' + type;
        }

        // Keystore 백업 다운로드
        async function backupKeystore() {
            const password = atob(adminToken).split(':')[0];
            
            try {
                const response = await fetch('/webjwtgen/setup?action=backup&password=' + encodeURIComponent(password));
                
                if (!response.ok) {
                    const data = await response.json();
                    showBackupMessage(data.error || '백업 실패', 'error');
                    return;
                }

                const data = await response.json();
                
                if (data.success) {
                    // Base64 데이터를 Blob으로 변환
                    const binaryString = atob(data.data);
                    const bytes = new Uint8Array(binaryString.length);
                    for (let i = 0; i < binaryString.length; i++) {
                        bytes[i] = binaryString.charCodeAt(i);
                    }
                    const blob = new Blob([bytes], { type: 'application/octet-stream' });
                    
                    // 다운로드
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = data.filename || 'keystore.jks';
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);
                    document.body.removeChild(a);

                    showBackupMessage('✓ Keystore 백업이 다운로드되었습니다', 'success');
                } else {
                    showBackupMessage(data.error || '백업 실패', 'error');
                }
            } catch (error) {
                showBackupMessage('오류: ' + error.message, 'error');
            }
        }

        // Keystore 복원
        async function restoreKeystore() {
            const fileInput = document.getElementById('keystoreFile');
            const file = fileInput.files[0];

            if (!file) {
                return;
            }

            // 파일 형식 확인
            if (!file.name.endsWith('.jks')) {
                showBackupMessage('❌ .jks 파일만 업로드 가능합니다', 'error');
                fileInput.value = '';
                return;
            }

            const password = atob(adminToken).split(':')[0];

            try {
                // 파일을 Base64로 변환
                const reader = new FileReader();
                reader.onload = async (e) => {
                    const base64Data = e.target.result.split(',')[1]; // Data URI에서 Base64만 추출

                    // 서버로 전송
                    const response = await fetch('/webjwtgen/setup?action=restore&password=' + encodeURIComponent(password), {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ data: base64Data })
                    });

                    const result = await response.json();
                    
                    if (result.success) {
                        showBackupMessage(result.message, 'success');
                        setTimeout(() => {
                            location.reload();
                        }, 2000);
                    } else {
                        showBackupMessage(result.error || '복원 실패', 'error');
                    }
                };
                
                reader.onerror = () => {
                    showBackupMessage('파일 읽기 실패', 'error');
                };
                
                reader.readAsDataURL(file);
            } catch (error) {
                showBackupMessage('오류: ' + error.message, 'error');
            }

            // 파일 입력 리셋
            fileInput.value = '';
        }

        // 백업 관련 메시지 표시
        function showBackupMessage(message, type) {
            const msgEl = document.getElementById('backupMessage');
            msgEl.textContent = message;
            msgEl.className = 'message ' + type;
        }

        // 모달 외부 클릭 시 닫기
        document.getElementById('resetModal').addEventListener('click', function(e) {
            if (e.target === this) {
                closeResetModal();
            }
        });
    </script>
</body>
</html>
