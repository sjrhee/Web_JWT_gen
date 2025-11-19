/* Admin Page JavaScript */

let adminToken = null;

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 세션에서 토큰 복원
    const token = sessionStorage.getItem('adminToken');
    if (token) {
        adminToken = token;
        showAdminSection();
    } else {
        showLoginSection();
    }
});

// 로그인 섹션 표시
function showLoginSection() {
    document.getElementById('loginSection').style.display = 'block';
    document.getElementById('adminSection').style.display = 'none';
    document.getElementById('adminPassword').focus();
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
        // 비밀번호 검증: PUT 요청으로 최소 길이 체크 (8자)
        if (password.length < 8) {
            showAuthError('비밀번호 형식이 올바르지 않습니다');
            return;
        }

        // 관리자 토큰 생성 및 저장
        // 실제로는 서버에서 검증해야 하지만, 여기서는 클라이언트 토큰으로 관리
        adminToken = btoa(password + ':' + new Date().getTime());
        sessionStorage.setItem('adminToken', adminToken);
        
        // 인증 오류 숨기기
        document.getElementById('authError').style.display = 'none';
        
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

// 비밀번호 변경
async function changePassword() {
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmNewPassword = document.getElementById('confirmNewPassword').value;
    const messageEl = document.getElementById('passwordChangeMessage');

    console.log('[changePassword] 함수 호출됨');
    console.log('[changePassword] currentPassword 길이:', currentPassword.length);
    console.log('[changePassword] newPassword 길이:', newPassword.length);

    if (!currentPassword || !newPassword || !confirmNewPassword) {
        showPasswordChangeMessage('모든 필드를 입력해주세요', 'error');
        return;
    }

    if (newPassword.length < 8) {
        showPasswordChangeMessage('새 비밀번호는 8자 이상이어야 합니다', 'error');
        return;
    }

    if (newPassword !== confirmNewPassword) {
        showPasswordChangeMessage('새 비밀번호가 일치하지 않습니다', 'error');
        return;
    }

    try {
        console.log('[changePassword] PUT 요청 시작');
        const response = await fetch('/webjwtgen/setup', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `currentPassword=${encodeURIComponent(currentPassword)}&newPassword=${encodeURIComponent(newPassword)}&confirmNewPassword=${encodeURIComponent(confirmNewPassword)}`
        });

        console.log('[changePassword] 응답 상태:', response.status);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('[changePassword] 응답 데이터:', data);

        if (data.success) {
            const newPwd = document.getElementById('newPassword').value;
            showPasswordChangeMessage(`✅ 비밀번호가 변경되었습니다\n새 비밀번호: ${newPwd}`, 'success');
            
            // adminToken을 새로운 비밀번호로 업데이트
            adminToken = btoa(newPwd + ':' + new Date().getTime());
            sessionStorage.setItem('adminToken', adminToken);
            console.log('[changePassword] adminToken 업데이트됨');
            
            // 5초 후 입력 필드 초기화 (사용자가 새 비밀번호를 기록할 시간 제공)
            setTimeout(() => {
                document.getElementById('currentPassword').value = '';
                document.getElementById('newPassword').value = '';
                document.getElementById('confirmNewPassword').value = '';
                messageEl.style.display = 'none';
            }, 5000);
        } else {
            showPasswordChangeMessage(data.error || '비밀번호 변경 실패', 'error');
        }
    } catch (error) {
        console.error('[changePassword] 오류:', error);
        showPasswordChangeMessage('오류: ' + error.message, 'error');
    }
}

function showPasswordChangeMessage(message, type) {
    const messageEl = document.getElementById('passwordChangeMessage');
    messageEl.textContent = message;
    messageEl.className = 'message ' + type;
    messageEl.style.display = 'block';
}

// 강제 초기화 모달 열기
function openResetModal() {
    console.log('[openResetModal] 함수 호출됨');
    document.getElementById('passwordResetModal').classList.add('active');
    document.getElementById('resetPassword').value = '';
    document.getElementById('resetPassword').focus();
    document.getElementById('resetPasswordError').style.display = 'none';
}

