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

                <!-- Keystore 비밀번호 기능 설명 섹션 -->
                <div class="section">
                    <h2>ℹ️ Keystore 비밀번호 기능</h2>
                    
                    <div class="info-box">
                        <div class="info-title">
                            <span class="info-icon">📌</span>
                            Keystore 비밀번호의 4가지 기능
                        </div>
                        <div class="info-list">
                            <div class="info-item">
                                <div class="item-number">1️⃣</div>
                                <div class="item-content">
                                    <strong>Keystore 접근 보호</strong>
                                    <p>Keystore 파일에 저장된 개인키 접근을 보호합니다. JWT 생성 시 이 비밀번호로 Keystore에 접근합니다.</p>
                                </div>
                            </div>
                            <div class="info-item">
                                <div class="item-number">2️⃣</div>
                                <div class="item-content">
                                    <strong>Keystore 백업 검증</strong>
                                    <p>Keystore를 백업할 때 현재 비밀번호를 확인하여 권한 있는 사용자만 백업을 받을 수 있도록 보호합니다.</p>
                                </div>
                            </div>
                            <div class="info-item">
                                <div class="item-number">3️⃣</div>
                                <div class="item-content">
                                    <strong>Keystore 복원</strong>
                                    <p>백업 시 사용한 비밀번호로 검증하여 백업 파일을 복원합니다. 이 비밀번호가 계속 사용됩니다.</p>
                                </div>
                            </div>
                            <div class="info-item">
                                <div class="item-number">4️⃣</div>
                                <div class="item-content">
                                    <strong>관리 기능 인증</strong>
                                    <p>Keystore의 비밀번호는 JWT 생성과 모든 관리 기능을 수행할 때 사용되며 보안을 보장합니다.</p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="warning-box">
                        <div class="warning-title">
                            <span class="warning-icon">💡</span>
                            중요 안내
                        </div>
                        <div class="warning-text">
                            Keystore 비밀번호는 시스템의 모든 보안 기능을 제어합니다. 
                            강력한 비밀번호를 설정하고 안전한 곳에 보관하세요.
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="admin.js"></script>
</body>
</html>
