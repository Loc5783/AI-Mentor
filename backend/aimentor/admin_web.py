# Embedded Web Admin Single Page App for AI Mentor Content Management

ADMIN_HTML = """<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Mentor - Quản trị Kho nội dung</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background-color: #f8fafc; font-family: system-ui, -apple-system, sans-serif; }
        .sidebar { background: #1e293b; color: white; min-height: 100vh; padding-top: 20px; }
        .sidebar .nav-link { color: #94a3b8; padding: 12px 20px; border-radius: 8px; margin: 4px 12px; }
        .sidebar .nav-link.active, .sidebar .nav-link:hover { color: white; background: #334155; }
        .card { border-radius: 12px; border: 1px solid #e2e8f0; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
        .badge-draft { background-color: #f59e0b; color: white; }
        .badge-published { background-color: #10b981; color: white; }
        .preview-box { background: #f1f5f9; border-left: 4px solid #3b82f6; padding: 16px; border-radius: 8px; }
    </style>
</head>
<body>
    <div class="container-fluid" id="app-container">
        <div class="row">
            <!-- Sidebar -->
            <div class="col-md-3 col-lg-2 sidebar">
                <div class="px-3 mb-4">
                    <h5 class="fw-bold text-primary">🎓 AI Mentor Admin</h5>
                    <small class="text-muted">Quản trị Kho nội dung</small>
                </div>
                <nav class="nav flex-column">
                    <a class="nav-link active" href="#" onclick="showSection('dashboard')">📊 Tổng quan</a>
                    <a class="nav-link" href="#" onclick="showSection('import')">📥 Import Excel</a>
                    <a class="nav-link" href="#" onclick="showSection('questions')">📝 Quản lý câu hỏi</a>
                </nav>
            </div>

            <!-- Main Content -->
            <div class="col-md-9 col-lg-10 p-4">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h3 class="fw-bold" id="page-title">Tổng quan Kho câu hỏi</h3>
                        <p class="text-muted mb-0">Quản lý, kiểm tra và xuất bản nội dung trắc nghiệm</p>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <span id="user-status" class="badge bg-secondary p-2">Chưa đăng nhập</span>
                        <button id="btn-login" class="btn btn-outline-primary btn-sm" onclick="openLoginModal()">🔑 Đăng nhập</button>
                        <button id="btn-logout" class="btn btn-outline-danger btn-sm" style="display:none;" onclick="handleLogout()">Đăng xuất</button>
                    </div>
                </div>

                <!-- Banner alert for non-admin or error -->
                <div id="auth-alert" class="alert alert-warning" style="display:none;"></div>

                <!-- Dashboard Section -->
                <div id="section-dashboard" class="content-section">
                    <div class="row g-3 mb-4">
                        <div class="col-md-4">
                            <div class="card p-3">
                                <h6 class="text-muted">Tổng số câu hỏi</h6>
                                <h2 class="fw-bold text-primary" id="stat-total">0</h2>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="card p-3">
                                <h6 class="text-muted">Đã xuất bản</h6>
                                <h2 class="fw-bold text-success" id="stat-published">0</h2>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="card p-3">
                                <h6 class="text-muted">Bản nháp</h6>
                                <h2 class="fw-bold text-warning" id="stat-draft">0</h2>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Import Section -->
                <div id="section-import" class="content-section" style="display:none;">
                    <div class="card p-4 mb-4">
                        <h5 class="fw-bold mb-3">Tải mẫu Excel & Tải lên file câu hỏi</h5>
                        <p class="text-muted">Định dạng file hỗ trợ: CSV, XLSX. Mỗi dòng đại diện cho 1 câu hỏi trắc nghiệm A-D.</p>
                        <div class="mb-3">
                            <a href="/admin/template/excel" class="btn btn-outline-success" download="Mau_Cau_Hoi_AI_Mentor.csv">
                                📄 Tải mẫu file `.csv` / `.xlsx` mẫu
                            </a>
                        </div>
                        <hr>
                        <form id="import-form" onsubmit="handleImportPreview(event)">
                            <div class="mb-3">
                                <label class="form-label fw-bold">Chọn file dữ liệu câu hỏi:</label>
                                <input type="file" id="import-file" class="form-control" accept=".csv,.xlsx,.txt" required>
                            </div>
                            <button type="submit" id="btn-import-preview" class="btn btn-primary">🔍 Tải lên & Kiểm tra lỗi (Preview)</button>
                        </form>
                    </div>

                    <div id="import-results" style="display:none;">
                        <div class="card p-4 mb-4">
                            <h5 class="fw-bold mb-3">Báo cáo kiểm tra dữ liệu Import</h5>
                            <div id="import-summary" class="alert alert-info"></div>

                            <div id="import-errors" class="alert alert-danger" style="display:none;">
                                <h6>⚠️ Danh sách lỗi cần sửa trong file:</h6>
                                <ul id="error-list" class="mb-0"></ul>
                            </div>

                            <div id="import-preview-list" class="mb-3">
                                <h6>✅ Danh sách câu hỏi hợp lệ sẵn sàng import:</h6>
                                <div class="table-responsive">
                                    <table class="table table-bordered table-hover">
                                        <thead class="table-light">
                                            <tr>
                                                <th>#</th>
                                                <th>Mã</th>
                                                <th>Môn</th>
                                                <th>Lớp</th>
                                                <th>Câu hỏi</th>
                                                <th>Đáp án đúng</th>
                                                <th>Độ khó</th>
                                            </tr>
                                        </thead>
                                        <tbody id="preview-table-body"></tbody>
                                    </table>
                                </div>
                            </div>

                            <div class="d-flex gap-2">
                                <button id="btn-import-draft" class="btn btn-warning" onclick="confirmImport(false)">💾 Lưu dưới dạng BẢN NHÁP (Draft)</button>
                                <button id="btn-import-publish" class="btn btn-success" onclick="confirmImport(true)">🚀 XUẤT BẢN NGAY (Publish)</button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Questions List Section -->
                <div id="section-questions" class="content-section" style="display:none;">
                    <div class="card p-3 mb-3">
                        <div class="row g-2">
                            <div class="col-md-3">
                                <select id="filter-subject" class="form-select" onchange="loadQuestions()">
                                    <option value="">Tất cả môn</option>
                                    <option value="physics" selected>Vật lý</option>
                                    <option value="chemistry">Hóa học</option>
                                    <option value="biology">Sinh học</option>
                                    <option value="math">Toán</option>
                                    <option value="literature">Ngữ văn</option>
                                    <option value="english">Tiếng Anh</option>
                                    <option value="history">Lịch sử</option>
                                    <option value="geography">Địa lý</option>
                                    <option value="economics_law">GDKT & PL</option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <select id="filter-grade" class="form-select" onchange="loadQuestions()">
                                    <option value="">Tất cả lớp</option>
                                    <option value="10" selected>Lớp 10</option>
                                    <option value="11">Lớp 11</option>
                                    <option value="12">Lớp 12</option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <select id="filter-status" class="form-select" onchange="loadQuestions()">
                                    <option value="">Tất cả trạng thái</option>
                                    <option value="PUBLISHED">Đã xuất bản</option>
                                    <option value="DRAFT">Bản nháp</option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <button class="btn btn-primary w-100" onclick="loadQuestions()">🔄 Tải lại</button>
                            </div>
                        </div>
                    </div>

                    <div id="questions-list"></div>
                </div>
            </div>
        </div>
    </div>

    <!-- Login Modal -->
    <div class="modal fade" id="loginModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold">Đăng nhập Tài khoản Quản trị</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div id="login-error" class="alert alert-danger" style="display:none;"></div>
                    <form onsubmit="handleLoginSubmit(event)">
                        <div class="mb-3">
                            <label class="form-label fw-bold">Tên đăng nhập / Email:</label>
                            <input type="text" id="login-identifier" class="form-control" required placeholder="admin@example.com">
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-bold">Mật khẩu:</label>
                            <input type="password" id="login-password" class="form-control" required>
                        </div>
                        <button type="submit" class="btn btn-primary w-100">Đăng nhập Web Admin</button>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        let token = localStorage.getItem('admin_token') || '';
        let refreshToken = localStorage.getItem('admin_refresh') || '';
        let lastPreviewData = null;
        let loginModalObj = null;
        let isImporting = false;
        let isPublishing = false;

        document.addEventListener('DOMContentLoaded', () => {
            loginModalObj = new bootstrap.Modal(document.getElementById('loginModal'));
            if (token) {
                checkAuth();
            } else {
                showAuthAlert('Vui lòng đăng nhập tài khoản Quản trị viên / Biên soạn để tiếp tục.');
            }
        });

        function escapeHtml(str) {
            if (!str) return '';
            return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
        }

        function showAuthAlert(msg) {
            let el = document.getElementById('auth-alert');
            el.innerText = msg;
            el.style.display = 'block';
        }

        function hideAuthAlert() {
            document.getElementById('auth-alert').style.display = 'none';
        }

        function openLoginModal() {
            document.getElementById('login-error').style.display = 'none';
            loginModalObj.show();
        }

        async function handleLoginSubmit(e) {
            e.preventDefault();
            let identifierEl = document.getElementById('login-identifier');
            let passwordEl = document.getElementById('login-password');
            let identifier = identifierEl.value.trim();
            let password = passwordEl.value;
            passwordEl.value = ''; // Clear password immediately after reading

            try {
                let res = await fetch('/auth/sign-in', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ identifier, password })
                });

                if (!res.ok) {
                    let err = await res.json();
                    document.getElementById('login-error').innerText = 'Đăng nhập thất bại: Tên đăng nhập hoặc mật khẩu không đúng.';
                    document.getElementById('login-error').style.display = 'block';
                    return;
                }

                let data = await res.json();
                token = data.accessToken;
                refreshToken = data.refreshToken;
                localStorage.setItem('admin_token', token);
                localStorage.setItem('admin_refresh', refreshToken);
                loginModalObj.hide();
                await checkAuth();
            } catch (err) {
                document.getElementById('login-error').innerText = 'Lỗi kết nối tới server: ' + err;
                document.getElementById('login-error').style.display = 'block';
            }
        }

        async function checkAuth() {
            try {
                let res = await fetch('/me/account', {
                    headers: { 'Authorization': 'Bearer ' + token }
                });

                if (res.status === 401) {
                    handleLogout();
                    showAuthAlert('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
                    return;
                }

                let acc = await res.json();
                let userRes = await fetch('/admin/questions', {
                    headers: { 'Authorization': 'Bearer ' + token }
                });

                if (userRes.status === 403) {
                    document.getElementById('user-status').innerText = 'Tài khoản: ' + acc.username + ' (Không có quyền Admin)';
                    document.getElementById('user-status').className = 'badge bg-danger p-2';
                    document.getElementById('btn-login').style.display = 'none';
                    document.getElementById('btn-logout').style.display = 'inline-block';
                    showAuthAlert('Tài khoản ' + acc.username + ' là STUDENT và không có quyền Quản trị / Biên soạn nội dung.');
                    return;
                }

                document.getElementById('user-status').innerText = 'Admin: ' + acc.username;
                document.getElementById('user-status').className = 'badge bg-success p-2';
                document.getElementById('btn-login').style.display = 'none';
                document.getElementById('btn-logout').style.display = 'inline-block';
                hideAuthAlert();
                loadStats();
            } catch (e) {
                showAuthAlert('Không thể kết nối đến máy chủ.');
            }
        }

        async function handleLogout() {
            if (token && refreshToken) {
                try {
                    await fetch('/auth/sign-out', {
                        method: 'POST',
                        headers: {
                            'Authorization': 'Bearer ' + token,
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ refreshToken })
                    });
                } catch(e){}
            }
            token = '';
            refreshToken = '';
            lastPreviewData = null;
            localStorage.removeItem('admin_token');
            localStorage.removeItem('admin_refresh');
            document.getElementById('user-status').innerText = 'Chưa đăng nhập';
            document.getElementById('user-status').className = 'badge bg-secondary p-2';
            document.getElementById('btn-login').style.display = 'inline-block';
            document.getElementById('btn-logout').style.display = 'none';
            document.getElementById('import-results').style.display = 'none';
            document.getElementById('questions-list').innerHTML = '';
            showAuthAlert('Bạn đã đăng xuất.');
        }

        function showSection(name) {
            document.querySelectorAll('.content-section').forEach(el => el.style.display = 'none');
            document.querySelectorAll('.sidebar .nav-link').forEach(el => el.classList.remove('active'));
            document.getElementById('section-' + name).style.display = 'block';
            if (name === 'dashboard') loadStats();
            if (name === 'questions') loadQuestions();
        }

        async function loadStats() {
            if (!token) return;
            try {
                let res = await fetch('/admin/questions', {
                    headers: { 'Authorization': 'Bearer ' + token }
                });
                if (!res.ok) return;
                let data = await res.json();
                let total = data.length;
                let pub = data.filter(q => q.status === 'PUBLISHED').length;
                let draft = total - pub;
                document.getElementById('stat-total').innerText = total;
                document.getElementById('stat-published').innerText = pub;
                document.getElementById('stat-draft').innerText = draft;
            } catch (e) {}
        }

        async function handleImportPreview(e) {
            e.preventDefault();
            let fileInput = document.getElementById('import-file');
            if (!fileInput.files.length) return;

            let file = fileInput.files[0];
            let text = await file.text();

            let res = await fetch('/admin/import/preview', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + token,
                    'Content-Type': 'text/plain'
                },
                body: text
            });

            if (res.status === 401) {
                handleLogout();
                alert('Phiên làm việc hết hạn (HTTP 401). Vui lòng đăng nhập lại.');
                return;
            }

            if (res.status === 403) {
                alert('Tài khoản của bạn không có quyền Quản trị / Biên soạn (HTTP 403)!');
                return;
            }

            if (!res.ok) {
                alert('Lỗi máy chủ khi kiểm tra file import (' + res.status + ').');
                return;
            }

            let data = await res.json();
            lastPreviewData = data;
            document.getElementById('import-results').style.display = 'block';
            document.getElementById('import-summary').innerHTML =
                `Tổng số dòng kiểm tra: <b>${data.totalRows}</b> | Số câu hợp lệ: <b class="text-success">${data.validCount}</b> | Số câu lỗi: <b class="text-danger">${data.errorCount}</b>`;

            let errDiv = document.getElementById('import-errors');
            let errList = document.getElementById('error-list');
            errList.innerHTML = '';
            if (data.errors && data.errors.length > 0) {
                errDiv.style.display = 'block';
                data.errors.forEach(err => {
                    let li = document.createElement('li');
                    li.innerText = escapeHtml(err.message);
                    errList.appendChild(li);
                });
            } else {
                errDiv.style.display = 'none';
            }

            let tbody = document.getElementById('preview-table-body');
            tbody.innerHTML = '';
            data.validRows.forEach((q, idx) => {
                let tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${idx + 1}</td>
                    <td><code>${escapeHtml(q.code)}</code></td>
                    <td>${escapeHtml(q.subjectId)}</td>
                    <td>${q.grade}</td>
                    <td>${escapeHtml(q.prompt)}</td>
                    <td><span class="badge bg-primary">${escapeHtml(q.correctOption)}</span></td>
                    <td>${escapeHtml(q.difficulty)}</td>
                `;
                tbody.appendChild(tr);
            });
        }

        async function confirmImport(publish) {
            if (isImporting) return;
            if (!lastPreviewData || !lastPreviewData.validRows || lastPreviewData.validRows.length === 0) {
                alert('Không có câu hỏi hợp lệ nào để import!');
                return;
            }

            let btnDraft = document.getElementById('btn-import-draft');
            let btnPublish = document.getElementById('btn-import-publish');
            isImporting = true;
            if (btnDraft) btnDraft.disabled = true;
            if (btnPublish) btnPublish.disabled = true;

            try {
                let res = await fetch('/admin/import/confirm', {
                    method: 'POST',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        validRows: lastPreviewData.validRows,
                        publish: publish
                    })
                });

                if (res.status === 401) {
                    handleLogout();
                    alert('Phiên đăng nhập đã hết hạn (HTTP 401). Vui lòng đăng nhập lại.');
                    return;
                }

                if (res.status === 403) {
                    alert('Tài khoản của bạn không có quyền Quản trị / Biên soạn (HTTP 403).');
                    return;
                }

                if (!res.ok) {
                    let errMsg = 'Lỗi máy chủ (' + res.status + ') khi import.';
                    try {
                        let errJson = await res.json();
                        if (errJson && errJson.code) errMsg = 'Lỗi nghiệp vụ (' + res.status + '): ' + errJson.code;
                    } catch(e) {}
                    alert(errMsg);
                    return;
                }

                let result;
                try {
                    result = await res.json();
                } catch(e) {
                    alert('Lỗi phản hồi dữ liệu từ máy chủ không hợp lệ.');
                    return;
                }

                if (!result || !Number.isSafeInteger(result.importedCount) || result.importedCount < 0) {
                    alert('Lỗi phản hồi: Kết quả import không hợp lệ.');
                    return;
                }

                alert('Đã lưu thành công ' + result.importedCount + ' câu hỏi! Trạng thái: ' + (publish ? 'Đã xuất bản' : 'Bản nháp'));
                showSection('questions');

            } catch (err) {
                alert('Lỗi kết nối mạng hoặc timeout: Không thể gửi yêu cầu import đến máy chủ. Hãy kiểm tra danh sách câu hỏi trước khi thử lại.');
            } finally {
                isImporting = false;
                if (btnDraft) btnDraft.disabled = false;
                if (btnPublish) btnPublish.disabled = false;
            }
        }

        async function loadQuestions() {
            if (!token) return;
            let subj = document.getElementById('filter-subject').value;
            let gr = document.getElementById('filter-grade').value;
            let st = document.getElementById('filter-status').value;

            let url = `/admin/questions?`;
            if (subj) url += `subject_id=${encodeURIComponent(subj)}&`;
            if (gr) url += `grade=${encodeURIComponent(gr)}&`;
            if (st) url += `status=${encodeURIComponent(st)}&`;

            let res = await fetch(url, {
                headers: { 'Authorization': 'Bearer ' + token }
            });

            let container = document.getElementById('questions-list');
            container.innerHTML = '';

            if (res.status === 401) {
                handleLogout();
                showAuthAlert('Phiên làm việc hết hạn. Vui lòng đăng nhập lại.');
                return;
            }

            if (res.status === 403) {
                let errCard = document.createElement('div');
                errCard.className = 'alert alert-danger text-center';
                errCard.textContent = 'Tài khoản của bạn không có quyền xem dữ liệu quản trị.';
                container.appendChild(errCard);
                return;
            }

            if (!res.ok) {
                let errCard = document.createElement('div');
                errCard.className = 'alert alert-warning text-center';
                errCard.textContent = 'Không thể tải danh sách câu hỏi (HTTP ' + res.status + ').';
                container.appendChild(errCard);
                return;
            }

            let questions = await res.json();
            if (!Array.isArray(questions) || questions.length === 0) {
                let emptyCard = document.createElement('div');
                emptyCard.className = 'alert alert-secondary text-center';
                emptyCard.textContent = 'Không tìm thấy câu hỏi nào phù hợp.';
                container.appendChild(emptyCard);
                return;
            }

            questions.forEach(q => {
                let card = document.createElement('div');
                card.className = 'card p-3 mb-3';
                let isPub = q.status === 'PUBLISHED';

                let headerRow = document.createElement('div');
                headerRow.className = 'd-flex justify-content-between align-items-center mb-2';

                let badgeGroup = document.createElement('div');
                badgeGroup.innerHTML = `
                    <span class="badge bg-secondary">${escapeHtml(q.subjectId).toUpperCase()}</span>
                    <span class="badge bg-dark">Lớp ${q.grade}</span>
                    <span class="badge ${isPub ? 'badge-published' : 'badge-draft'}">${escapeHtml(q.status)}</span>
                    <small class="text-muted ms-2">Mã: <code>${escapeHtml(q.code)}</code> (v${q.version})</small>
                `;

                let btnToggle = document.createElement('button');
                btnToggle.className = `btn btn-sm ${isPub ? 'btn-outline-warning' : 'btn-outline-success'}`;
                btnToggle.textContent = isPub ? 'Ẩn bản nháp' : '🚀 Xuất bản';
                btnToggle.addEventListener('click', () => togglePublish(q.id, !isPub));

                headerRow.appendChild(badgeGroup);
                headerRow.appendChild(btnToggle);

                let promptEl = document.createElement('h6');
                promptEl.className = 'fw-bold mb-2';
                promptEl.textContent = q.prompt;

                let optsRow = document.createElement('div');
                optsRow.className = 'row g-2 mb-2';
                optsRow.innerHTML = `
                    <div class="col-6"><small>A: ${escapeHtml(q.optionA)}</small></div>
                    <div class="col-6"><small>B: ${escapeHtml(q.optionB)}</small></div>
                    <div class="col-6"><small>C: ${escapeHtml(q.optionC)}</small></div>
                    <div class="col-6"><small>D: ${escapeHtml(q.optionD)}</small></div>
                `;

                let previewBox = document.createElement('div');
                previewBox.className = 'preview-box mt-2';
                previewBox.innerHTML = `
                    <small class="text-success fw-bold">✓ Đáp án đúng: ${escapeHtml(q.correctOption)}</small><br>
                    <small class="text-muted">💡 Lời giải: ${escapeHtml(q.explanation || 'Chưa có lời giải')}</small><br>
                    <small class="text-muted">📌 Nguồn: ${escapeHtml(q.source || 'Chưa xác định')} | Tác giả: ${escapeHtml(q.author || 'N/A')}</small>
                `;

                card.appendChild(headerRow);
                card.appendChild(promptEl);
                card.appendChild(optsRow);
                card.appendChild(previewBox);
                container.appendChild(card);
            });
        }

        async function togglePublish(qid, publish) {
            if (isPublishing) return;
            isPublishing = true;
            try {
                let res = await fetch(`/admin/questions/${encodeURIComponent(qid)}/publish?publish=${publish}`, {
                    method: 'POST',
                    headers: { 'Authorization': 'Bearer ' + token }
                });

                if (res.status === 401) {
                    handleLogout();
                    alert('Phiên đăng nhập đã hết hạn (HTTP 401). Vui lòng đăng nhập lại.');
                    return;
                }

                if (res.status === 403) {
                    alert('Tài khoản của bạn không có quyền xuất bản câu hỏi (HTTP 403)!');
                    return;
                }

                if (!res.ok) {
                    alert('Lỗi máy chủ (' + res.status + ') khi cập nhật trạng thái câu hỏi.');
                    return;
                }

                let result = await res.json();
                if (!result || !result.id) {
                    alert('Phản hồi cập nhật trạng thái không hợp lệ.');
                    return;
                }

                loadQuestions();
            } catch (e) {
                alert('Lỗi kết nối mạng: Không thể cập nhật trạng thái câu hỏi.');
            } finally {
                isPublishing = false;
            }
        }
    </script>
</body>
</html>
"""