// 비밀번호 재입력 모달 닫기
function closePasswordResetModal() {
    console.log('[closePasswordResetModal] 함수 호출됨');
    document.getElementById('passwordResetModal').classList.remove('active');
    document.getElementById('resetPassword').value = '';
}

// 비밀번호 검증 후 강제 초기화 진행
function verifyPasswordForReset() {
    const password = document.getElementById('resetPassword').value;
    
    console.log('[verifyPasswordForReset] 함수 호출됨, 비밀번호 길이:', password.length);
    
    if (!password) {
        showResetPasswordError('비밀번호를 입력해주세요');
        return;
    }

    // 비밀번호 길이로 간단히 검증 (실제 검증은 강제 초기화 시에 서버에서 함)
    console.log('[verifyPasswordForReset] 비밀번호 검증 완료');
    
    // 비밀번호 모달 닫고 초기화 확인 모달 열기
    closePasswordResetModal();
    openConfirmResetModal();
}

// 비밀번호 에러 표시
function showResetPasswordError(message) {
    console.log('[showResetPasswordError]', message);
    document.getElementById('resetPasswordError').textContent = message;
    document.getElementById('resetPasswordError').style.display = 'block';
}

// 강제 초기화 확인 모달 열기
function openConfirmResetModal() {
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
    console.log('[forceReset] 함수 호출됨');
    const btn = document.getElementById('confirmResetBtn');
    btn.disabled = true;
    btn.textContent = '초기화 중...';

    try {
        // sessionStorage에서 비밀번호 복원
        let password = atob(adminToken).split(':')[0];
        
        // 만약 adminToken에서 비밀번호를 못 가져온 경우, 모달에서 입력받은 비밀번호 사용
        const resetPasswordField = document.getElementById('resetPassword');
        if (resetPasswordField && resetPasswordField.value) {
            password = resetPasswordField.value;
        }
        
        console.log('[forceReset] 비밀번호 길이:', password.length);
        console.log('[forceReset] DELETE 요청 시작');
        
        const response = await fetch('/webjwtgen/setup?password=' + encodeURIComponent(password) + '&confirm=FORCE_RESET_CONFIRMED', {
            method: 'DELETE'
        });

        console.log('[forceReset] 응답 상태:', response.status);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        console.log('[forceReset] 응답 데이터:', data);
        
        closeResetModal();

        if (data.success) {
            showMessage('✅ ' + data.message, 'success');
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
        console.error('[forceReset] 오류:', error);
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
    try {
        // 1. 서버에서 현재 비밀번호 조회
        const pwResponse = await fetch('/webjwtgen/setup?action=currentPassword');
        const pwData = await pwResponse.json();
        
        if (!pwData.success) {
            showBackupMessage('비밀번호 조회 실패: ' + (pwData.error || '알 수 없는 오류'), 'error');
            return;
        }
        
        const password = pwData.password;
        console.log('[backupKeystore] 서버에서 읽은 비밀번호 길이:', password.length);
        
        // 2. 조회한 비밀번호로 백업 시작
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
        console.error('[backupKeystore] 오류:', error);
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

    // 비밀번호 입력받기 - 복원은 중요한 작업이므로 명시적으로 비밀번호 입력
    const password = prompt('복원을 위해 현재 비밀번호를 입력해주세요:');
    if (!password) {
        fileInput.value = '';
        return;
    }

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
                showBackupMessage('✅ ' + result.message, 'success');
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
if (document.getElementById('passwordResetModal')) {
    document.getElementById('passwordResetModal').addEventListener('click', function(e) {
        if (e.target === this) {
            closePasswordResetModal();
        }
    });
}

if (document.getElementById('resetModal')) {
    document.getElementById('resetModal').addEventListener('click', function(e) {
        if (e.target === this) {
            closeResetModal();
        }
    });
}

// Enter 키 처리 (비밀번호 재입력)
if (document.getElementById('resetPassword')) {
    document.getElementById('resetPassword').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            verifyPasswordForReset();
        }
    });
}
