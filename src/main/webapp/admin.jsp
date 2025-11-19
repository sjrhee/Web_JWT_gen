<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>JWT 생성기 - 관리자 페이지</title>
    <link rel="stylesheet" href="admin.css">
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔧 관리자 페이지</h1>
            <p>JWT 생성기 시스템 관리</p>
        </div>

        <div class="content">
            <!-- 관리자 기능 화면 -->
            <div id="adminSection">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <a href="index.jsp" class="back-link">← JWT 생성기로 돌아가기</a>
                </div>

                <div class="message" id="message"></div>

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

                    <div class="button-group">
                        <button class="btn-backup" onclick="backupKeystore()">📥 Keystore 다운로드</button>
                        <button class="btn-restore" onclick="document.getElementById('keystoreFile').click()">📤 Keystore 복원</button>
                    </div>
                    <input type="file" id="keystoreFile" style="display: none;" accept=".jks" onchange="restoreKeystore()">
                    
                    <div id="backupMessage" class="message" style="margin-top: 20px;"></div>
                </div>

                <!-- Keystore 비밀번호 변경 섹션 -->
                <div class="section">
                    <h2>🔐 비밀번호 변경</h2>
                    
                    <div class="warning-box">
                        <div class="warning-title">
                            <span class="warning-icon">⚠️</span>
                            Keystore 비밀번호
                        </div>
                        <div class="warning-text">
                            Keystore 비밀번호를 변경합니다. 현재 비밀번호가 필요합니다.
                        </div>
                    </div>

                    <div style="display: flex; gap: 10px;">
                        <input type="password" id="currentPassword" placeholder="현재 비밀번호" style="flex: 1; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
                        <input type="password" id="newPassword" placeholder="새 비밀번호" style="flex: 1; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
                        <input type="password" id="confirmPassword" placeholder="비밀번호 확인" style="flex: 1; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
                        <button class="btn-backup" onclick="changePassword()" style="padding: 8px 16px;">🔑 변경</button>
                    </div>
                    
                    <div id="passwordChangeMessage" class="message" style="margin-top: 10px;"></div>
                </div>

                <!-- 시스템 초기화 섹션 -->
                <div class="section">
                    <h2>⚙️ 시스템 초기화</h2>
                    
                    <div class="warning-box" style="border-left: 4px solid #ff6b6b;">
                        <div class="warning-title" style="color: #ff6b6b;">
                            <span class="warning-icon">🔴</span>
                            주의
                        </div>
                        <div class="warning-text">
                            시스템을 초기화하면 기존 키 정보가 삭제됩니다.
                            관리자 비밀번호로 인증 후 새로운 비밀번호로 초기화됩니다.
                        </div>
                    </div>

                    <div style="display: flex; gap: 10px;">
                        <input type="password" id="resetAdminPassword" placeholder="관리자 비밀번호 (현재)" style="flex: 1; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
                        <input type="password" id="resetNewPassword" placeholder="새 비밀번호" style="flex: 1; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
                        <input type="password" id="resetConfirmPassword" placeholder="비밀번호 확인" style="flex: 1; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
                        <button class="btn-backup" onclick="forceReset()" style="padding: 8px 16px; background-color: #ff6b6b;">🔄 초기화</button>
                    </div>
                    
                    <div id="resetMessage" class="message" style="margin-top: 10px;"></div>
                </div>
            </div>
        </div>
    </div>

    <script src="admin.js"></script>
</body>
</html>
