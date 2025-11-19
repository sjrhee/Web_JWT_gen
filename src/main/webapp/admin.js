/* Admin Page JavaScript */

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    showAdminSection();
});

// 관리자 섹션 표시
function showAdminSection() {
    document.getElementById('adminSection').style.display = 'block';
}

// Keystore 백업 다운로드
async function backupKeystore() {
    console.log('[backupKeystore] 시작');
    try {
        // 1. 비밀번호 입력받기
        const password = prompt('Keystore 비밀번호를 입력하세요:');
        if (!password) {
            console.log('[backupKeystore] 비밀번호 입력 취소');
            return;
        }
        
        // 2. 백업 요청 (비밀번호 직접 전송)
        console.log('[backupKeystore] 백업 요청 시작');
        const response = await fetch('/webjwtgen/setup?action=backup', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: 'password=' + encodeURIComponent(password)
        });
        
        console.log('[backupKeystore] 응답 상태:', response.status);
        
        if (!response.ok) {
            console.log('[backupKeystore] HTTP 오류');
            const data = await response.json();
            console.log('[backupKeystore] 오류 메시지:', data.error);
            showBackupMessage('❌ ' + (data.error || '백업 실패'), 'error');
            return;
        }

        const data = await response.json();
        console.log('[backupKeystore] 응답 데이터 success:', data.success);
        
        if (data.success) {
            console.log('[backupKeystore] Base64 데이터 길이:', data.data.length);
            // Base64 데이터를 Blob으로 변환
            const binaryString = atob(data.data);
            const bytes = new Uint8Array(binaryString.length);
            for (let i = 0; i < binaryString.length; i++) {
                bytes[i] = binaryString.charCodeAt(i);
            }
            const blob = new Blob([bytes], { type: 'application/octet-stream' });
            
            console.log('[backupKeystore] Blob 생성 완료, 크기:', blob.size);
            
            // 다운로드
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'keystore.jks';
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            
            showBackupMessage('✅ Keystore 백업이 다운로드되었습니다', 'success');
        } else {
            console.log('[backupKeystore] 서버 오류:', data.error);
            showBackupMessage('❌ ' + (data.error || '백업 실패'), 'error');
        }
    } catch (error) {
        console.error('[backupKeystore] 오류:', error);
        console.error('[backupKeystore] 오류 메시지:', error.message);
        showBackupMessage('❌ 오류: ' + error.message, 'error');
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

    // 1. 비밀번호 입력받기
    const password = prompt('Keystore 비밀번호를 입력하세요:');
    if (!password) {
        console.log('[restoreKeystore] 비밀번호 입력 취소');
        fileInput.value = '';
        return;
    }
    
    console.log('[restoreKeystore] 파일을 Base64로 변환 중...');

    // 2. 파일을 Base64로 변환 후 비밀번호와 함께 전송
    const reader = new FileReader();
    reader.onload = async (e) => {
        try {
            const base64Data = e.target.result.split(',')[1]; // Data URI에서 Base64만 추출

            console.log('[restoreKeystore] 서버로 복원 요청 전송');

            // 서버로 전송 (비밀번호 포함)
            const response = await fetch('/webjwtgen/setup?action=restore', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ 
                    data: base64Data,
                    password: password  // 복원할 Keystore의 비밀번호
                })
            });

            const result = await response.json();
            
            if (result.success) {
                showBackupMessage('✅ ' + result.message, 'success');
                setTimeout(() => {
                    location.reload();
                }, 2000);
            } else {
                showBackupMessage('❌ ' + (result.error || '복원 실패'), 'error');
            }
        } catch (error) {
            console.error('[restoreKeystore] 오류:', error);
            showBackupMessage('❌ 오류: ' + error.message, 'error');
        }
    };
    
    reader.onerror = () => {
        console.error('[restoreKeystore] 파일 읽기 실패');
        showBackupMessage('❌ 파일 읽기 실패', 'error');
    };
    
    reader.readAsDataURL(file);

    // 파일 입력 리셋
    fileInput.value = '';
}

