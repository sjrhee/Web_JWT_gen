<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.nio.file.Files" %>
<%@ page import="java.nio.file.Paths" %>
<%
    // Keystore 파일 확인
    String webappPath = application.getRealPath("/");
    String keystorePath = webappPath + "keystore.jks";
    
    // Keystore가 없으면 setup.jsp로 리다이렉트
    if (!Files.exists(Paths.get(keystorePath))) {
        response.sendRedirect("setup.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>JWT Generator - ES256</title>
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
            max-width: 1000px;
            width: 100%;
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
            display: grid;
            grid-template-columns: 4fr 6fr;
            gap: 30px;
            padding: 30px;
        }

        @media (max-width: 768px) {
            .content {
                grid-template-columns: 1fr;
            }
        }

        .form-section h2 {
            color: #333;
            margin-bottom: 20px;
            font-size: 18px;
            border-bottom: 2px solid #667eea;
            padding-bottom: 10px;
        }

        .form-group {
            margin-bottom: 20px;
        }

        label {
            display: block;
            margin-bottom: 8px;
            color: #555;
            font-weight: 500;
            font-size: 14px;
        }

        input, textarea {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
            transition: border-color 0.3s;
        }

        input:focus, textarea:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }

        .help-text {
            font-size: 12px;
            color: #999;
            margin-top: 5px;
        }

        button {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 12px 30px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 14px;
            font-weight: 600;
            transition: transform 0.2s, box-shadow 0.2s;
            width: 100%;
        }

        button:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }

        button:active {
            transform: translateY(0);
        }

        .result-section {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 5px;
            border: 1px solid #e9ecef;
        }

        .result-section h2 {
            color: #333;
            margin-bottom: 20px;
            font-size: 18px;
            border-bottom: 2px solid #667eea;
            padding-bottom: 10px;
        }

        .result-item {
            margin-bottom: 20px;
        }

        .result-label {
            color: #666;
            font-weight: 600;
            margin-bottom: 8px;
            font-size: 12px;
            text-transform: uppercase;
        }

        .result-value {
            background: white;
            padding: 12px;
            border-radius: 5px;
            border: 1px solid #ddd;
            font-family: 'Courier New', monospace;
            font-size: 12px;
            word-break: break-all;
            line-height: 1.5;
            max-height: 200px;
            overflow-y: auto;
            color: #333;
            white-space: pre-wrap;
        }

        .copy-btn {
            background: #28a745;
            color: white;
            padding: 8px 15px;
            border: none;
            border-radius: 3px;
            cursor: pointer;
            font-size: 12px;
            margin-top: 8px;
            transition: background 0.3s;
        }

        .copy-btn:hover {
            background: #218838;
        }

        .success {
            color: #28a745;
            font-size: 14px;
            margin-top: 8px;
        }

        .error {
            color: #dc3545;
            font-size: 14px;
            margin-top: 8px;
            padding: 10px;
            background: #f8d7da;
            border: 1px solid #f5c6cb;
            border-radius: 3px;
        }

        .loading {
            display: none;
            text-align: center;
            color: #667eea;
            font-size: 14px;
        }

        .spinner {
            display: inline-block;
            width: 16px;
            height: 16px;
            border: 2px solid #f3f3f3;
            border-top: 2px solid #667eea;
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin-right: 8px;
        }

        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }

        .hidden {
            display: none;
        }

        .jwt-parts {
            background: white;
            padding: 12px;
            border-radius: 3px;
            border: 1px solid #ddd;
            font-size: 11px;
            line-height: 1.6;
            word-break: break-all;
        }

        .jwt-part {
            margin-bottom: 8px;
            padding: 8px;
            background: #f0f0f0;
            border-radius: 3px;
        }

        .jwt-label {
            font-weight: bold;
            color: #667eea;
            font-size: 10px;
            text-transform: uppercase;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <h1>🔐 JWT Generator</h1>
                    <p>ES256 (ECDSA with SHA-256) 기반 JWT 토큰 생성기</p>
                </div>
                <a href="admin.jsp" style="background: rgba(255,255,255,0.2); color: white; padding: 10px 20px; border-radius: 5px; text-decoration: none; font-weight: 600; font-size: 14px; border: 2px solid white; transition: all 0.3s; display: inline-block;">⚙️ 관리자</a>
            </div>
        </div>

        <div class="content">
            <!-- 입력 폼 -->
            <div class="form-section">
                <h2>JWT 생성</h2>

                <div class="form-group">
                    <label for="password">Keystore 비밀번호 *</label>
                    <input type="password" id="password" name="password" placeholder="Keystore 비밀번호" required>
                    <div class="help-text">현재 Keystore에 설정된 비밀번호</div>
                </div>

                <div class="form-group">
                    <label for="expYear">만료 일시 (Expiration) *</label>
                    <div style="display: grid; grid-template-columns: 1.2fr 0.9fr 0.9fr 1fr; gap: 8px;">
                        <div>
                            <input type="number" id="expYear" name="expYear" placeholder="YYYY" min="2025" max="2099" required>
                            <div class="help-text">연도</div>
                        </div>
                        <div>
                            <input type="number" id="expMonth" name="expMonth" placeholder="MM" min="1" max="12" required>
                            <div class="help-text">월</div>
                        </div>
                        <div>
                            <input type="number" id="expDay" name="expDay" placeholder="DD" min="1" max="31" required>
                            <div class="help-text">일</div>
                        </div>
                        <div>
                            <input type="time" id="expTime" name="expTime" required>
                            <div class="help-text">시간:분</div>
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <label for="iss">발급자 (Issuer) *</label>
                    <input type="text" id="iss" name="iss" placeholder="jwt-issuer" value="jwt-issuer" required>
                    <div class="help-text">토큰 발급 기관</div>
                </div>

                <div class="form-group">
                    <label for="sub">주제 (Subject) *</label>
                    <input type="text" id="sub" name="sub" placeholder="user-123" value="user-123" required>
                    <div class="help-text">토큰의 사용 주체 (사용자 ID 등)</div>
                </div>

                <button onclick="generateJWT()">JWT 생성</button>
                
                <div class="loading" id="loading">
                    <div class="spinner"></div>
                    JWT를 생성 중입니다...
                </div>
                <div class="error hidden" id="error"></div>
                <div class="success hidden" id="success">✓ JWT가 생성되었습니다!</div>
            </div>

            <!-- 결과 표시 -->
            <div class="result-section">
                <h2>결과</h2>

                <div class="result-item hidden" id="jwtPartsResult">
                    <div class="result-label">JWT 구조</div>
                    <div class="jwt-parts">
                        <div class="jwt-part">
                            <div class="jwt-label">Header</div>
                            <div id="jwtHeader"></div>
                        </div>
                        <div class="jwt-part">
                            <div class="jwt-label">Payload</div>
                            <div id="jwtPayload"></div>
                        </div>
                        <div class="jwt-part">
                            <div class="jwt-label">Signature</div>
                            <div id="jwtSignature"></div>
                        </div>
                    </div>
                </div>

                <div class="result-item hidden" id="publicKeyResult">
                    <div class="result-label">공개키 (Public Key)</div>
                    <div class="result-value" id="publicKey"></div>
                    <button class="copy-btn" onclick="copyToClipboard('publicKey')">복사</button>
                </div>

                <div class="result-item hidden" id="jwtResult">
                    <div class="result-label">JWT 토큰</div>
                    <div class="result-value" id="jwtToken"></div>
                    <button class="copy-btn" onclick="copyToClipboard('jwtToken')">복사</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        // 현재 날짜/시간을 기본값으로 설정 (10년 뒤)
        document.addEventListener('DOMContentLoaded', function() {
            const now = new Date();
            const future = new Date(now.getTime() + 10 * 365.25 * 24 * 3600 * 1000); // 10년 뒤

            const year = future.getFullYear();
            const month = String(future.getMonth() + 1).padStart(2, '0');
            const day = String(future.getDate()).padStart(2, '0');
            const hours = String(future.getHours()).padStart(2, '0');
            const minutes = String(future.getMinutes()).padStart(2, '0');

            document.getElementById('expYear').value = year;
            document.getElementById('expMonth').value = month;
            document.getElementById('expDay').value = day;
            document.getElementById('expTime').value = `${hours}:${minutes}`;
        });

        function generateJWT() {
            const password = document.getElementById('password').value;
            const year = document.getElementById('expYear').value;
            const month = document.getElementById('expMonth').value;
            const day = document.getElementById('expDay').value;
            const time = document.getElementById('expTime').value;
            const iss = document.getElementById('iss').value;
            const sub = document.getElementById('sub').value;

            if (!password || !year || !month || !day || !time || !iss || !sub) {
                showError('모든 필드를 입력해주세요.');
                return;
            }

            // 시간 형식 파싱 (HH:mm)
            const [hours, minutes] = time.split(':');
            
            // Unix timestamp 계산
            const expDate = new Date(year, month - 1, day, hours, minutes, 0, 0);
            const exp = Math.floor(expDate.getTime() / 1000);

            showLoading(true);
            hideMessages();

            fetch(`/webjwtgen/generate?exp=${exp}&iss=${encodeURIComponent(iss)}&sub=${encodeURIComponent(sub)}&password=${encodeURIComponent(password)}`)
                .then(response => {
                    // 응답 텍스트를 먼저 읽음
                    return response.text().then(text => {
                        try {
                            return JSON.parse(text);
                        } catch (e) {
                            console.error('JSON 파싱 실패:', text);
                            throw new Error('서버가 유효하지 않은 응답을 반환했습니다: ' + text.substring(0, 100));
                        }
                    });
                })
                .then(data => {
                    showLoading(false);
                    if (data.success) {
                        displayResult(data.jwt, data.publicKey);
                        showSuccess();
                    } else {
                        // Keystore 없음 에러 처리
                        if (data.needsSetup) {
                            showError(data.message);
                            // 초기화 또는 복원 선택 알림
                            setTimeout(() => {
                                const choice = confirm('Keystore가 없습니다.\n\n[확인] 초기 설정으로 이동\n[취소] Keystore 복원하기');
                                if (choice) {
                                    window.location.href = 'setup.jsp';
                                } else {
                                    window.location.href = 'admin.jsp';
                                }
                            }, 500);
                        } else {
                            showError(data.error || 'JWT 생성 실패');
                        }
                    }
                })
                .catch(error => {
                    showLoading(false);
                    console.error('에러:', error);
                    showError('요청 실패: ' + error.message);
                });
        }

        function displayResult(jwt, publicKey) {
            const parts = jwt.split('.');
            
            // JWT 토큰 표시
            document.getElementById('jwtToken').textContent = jwt;
            document.getElementById('jwtResult').classList.remove('hidden');

            // JWT 구조 표시
            document.getElementById('jwtHeader').textContent = atob(parts[0]);
            document.getElementById('jwtPayload').textContent = atob(parts[1]);
            document.getElementById('jwtSignature').textContent = parts[2];
            document.getElementById('jwtPartsResult').classList.remove('hidden');

            // 공개키 표시 (개행 포함)
            const publicKeyFormatted = publicKey.replace(/\\n/g, '\n');
            document.getElementById('publicKey').textContent = publicKeyFormatted;
            document.getElementById('publicKeyResult').classList.remove('hidden');
        }

        function copyToClipboard(elementId) {
            const element = document.getElementById(elementId);
            const text = element.textContent;
            
            // 최신 Clipboard API 사용
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text)
                    .then(function() {
                        // 성공 - 메시지 없음
                    })
                    .catch(function(err) {
                        console.error('복사 실패:', err);
                        // 폴백: 이전 방식
                        copyToClipboardFallback(text);
                    });
            } else {
                // 폴백: 이전 방식
                copyToClipboardFallback(text);
            }
        }

        function copyToClipboardFallback(text) {
            const textarea = document.createElement('textarea');
            textarea.value = text;
            textarea.style.position = 'fixed';
            textarea.style.opacity = '0';
            document.body.appendChild(textarea);
            textarea.select();
            try {
                document.execCommand('copy');
                // 성공 - 메시지 없음
            } catch (err) {
                console.error('복사 실패:', err);
            }
            document.body.removeChild(textarea);
        }

        function showLoading(show) {
            document.getElementById('loading').style.display = show ? 'block' : 'none';
        }

        function showError(message) {
            const errorEl = document.getElementById('error');
            errorEl.textContent = message;
            errorEl.classList.remove('hidden');
        }

        function showSuccess() {
            document.getElementById('success').classList.remove('hidden');
            setTimeout(() => {
                document.getElementById('success').classList.add('hidden');
            }, 3000);
        }

        function hideMessages() {
            document.getElementById('error').classList.add('hidden');
            document.getElementById('success').classList.add('hidden');
        }

        // 페이지 로드 시 초기화 상태 확인 (첫 로드 시에만)
        // 이제 JWT 생성 시에 Keystore 상태를 확인하므로 여기서는 제거
        window.addEventListener('load', async () => {
            // 빈 상태로 유지
        });
    </script>
</body>
</html>