// 백업 관련 메시지 표시
function showBackupMessage(message, type) {
    const msgEl = document.getElementById('backupMessage');
    msgEl.textContent = message;
    msgEl.className = 'message ' + type;
    msgEl.style.display = 'block';
}

// 돌아가기
function goBack() {
    window.location.href = 'index.jsp';
}

// 비밀번호 변경
async function changePassword() {
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const msgEl = document.getElementById('passwordChangeMessage');
    
    if (!currentPassword || !newPassword || !confirmPassword) {
        msgEl.textContent = '❌ 모든 필드를 입력해주세요';
        msgEl.className = 'message error';
        return;
    }
    
    if (newPassword !== confirmPassword) {
        msgEl.textContent = '❌ 새 비밀번호가 일치하지 않습니다';
        msgEl.className = 'message error';
        return;
    }
    
    if (newPassword === currentPassword) {
        msgEl.textContent = '❌ 새 비밀번호는 현재 비밀번호와 달라야 합니다';
        msgEl.className = 'message error';
        return;
    }
    
    try {
        msgEl.textContent = '⏳ 비밀번호 변경 중...';
        msgEl.className = 'message info';
        
        const response = await fetch('/webjwtgen/setup', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: 'action=changePassword&currentPassword=' + encodeURIComponent(currentPassword) +
                  '&newPassword=' + encodeURIComponent(newPassword) +
                  '&confirmPassword=' + encodeURIComponent(confirmPassword)
        });
        
        const data = await response.json();
        
        if (data.success) {
            msgEl.textContent = '✅ ' + data.message;
            msgEl.className = 'message success';
            
            // 입력 필드 초기화
            document.getElementById('currentPassword').value = '';
            document.getElementById('newPassword').value = '';
            document.getElementById('confirmPassword').value = '';
            
            setTimeout(() => {
                msgEl.textContent = '';
            }, 3000);
        } else {
            msgEl.textContent = '❌ ' + (data.error || '비밀번호 변경 실패');
            msgEl.className = 'message error';
        }
    } catch (error) {
        msgEl.textContent = '❌ 오류: ' + error.message;
        msgEl.className = 'message error';
    }
}

// 시스템 초기화
async function forceReset() {
    const adminPassword = document.getElementById('resetAdminPassword').value;
    const newPassword = document.getElementById('resetNewPassword').value;
    const confirmPassword = document.getElementById('resetConfirmPassword').value;
    const msgEl = document.getElementById('resetMessage');
    
    if (!adminPassword || !newPassword || !confirmPassword) {
        msgEl.textContent = '❌ 모든 필드를 입력해주세요';
        msgEl.className = 'message error';
        return;
    }
    
    if (newPassword !== confirmPassword) {
        msgEl.textContent = '❌ 새 비밀번호가 일치하지 않습니다';
        msgEl.className = 'message error';
        return;
    }
    
    // 최종 확인
    if (!confirm('⚠️ 정말로 시스템을 초기화하시겠습니까?\n기존의 모든 키 정보가 삭제됩니다.')) {
        return;
    }
    
    try {
        msgEl.textContent = '⏳ 시스템 초기화 중...';
        msgEl.className = 'message info';
        
        const response = await fetch('/webjwtgen/setup', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: 'action=forceReset&adminPassword=' + encodeURIComponent(adminPassword) +
                  '&password=' + encodeURIComponent(newPassword) +
                  '&confirmPassword=' + encodeURIComponent(confirmPassword)
        });
        
        const data = await response.json();
        
        if (data.success) {
            msgEl.textContent = '✅ ' + data.message;
            msgEl.className = 'message success';
            
            // 입력 필드 초기화
            document.getElementById('resetAdminPassword').value = '';
            document.getElementById('resetNewPassword').value = '';
            document.getElementById('resetConfirmPassword').value = '';
            
            setTimeout(() => {
                location.reload();
            }, 2000);
        } else {
            msgEl.textContent = '❌ ' + (data.error || '초기화 실패');
            msgEl.className = 'message error';
        }
    } catch (error) {
        msgEl.textContent = '❌ 오류: ' + error.message;
        msgEl.className = 'message error';
    }
}
