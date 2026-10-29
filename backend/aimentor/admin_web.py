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
                    <a class="nav-link" href="#" onclick="showSection('quizzes')">📚 Quản lý bộ đề</a>
                    <a class="nav-link" href="#" onclick="showSection('exams')">📋 Quản lý đề thi</a>
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
                        <div class="mb-3 d-flex gap-2">
                            <a href="/admin/template/excel?format=xlsx" class="btn btn-outline-success" download="Mau_Cau_Hoi_AI_Mentor.xlsx">
                                📊 Tải mẫu Excel (.xlsx)
                            </a>
                            <a href="/admin/template/excel?format=csv" class="btn btn-outline-secondary" download="Mau_Cau_Hoi_AI_Mentor.csv">
                                📄 Tải mẫu CSV (.csv)
                            </a>
                        </div>
                        <hr>
                        <form id="import-form" onsubmit="handleImportPreview(event)">
                            <div class="mb-3">
                                <label class="form-label fw-bold">Chọn file dữ liệu câu hỏi:</label>
                                <input type="file" id="import-file" class="form-control" accept=".csv,.xlsx,.txt" required onchange="handleFileChange()">
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

                <!-- Quizzes List Section -->
                <div id="section-quizzes" class="content-section" style="display:none;">
                    <div class="card p-3 mb-3">
                        <div class="row g-2 align-items-center">
                            <div class="col-md-3">
                                <select id="filter-quiz-subject" class="form-select" onchange="loadQuizzes()">
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
                            <div class="col-md-2">
                                <select id="filter-quiz-grade" class="form-select" onchange="loadQuizzes()">
                                    <option value="">Tất cả lớp</option>
                                    <option value="10" selected>Lớp 10</option>
                                    <option value="11">Lớp 11</option>
                                    <option value="12">Lớp 12</option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <select id="filter-quiz-status" class="form-select" onchange="loadQuizzes()">
                                    <option value="">Tất cả trạng thái</option>
                                    <option value="PUBLISHED">Đã xuất bản</option>
                                    <option value="DRAFT">Bản nháp</option>
                                </select>
                            </div>
                            <div class="col-md-2">
                                <button class="btn btn-primary w-100" onclick="loadQuizzes()">🔄 Tải lại</button>
                            </div>
                            <div class="col-md-2">
                                <button class="btn btn-success w-100" id="btn-open-create-quiz" onclick="openQuizModal()">➕ Tạo bộ đề</button>
                            </div>
                        </div>
                    </div>

                    <div id="quizzes-list"></div>
                </div>

                <!-- Exams List Section -->
                <div id="section-exams" class="content-section" style="display:none;">
                    <div class="card p-3 mb-3">
                        <div class="row g-2 align-items-center">
                            <div class="col-md-2">
                                <label class="form-label small fw-bold mb-1">Môn học</label>
                                <select id="filter-exam-subject" class="form-select" onchange="loadExams()">
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
                            <div class="col-md-2">
                                <label class="form-label small fw-bold mb-1">Khối lớp</label>
                                <select id="filter-exam-grade" class="form-select" onchange="loadExams()">
                                    <option value="">Tất cả khối lớp</option>
                                    <option value="10" selected>Lớp 10</option>
                                    <option value="11">Lớp 11</option>
                                    <option value="12">Lớp 12</option>
                                </select>
                            </div>
                            <div class="col-md-2">
                                <label class="form-label small fw-bold mb-1">Kỳ thi</label>
                                <select id="filter-exam-type" class="form-select" onchange="loadExams()">
                                    <option value="">Tất cả kỳ thi</option>
                                    <option value="MIDTERM">Giữa kỳ</option>
                                    <option value="FINAL">Cuối kỳ</option>
                                </select>
                            </div>
                            <div class="col-md-2">
                                <label class="form-label small fw-bold mb-1">Học kỳ</label>
                                <select id="filter-exam-semester" class="form-select" onchange="loadExams()">
                                    <option value="">Tất cả học kỳ</option>
                                    <option value="1">Học kỳ 1</option>
                                    <option value="2">Học kỳ 2</option>
                                </select>
                            </div>
                            <div class="col-md-2">
                                <label class="form-label small fw-bold mb-1">Trạng thái</label>
                                <select id="filter-exam-status" class="form-select" onchange="loadExams()">
                                    <option value="">Tất cả trạng thái</option>
                                    <option value="PUBLISHED">Đã xuất bản</option>
                                    <option value="DRAFT">Bản nháp</option>
                                </select>
                            </div>
                            <div class="col-md-2 d-flex gap-2 align-items-end pt-3">
                                <button class="btn btn-outline-secondary w-50" onclick="loadExams()">🔄 Tải lại</button>
                                <button class="btn btn-primary w-50" id="btn-open-create-exam" onclick="openExamModal()">➕ Tạo đề</button>
                            </div>
                        </div>
                    </div>

                    <div id="exams-list"></div>
                </div>
            </div>
        </div>
    </div>

    <!-- Quiz Modal -->
            <!-- Question Edit Modal -->
        <div class="modal fade" id="questionEditModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold" id="question-edit-modal-title">✏️ Chỉnh sửa câu hỏi</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" onclick="closeQuestionEditModal()"></button>
                    </div>
                    <div class="modal-body">
                        <div id="question-edit-modal-error" class="alert alert-danger" style="display:none;"></div>
                        <input type="hidden" id="q-edit-id">
                        <input type="hidden" id="q-edit-version">
                        <input type="hidden" id="q-edit-status">

                        <div class="row g-3">
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Mã câu hỏi <span class="text-danger">*</span></label>
                                <input type="text" id="q-edit-code" class="form-control" maxlength="64">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Môn học <span class="text-danger">*</span></label>
                                <select id="q-edit-subject" class="form-select">
                                    <option value="physics">Vật lý</option>
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
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Khối lớp <span class="text-danger">*</span></label>
                                <select id="q-edit-grade" class="form-select">
                                    <option value="10">Lớp 10</option>
                                    <option value="11">Lớp 11</option>
                                    <option value="12">Lớp 12</option>
                                </select>
                            </div>

                            <div class="col-md-4">
                                <label class="form-label">Chương</label>
                                <input type="text" id="q-edit-chapter" class="form-control">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Chủ đề</label>
                                <input type="text" id="q-edit-topic" class="form-control">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Bộ sách</label>
                                <input type="text" id="q-edit-textbook" class="form-control">
                            </div>

                            <div class="col-12">
                                <label class="form-label fw-semibold">Nội dung câu hỏi <span class="text-danger">*</span></label>
                                <textarea id="q-edit-prompt" class="form-control" rows="3" maxlength="5000"></textarea>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Phương án A <span class="text-danger">*</span></label>
                                <input type="text" id="q-edit-opt-a" class="form-control" maxlength="2000">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Phương án B <span class="text-danger">*</span></label>
                                <input type="text" id="q-edit-opt-b" class="form-control" maxlength="2000">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Phương án C <span class="text-danger">*</span></label>
                                <input type="text" id="q-edit-opt-c" class="form-control" maxlength="2000">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Phương án D <span class="text-danger">*</span></label>
                                <input type="text" id="q-edit-opt-d" class="form-control" maxlength="2000">
                            </div>

                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Đáp án đúng <span class="text-danger">*</span></label>
                                <select id="q-edit-correct" class="form-select">
                                    <option value="A">A</option>
                                    <option value="B">B</option>
                                    <option value="C">C</option>
                                    <option value="D">D</option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Độ khó <span class="text-danger">*</span></label>
                                <select id="q-edit-difficulty" class="form-select">
                                    <option value="EASY">Dễ (EASY)</option>
                                    <option value="MEDIUM">Trung bình (MEDIUM)</option>
                                    <option value="HARD">Khó (HARD)</option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Điểm số (1-100)</label>
                                <input type="number" id="q-edit-points" class="form-control" min="1" max="100" value="10">
                            </div>

                            <div class="col-12">
                                <label class="form-label">Lời giải chi tiết</label>
                                <textarea id="q-edit-explanation" class="form-control" rows="2"></textarea>
                            </div>

                            <div class="col-md-4">
                                <label class="form-label">Nguồn câu hỏi</label>
                                <input type="text" id="q-edit-source" class="form-control">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Tác giả</label>
                                <input type="text" id="q-edit-author" class="form-control">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Bản quyền</label>
                                <input type="text" id="q-edit-license" class="form-control">
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal" onclick="closeQuestionEditModal()">Đóng</button>
                        <button type="button" class="btn btn-primary" id="btn-save-question" onclick="saveQuestionEdit()">💾 Lưu thay đổi</button>
                    </div>
                </div>
            </div>
        </div>

<div class="modal fade" id="quizModal" tabindex="-1">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold" id="quiz-modal-title">Tạo / Chỉnh sửa Bộ đề</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" onclick="closeQuizModal()"></button>
                </div>
                <div class="modal-body">
                    <div id="quiz-modal-error" class="alert alert-danger" style="display:none;"></div>
                    <form id="quiz-form" onsubmit="event.preventDefault()">
                        <input type="hidden" id="quiz-id" value="">
                        <div class="mb-3">
                            <label class="form-label fw-bold">Tiêu đề bộ đề: <span class="text-danger">*</span></label>
                            <input type="text" id="quiz-title" class="form-control" required placeholder="Ví dụ: Ôn tập Động học chất điểm 10">
                        </div>
                        <div class="row g-2 mb-3">
                            <div class="col-md-4">
                                <label class="form-label fw-bold">Môn học: <span class="text-danger">*</span></label>
                                <select id="quiz-subject" class="form-select" onchange="handleQuizSubjectGradeChange()">
                                    <option value="physics">Vật lý</option>
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
                            <div class="col-md-4">
                                <label class="form-label fw-bold">Khối lớp: <span class="text-danger">*</span></label>
                                <select id="quiz-grade" class="form-select" onchange="handleQuizSubjectGradeChange()">
                                    <option value="10">Lớp 10</option>
                                    <option value="11">Lớp 11</option>
                                    <option value="12">Lớp 12</option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-bold">Độ khó:</label>
                                <select id="quiz-difficulty" class="form-select">
                                    <option value="EASY">Dễ (EASY)</option>
                                    <option value="MEDIUM" selected>Trung bình (MEDIUM)</option>
                                    <option value="HARD">Khó (HARD)</option>
                                </select>
                            </div>
                        </div>
                        <div class="row g-2 mb-3">
                            <div class="col-md-6">
                                <label class="form-label fw-bold">Chương:</label>
                                <input type="text" id="quiz-chapter" class="form-control" placeholder="Chương 1">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-bold">Chủ đề:</label>
                                <input type="text" id="quiz-topic" class="form-control" placeholder="Chuyển động thẳng đều">
                            </div>
                        </div>
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <label class="form-label fw-bold mb-0">Chọn câu hỏi trong kho: <span class="text-danger">*</span></label>
                                <span class="badge bg-primary">Đã chọn: <span id="quiz-selected-count">0</span> câu hỏi</span>
                            </div>
                            <div class="form-text mb-2">Lưu ý: Chỉ các câu hỏi cùng Môn và cùng Lớp mới hợp lệ. Khi xuất bản bộ đề, mọi câu hỏi được chọn phải ở trạng thái ĐÃ XUẤT BẢN.</div>
                            <div id="quiz-candidate-questions" style="max-height: 260px; overflow-y: auto; border: 1px solid #dee2e6; border-radius: 6px; padding: 10px; background: #fff;">
                                <div class="text-muted text-center py-2">Đang tải danh sách câu hỏi...</div>
                            </div>
                        </div>
                        <div class="d-flex justify-content-end gap-2 pt-2 border-top">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal" onclick="closeQuizModal()">Đóng</button>
                            <button type="button" id="btn-quiz-save-draft" class="btn btn-warning" onclick="saveQuiz('DRAFT')">💾 Lưu bản nháp (Draft)</button>
                            <button type="button" id="btn-quiz-save-publish" class="btn btn-success" onclick="saveQuiz('PUBLISHED')">🚀 Xuất bản ngay (Publish)</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <!-- Exam Modal -->
    <div class="modal fade" id="examModal" tabindex="-1">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold" id="exam-modal-title">Tạo / Chỉnh sửa Đề thi</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" onclick="closeExamModal()"></button>
                </div>
                <div class="modal-body">
                    <div id="exam-modal-error" class="alert alert-danger" style="display:none;"></div>
                    <form id="exam-form" onsubmit="event.preventDefault()">
                        <input type="hidden" id="exam-id" value="">
                        <input type="hidden" id="exam-version" value="1">
                        <div class="mb-3">
                            <label class="form-label fw-bold">Tiêu đề đề thi: <span class="text-danger">*</span></label>
                            <input type="text" id="exam-title" class="form-control" required placeholder="Ví dụ: Đề thi Giữa Học kỳ 1 Vật lý 10">
                        </div>
                        <div class="row g-2 mb-3">
                            <div class="col-md-3">
                                <label class="form-label fw-bold">Môn học: <span class="text-danger">*</span></label>
                                <select id="exam-subject" class="form-select" onchange="handleExamSubjectGradeChange()">
                                    <option value="physics">Vật lý</option>
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
                                <label class="form-label fw-bold">Khối lớp: <span class="text-danger">*</span></label>
                                <select id="exam-grade" class="form-select" onchange="handleExamSubjectGradeChange()">
                                    <option value="10">Lớp 10</option>
                                    <option value="11">Lớp 11</option>
                                    <option value="12">Lớp 12</option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label fw-bold">Loại kỳ thi: <span class="text-danger">*</span></label>
                                <select id="exam-type" class="form-select">
                                    <option value="MIDTERM" selected>Giữa kỳ (MIDTERM)</option>
                                    <option value="FINAL">Cuối kỳ (FINAL)</option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label fw-bold">Học kỳ: <span class="text-danger">*</span></label>
                                <select id="exam-semester" class="form-select">
                                    <option value="1" selected>Học kỳ 1</option>
                                    <option value="2">Học kỳ 2</option>
                                </select>
                            </div>
                        </div>
                        <div class="row g-2 mb-3">
                            <div class="col-md-6">
                                <label class="form-label fw-bold">Thời lượng làm bài (phút): <span class="text-danger">*</span></label>
                                <input type="number" id="exam-duration" class="form-control" value="45" min="5" max="300" required>
                            </div>
                        </div>
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <label class="form-label fw-bold mb-0">Chọn câu hỏi trong kho: <span class="text-danger">*</span></label>
                                <span class="badge bg-primary">Đã chọn: <span id="exam-selected-count">0</span> câu hỏi (<span id="exam-selected-points">0</span> điểm)</span>
                            </div>
                            <div class="form-text mb-2">Lưu ý: Chỉ các câu hỏi cùng Môn và cùng Lớp mới hợp lệ. Khi xuất bản đề thi, mọi câu hỏi được chọn phải ở trạng thái ĐÃ XUẤT BẢN.</div>
                            <div id="exam-candidate-questions" style="max-height: 260px; overflow-y: auto; border: 1px solid #dee2e6; border-radius: 6px; padding: 10px; background: #fff;">
                                <div class="text-muted text-center py-2">Đang tải danh sách câu hỏi...</div>
                            </div>
                        </div>
                        <div class="d-flex justify-content-end gap-2 pt-2 border-top">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal" onclick="closeExamModal()">Đóng</button>
                            <button type="button" id="btn-exam-save-draft" class="btn btn-warning" onclick="saveExam('DRAFT')">💾 Lưu bản nháp (Draft)</button>
                            <button type="button" id="btn-exam-save-publish" class="btn btn-success" onclick="saveExam('PUBLISHED')">🚀 Xuất bản ngay (Publish)</button>
                        </div>
                    </form>
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
        let previewSequence = 0;
        let loginModalObj = null;
        let isImporting = false;
        let isPublishing = false;
        let questionEditModalObj = null;
        let questionModalOpenGen = 0;
        let questionModalActiveGen = 0;
        let questionModalActiveQid = null;
        let questionModalClosingGen = 0;
        let currentQuestionSaveId = 0;
        let activeQuestionSaveOwner = null;
        let quizModalObj = null;
        let isSubmittingQuiz = false;
        let submittingModalGen = null;
        let isPublishingQuiz = false;
        let selectedQuizQuestionIds = new Set();
        let candidateQuestionsList = [];
        let quizModalSubject = 'physics';
        let quizModalGrade = '10';

        let examModalObj = null;
        let examModalOpenGen = 0;
        let examModalActiveGen = 0;
        let examModalActiveId = null;
        let examModalClosingGen = 0;
        let currentExamSaveId = 0;
        let activeExamSaveOwner = null;
        let selectedExamQuestionIds = new Set();
        let candidateExamQuestionsList = [];
        let examCandidateLoadSeq = 0;
        let isCandidateExamLoading = false;
        let isSubmittingExam = false;
        let examModalSubject = 'physics';
        let examModalGrade = '10';
        let candidateLoadSeq = 0;
        let isCandidateLoading = false;
        let quizModalOpenGen = 0;
        let quizModalActiveGen = 0;
        let modalClosingGen = 0;

        function handleFileChange() {
            if (isImporting) return;

            previewSequence++;
            lastPreviewData = null;
            let resDiv = document.getElementById('import-results');
            if (resDiv) resDiv.style.display = 'none';
            let btnDraft = document.getElementById('btn-import-draft');
            let btnPublish = document.getElementById('btn-import-publish');
            if (btnDraft) btnDraft.disabled = true;
            if (btnPublish) btnPublish.disabled = true;
            let confirmNotice = document.getElementById('import-confirm-notice');
            if (confirmNotice) confirmNotice.style.display = 'none';

            let btnPreview = document.getElementById('btn-import-preview');
            if (btnPreview) btnPreview.disabled = false;
        }

        async function parseApiErrorMessage(res, defaultTitle) {
            let statusText = res.status ? ` (HTTP ${res.status})` : '';
            let fallback = (defaultTitle || 'Yêu cầu không thành công') + statusText;
            try {
                let errJson = await res.json();
                if (!errJson) return fallback;

                let msg = errJson.message;
                let row = errJson.row;
                let code = errJson.code;

                if (!msg && errJson.detail) {
                    if (typeof errJson.detail === 'string') {
                        msg = errJson.detail;
                    } else if (Array.isArray(errJson.detail)) {
                        msg = errJson.detail.map(d => {
                            let loc = Array.isArray(d.loc) ? d.loc.filter(x => x !== 'body').join('.') : '';
                            return (loc ? loc + ': ' : '') + (d.msg || JSON.stringify(d));
                        }).join('; ');
                    } else if (typeof errJson.detail === 'object') {
                        msg = errJson.detail.message || errJson.detail.code;
                        if (!row && errJson.detail.row) row = errJson.detail.row;
                        if (!code && errJson.detail.code) code = errJson.detail.code;
                    }
                }

                if (msg) {
                    let rowPrefix = (row !== undefined && row !== null) ? `[Dòng ${row}] ` : '';
                    let codeSuffix = code ? ` (${code})` : '';
                    return `${rowPrefix}${msg}${codeSuffix}`;
                }
                if (code) {
                    return `Lỗi hệ thống (${code})`;
                }
            } catch (e) {}
            return fallback;
        }

        document.addEventListener('DOMContentLoaded', () => {
            loginModalObj = new bootstrap.Modal(document.getElementById('loginModal'));
            let qmEditEl = document.getElementById('questionEditModal');
            if (qmEditEl && qmEditEl.addEventListener && typeof bootstrap !== 'undefined' && bootstrap.Modal) {
                questionEditModalObj = new bootstrap.Modal(qmEditEl);
                qmEditEl.addEventListener('hide.bs.modal', function() {
                    questionModalClosingGen = questionModalActiveGen;
                    questionModalActiveGen = 0;
                    questionModalActiveQid = null;
                });
                qmEditEl.addEventListener('hidden.bs.modal', function() {
                    if (questionModalClosingGen && questionModalClosingGen === questionModalOpenGen) {
                        questionModalActiveGen = 0;
                        questionModalActiveQid = null;
                    }
                    questionModalClosingGen = 0;
                });
            }

            let qmEl = document.getElementById('quizModal');
            if (qmEl && typeof bootstrap !== 'undefined' && bootstrap.Modal) {
                quizModalObj = new bootstrap.Modal(qmEl);
                qmEl.addEventListener('hide.bs.modal', () => {
                    modalClosingGen = quizModalActiveGen;
                    handleModalDismiss(modalClosingGen);
                });
                qmEl.addEventListener('hidden.bs.modal', () => {
                    if (modalClosingGen && modalClosingGen === quizModalActiveGen) {
                        handleModalDismiss(modalClosingGen);
                    }
                    modalClosingGen = 0;
                });
            }

            let emEl = document.getElementById('examModal');
            if (emEl && typeof bootstrap !== 'undefined' && bootstrap.Modal) {
                examModalObj = new bootstrap.Modal(emEl);
                emEl.addEventListener('hide.bs.modal', () => {
                    examModalClosingGen = examModalActiveGen;
                    examModalActiveGen = 0;
                    examModalActiveId = null;
                });
                emEl.addEventListener('hidden.bs.modal', () => {
                    if (examModalClosingGen && examModalClosingGen === examModalOpenGen) {
                        examModalActiveGen = 0;
                        examModalActiveId = null;
                    }
                    examModalClosingGen = 0;
                });
            }
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
            let qzList = document.getElementById('quizzes-list');
            if (qzList) qzList.innerHTML = '';
            showAuthAlert('Bạn đã đăng xuất.');
        }

        function showSection(name) {
            document.querySelectorAll('.content-section').forEach(el => el.style.display = 'none');
            document.querySelectorAll('.sidebar .nav-link').forEach(el => el.classList.remove('active'));
            document.getElementById('section-' + name).style.display = 'block';
            if (name === 'dashboard') loadStats();
            if (name === 'questions') loadQuestions();
            if (name === 'quizzes') loadQuizzes();
            if (name === 'exams') loadExams();
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
            if (e && e.preventDefault) e.preventDefault();
            if (isImporting) {
                alert('Đang trong quá trình lưu câu hỏi, vui lòng đợi hoàn tất trước khi kiểm tra file mới!');
                return;
            }

            let fileInput = document.getElementById('import-file');
            if (!fileInput || !fileInput.files || !fileInput.files.length) return;

            let file = fileInput.files[0];
            let btnPreview = document.getElementById('btn-import-preview');
            let btnDraft = document.getElementById('btn-import-draft');
            let btnPublish = document.getElementById('btn-import-publish');

            // Sequence control: Invalidate old preview IMMEDIATELY before first await
            previewSequence++;
            const currentSeq = previewSequence;
            lastPreviewData = null;

            let resDiv = document.getElementById('import-results');
            if (resDiv) resDiv.style.display = 'none';
            if (btnDraft) btnDraft.disabled = true;
            if (btnPublish) btnPublish.disabled = true;
            if (btnPreview) btnPreview.disabled = true;

            let buffer;
            try {
                if (file.arrayBuffer) {
                    buffer = await file.arrayBuffer();
                } else if (file.text) {
                    let text = await file.text();
                    buffer = text;
                }
            } catch (err) {
                if (currentSeq !== previewSequence) return;
                lastPreviewData = null;
                if (btnPreview) btnPreview.disabled = false;
                alert('Không thể đọc file: ' + (err.message || 'Lỗi không xác định'));
                return;
            }

            if (currentSeq !== previewSequence) return;

            let res;
            try {
                res = await fetch('/admin/import/preview', {
                    method: 'POST',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/octet-stream'
                    },
                    body: buffer
                });
            } catch (err) {
                if (currentSeq !== previewSequence) return;
                lastPreviewData = null;
                if (btnPreview) btnPreview.disabled = false;
                alert('Lỗi kết nối mạng khi gửi file kiểm tra: ' + (err.message || 'Không thể kết nối máy chủ'));
                return;
            }

            if (currentSeq !== previewSequence) return;

            if (res.status === 401) {
                if (btnPreview) btnPreview.disabled = false;
                handleLogout();
                alert('Phiên làm việc hết hạn (HTTP 401). Vui lòng đăng nhập lại.');
                return;
            }

            if (res.status === 403) {
                if (btnPreview) btnPreview.disabled = false;
                alert('Tài khoản của bạn không có quyền Quản trị / Biên soạn (HTTP 403)!');
                return;
            }

            if (!res.ok) {
                let errMsg = await parseApiErrorMessage(res, 'Lỗi kiểm tra file import');
                if (currentSeq !== previewSequence) return;
                lastPreviewData = null;
                if (btnPreview) btnPreview.disabled = false;
                alert(errMsg);
                return;
            }

            let data;
            try {
                data = await res.json();
            } catch (err) {
                if (currentSeq !== previewSequence) return;
                lastPreviewData = null;
                if (btnPreview) btnPreview.disabled = false;
                alert('Lỗi phản hồi dữ liệu từ máy chủ (JSON không hợp lệ).');
                return;
            }

            if (currentSeq !== previewSequence) return;
            if (btnPreview) btnPreview.disabled = false;

            // Render preview results safely
            lastPreviewData = data;
            if (lastPreviewData && !lastPreviewData.operationKeys) {
                lastPreviewData.operationKeys = {
                    draft: 'imp_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9),
                    publish: 'imp_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9)
                };
            }
            if (resDiv) resDiv.style.display = 'block';

            let summaryEl = document.getElementById('import-summary');
            if (summaryEl) {
                summaryEl.innerHTML = `Tổng số dòng kiểm tra: <b>${data.totalRows}</b> | Số câu hợp lệ: <b class="text-success">${data.validCount}</b> | Số câu lỗi: <b class="text-danger">${data.errorCount}</b>`;
            }

            let errDiv = document.getElementById('import-errors');
            let errList = document.getElementById('error-list');
            if (errList) errList.innerHTML = '';
            if (data.errors && data.errors.length > 0) {
                if (errDiv) errDiv.style.display = 'block';
                data.errors.forEach(err => {
                    let li = document.createElement('li');
                    li.textContent = err.message || JSON.stringify(err);
                    if (errList) errList.appendChild(li);
                });
            } else {
                if (errDiv) errDiv.style.display = 'none';
            }

            let tbody = document.getElementById('preview-table-body');
            if (tbody) {
                tbody.innerHTML = '';
                (data.validRows || []).forEach((q, idx) => {
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

            let hasErrors = (data.errors && data.errors.length > 0);
            let confirmNotice = document.getElementById('import-confirm-notice');
            if (!confirmNotice && btnDraft && btnDraft.parentNode) {
                confirmNotice = document.createElement('div');
                confirmNotice.id = 'import-confirm-notice';
                btnDraft.parentNode.insertBefore(confirmNotice, btnDraft);
            }

            if (hasErrors) {
                if (btnDraft) btnDraft.disabled = true;
                if (btnPublish) btnPublish.disabled = true;
                if (confirmNotice) {
                    confirmNotice.className = 'alert alert-warning mb-3 w-100';
                    confirmNotice.textContent = `⚠️ File còn ${data.errorCount} lỗi. Để đảm bảo tính toàn vẹn dữ liệu, vui lòng sửa lại toàn bộ lỗi trong file và tải lên kiểm tra lại trước khi có thể Import.`;
                    confirmNotice.style.display = 'block';
                }
            } else if (data.validCount === 0) {
                if (btnDraft) btnDraft.disabled = true;
                if (btnPublish) btnPublish.disabled = true;
                if (confirmNotice) {
                    confirmNotice.className = 'alert alert-secondary mb-3 w-100';
                    confirmNotice.textContent = 'Không có câu hỏi hợp lệ nào để import.';
                    confirmNotice.style.display = 'block';
                }
            } else {
                if (btnDraft) btnDraft.disabled = false;
                if (btnPublish) btnPublish.disabled = false;
                if (confirmNotice) confirmNotice.style.display = 'none';
            }
        }

        async function confirmImport(publish) {
            if (isImporting) return;
            if (!lastPreviewData || !lastPreviewData.validRows || lastPreviewData.validRows.length === 0) {
                alert('Không có câu hỏi hợp lệ nào để import!');
                return;
            }
            if (lastPreviewData.errors && lastPreviewData.errors.length > 0) {
                alert('File vẫn còn lỗi chưa được sửa (' + lastPreviewData.errorCount + ' lỗi). Vui lòng sửa lỗi và kiểm tra lại (Preview) trước khi import.');
                return;
            }

            let btnDraft = document.getElementById('btn-import-draft');
            let btnPublish = document.getElementById('btn-import-publish');
            let btnPreview = document.getElementById('btn-import-preview');
            let fileInput = document.getElementById('import-file');

            isImporting = true;
            const confirmSeq = previewSequence;
            const rowsToConfirm = lastPreviewData.validRows;

            if (btnDraft) btnDraft.disabled = true;
            if (btnPublish) btnPublish.disabled = true;
            if (btnPreview) btnPreview.disabled = true;
            if (fileInput) fileInput.disabled = true;

            try {
                const opKey = (lastPreviewData && lastPreviewData.operationKeys) ?
                    (publish ? lastPreviewData.operationKeys.publish : lastPreviewData.operationKeys.draft) :
                    ('imp_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9));

                let res = await fetch('/admin/import/confirm', {
                    method: 'POST',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json',
                        'Idempotency-Key': opKey
                    },
                    body: JSON.stringify({
                        importKey: opKey,
                        validRows: rowsToConfirm,
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
                    let errMsg = await parseApiErrorMessage(res, 'Lỗi import');
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

                // Only clear preview if user has not loaded a newer preview during confirmation
                if (confirmSeq === previewSequence) {
                    lastPreviewData = null;
                    let resDiv = document.getElementById('import-results');
                    if (resDiv) resDiv.style.display = 'none';
                    if (fileInput) fileInput.value = '';
                }

                alert('Đã lưu thành công ' + result.importedCount + ' câu hỏi! Trạng thái: ' + (publish ? 'Đã xuất bản' : 'Bản nháp'));
                showSection('questions');

            } catch (err) {
                alert('Lỗi kết nối mạng hoặc timeout: Không thể gửi yêu cầu import đến máy chủ. Hãy kiểm tra danh sách câu hỏi trước khi thử lại.');
            } finally {
                isImporting = false;
                if (btnPreview) btnPreview.disabled = false;
                if (fileInput) fileInput.disabled = false;
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

                let actionGroup = document.createElement('div');
                actionGroup.className = 'd-flex align-items-center gap-2';

                let btnEdit = document.createElement('button');
                btnEdit.className = 'btn btn-sm btn-outline-primary';
                btnEdit.textContent = '✏️ Sửa';
                btnEdit.addEventListener('click', () => openQuestionEditModal(q));

                let btnToggle = document.createElement('button');
                btnToggle.className = `btn btn-sm ${isPub ? 'btn-outline-warning' : 'btn-outline-success'}`;
                btnToggle.textContent = isPub ? 'Ẩn bản nháp' : '🚀 Xuất bản';
                btnToggle.addEventListener('click', () => togglePublish(q.id, !isPub));

                actionGroup.appendChild(btnEdit);
                actionGroup.appendChild(btnToggle);

                headerRow.appendChild(badgeGroup);
                headerRow.appendChild(actionGroup);

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

        function isQuestionModalSessionActive(session) {
            return (
                session &&
                questionModalActiveGen !== 0 &&
                session.gen === questionModalActiveGen &&
                session.qid === questionModalActiveQid
            );
        }

        function openQuestionEditModal(q) {
            if (!q) return;
            questionModalOpenGen++;
            const currentGen = questionModalOpenGen;
            questionModalActiveGen = currentGen;
            questionModalActiveQid = q.id || '';
            questionModalClosingGen = 0;

            // Clear any active save ownership so new modal is immediately unlocked
            activeQuestionSaveOwner = null;
            let btnSave = document.getElementById('btn-save-question');
            if (btnSave) {
                btnSave.disabled = false;
                btnSave.textContent = '💾 Lưu thay đổi';
            }

            let errDiv = document.getElementById('question-edit-modal-error');
            if (errDiv) {
                errDiv.style.display = 'none';
                errDiv.textContent = '';
            }

            document.getElementById('q-edit-id').value = q.id || '';
            document.getElementById('q-edit-version').value = q.version != null ? q.version : 1;
            document.getElementById('q-edit-status').value = q.status || 'DRAFT';
            document.getElementById('q-edit-code').value = q.code || '';
            document.getElementById('q-edit-subject').value = q.subjectId || 'physics';
            document.getElementById('q-edit-grade').value = String(q.grade || 10);
            document.getElementById('q-edit-chapter').value = q.chapter || '';
            document.getElementById('q-edit-topic').value = q.topic || '';
            document.getElementById('q-edit-textbook').value = q.textbook || '';
            document.getElementById('q-edit-prompt').value = q.prompt || '';
            document.getElementById('q-edit-opt-a').value = q.optionA || '';
            document.getElementById('q-edit-opt-b').value = q.optionB || '';
            document.getElementById('q-edit-opt-c').value = q.optionC || '';
            document.getElementById('q-edit-opt-d').value = q.optionD || '';
            document.getElementById('q-edit-correct').value = (q.correctOption || 'A').toUpperCase();
            document.getElementById('q-edit-difficulty').value = q.difficulty || 'MEDIUM';
            document.getElementById('q-edit-points').value = q.points != null ? q.points : 10;
            document.getElementById('q-edit-explanation').value = q.explanation || '';
            document.getElementById('q-edit-source').value = q.source || '';
            document.getElementById('q-edit-author').value = q.author || '';
            document.getElementById('q-edit-license').value = q.license || '';

            let modalTitle = document.getElementById('question-edit-modal-title');
            if (modalTitle) {
                modalTitle.textContent = `✏️ Chỉnh sửa câu hỏi [${q.code || q.id}] (v${q.version != null ? q.version : 1})`;
            }

            if (questionEditModalObj) {
                questionEditModalObj.show();
            } else {
                let el = document.getElementById('questionEditModal');
                if (typeof bootstrap !== 'undefined' && bootstrap.Modal) {
                    questionEditModalObj = new bootstrap.Modal(el);
                    questionEditModalObj.show();
                } else if (el) {
                    el.style.display = 'block';
                    el.classList.add('show');
                }
            }
        }

        function closeQuestionEditModal() {
            questionModalActiveGen = 0;
            questionModalActiveQid = null;
            if (questionEditModalObj) {
                questionEditModalObj.hide();
            } else {
                let el = document.getElementById('questionEditModal');
                if (el) {
                    el.style.display = 'none';
                    el.classList.remove('show');
                }
            }
        }

        async function saveQuestionEdit() {
            if (questionModalActiveGen === 0 || !questionModalActiveQid) return;
            const sessionGen = questionModalActiveGen;
            const sessionQid = document.getElementById('q-edit-id').value;
            if (sessionQid !== questionModalActiveQid) return;

            // Anti-double-click guard for active modal session
            if (activeQuestionSaveOwner && activeQuestionSaveOwner.gen === sessionGen) {
                return;
            }

            let errDiv = document.getElementById('question-edit-modal-error');
            if (errDiv) {
                errDiv.style.display = 'none';
                errDiv.textContent = '';
            }

            let expectedVer = parseInt(document.getElementById('q-edit-version').value, 10);
            let code = document.getElementById('q-edit-code').value.trim();
            let prompt = document.getElementById('q-edit-prompt').value.trim();
            let optA = document.getElementById('q-edit-opt-a').value.trim();
            let optB = document.getElementById('q-edit-opt-b').value.trim();
            let optC = document.getElementById('q-edit-opt-c').value.trim();
            let optD = document.getElementById('q-edit-opt-d').value.trim();
            let correct = document.getElementById('q-edit-correct').value.trim().toUpperCase();
            let points = parseInt(document.getElementById('q-edit-points').value, 10);

            if (!prompt) {
                if (errDiv) { errDiv.textContent = 'Nội dung câu hỏi không được để trống.'; errDiv.style.display = 'block'; }
                return;
            }
            if (!optA || !optB || !optC || !optD) {
                if (errDiv) { errDiv.textContent = 'Đủ 4 phương án A, B, C, D không được để trống.'; errDiv.style.display = 'block'; }
                return;
            }
            if (!['A', 'B', 'C', 'D'].includes(correct)) {
                if (errDiv) { errDiv.textContent = 'Phương án đúng phải là một trong 4 ký tự: A, B, C hoặc D.'; errDiv.style.display = 'block'; }
                return;
            }

            let payload = {
                id: sessionQid,
                expectedVersion: isNaN(expectedVer) ? null : expectedVer,
                code: code,
                subjectId: document.getElementById('q-edit-subject').value,
                grade: parseInt(document.getElementById('q-edit-grade').value, 10),
                chapter: document.getElementById('q-edit-chapter').value.trim(),
                topic: document.getElementById('q-edit-topic').value.trim(),
                textbook: document.getElementById('q-edit-textbook').value.trim(),
                prompt: prompt,
                optionA: optA,
                optionB: optB,
                optionC: optC,
                optionD: optD,
                correctOption: correct,
                explanation: document.getElementById('q-edit-explanation').value.trim(),
                difficulty: document.getElementById('q-edit-difficulty').value,
                points: isNaN(points) ? 10 : points,
                source: document.getElementById('q-edit-source').value.trim(),
                author: document.getElementById('q-edit-author').value.trim(),
                license: document.getElementById('q-edit-license').value.trim(),
                status: document.getElementById('q-edit-status').value
            };

            currentQuestionSaveId++;
            const thisSaveId = currentQuestionSaveId;
            const saveSession = {
                saveId: thisSaveId,
                gen: sessionGen,
                qid: sessionQid,
                expectedVer: isNaN(expectedVer) ? null : expectedVer
            };
            activeQuestionSaveOwner = saveSession;

            let btnSave = document.getElementById('btn-save-question');
            if (btnSave) {
                btnSave.disabled = true;
                btnSave.textContent = '⏳ Đang lưu...';
            }

            try {
                let res;
                try {
                    res = await fetch('/admin/questions', {
                        method: 'POST',
                        headers: {
                            'Authorization': 'Bearer ' + token,
                            'Content-Type': 'application/json',
                            'If-Match': saveSession.expectedVer != null ? `"${saveSession.expectedVer}"` : ''
                        },
                        body: JSON.stringify(payload)
                    });
                } catch (netErr) {
                    if (!isQuestionModalSessionActive(saveSession)) return;
                    if (errDiv) {
                        errDiv.textContent = 'Lỗi kết nối mạng: Không thể lưu câu hỏi. Hãy thử lại.';
                        errDiv.style.display = 'block';
                    }
                    return;
                }

                // Guard after fetch
                if (!isQuestionModalSessionActive(saveSession)) return;

                if (res.status === 401) {
                    handleLogout();
                    alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
                    return;
                }

                if (res.status === 403) {
                    if (errDiv) {
                        errDiv.textContent = 'Tài khoản không có quyền chỉnh sửa câu hỏi (HTTP 403).';
                        errDiv.style.display = 'block';
                    }
                    return;
                }

                if (!res.ok) {
                    let errMsg;
                    try {
                        errMsg = await parseApiErrorMessage(res, 'Lỗi lưu câu hỏi');
                    } catch(e) {
                        errMsg = 'Lỗi lưu câu hỏi (HTTP ' + res.status + ')';
                    }

                    // Guard after parseApiErrorMessage
                    if (!isQuestionModalSessionActive(saveSession)) return;

                    if (errDiv) {
                        errDiv.textContent = errMsg;
                        errDiv.style.display = 'block';
                    }
                    return;
                }

                let updatedQ;
                try {
                    updatedQ = await res.json();
                } catch (jsonErr) {
                    // Guard after json parse failure
                    if (!isQuestionModalSessionActive(saveSession)) return;
                    if (errDiv) {
                        errDiv.textContent = 'Máy chủ trả dữ liệu JSON không hợp lệ. Vui lòng kiểm tra lại danh sách câu hỏi.';
                        errDiv.style.display = 'block';
                    }
                    return;
                }

                // Guard after json parse
                if (!isQuestionModalSessionActive(saveSession)) return;

                // Strict contract validation
                if (!updatedQ || typeof updatedQ !== 'object' || updatedQ.id !== sessionQid || updatedQ.version == null || typeof updatedQ.version !== 'number') {
                    if (errDiv) {
                        errDiv.textContent = 'Phản hồi từ máy chủ không khớp định danh câu hỏi hoặc cấu trúc không hợp lệ.';
                        errDiv.style.display = 'block';
                    }
                    return;
                }

                closeQuestionEditModal();
                alert(`Đã lưu thành công câu hỏi [${updatedQ.code || sessionQid}] (phiên bản v${updatedQ.version})!`);
                await loadQuestions();

            } catch (err) {
                if (!isQuestionModalSessionActive(saveSession)) return;
                if (errDiv) {
                    errDiv.textContent = 'Lỗi không xác định: ' + (err.message || 'Lỗi thao tác');
                    errDiv.style.display = 'block';
                }
            } finally {
                // Only unlock button if this save operation is still the active owner
                if (activeQuestionSaveOwner && activeQuestionSaveOwner.saveId === thisSaveId) {
                    activeQuestionSaveOwner = null;
                    let currentBtn = document.getElementById('btn-save-question');
                    if (currentBtn) {
                        currentBtn.disabled = false;
                        currentBtn.textContent = '💾 Lưu thay đổi';
                    }
                }
            }
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

        async function loadQuizzes() {
            if (!token) return;
            let subj = document.getElementById('filter-quiz-subject') ? document.getElementById('filter-quiz-subject').value : '';
            let gr = document.getElementById('filter-quiz-grade') ? document.getElementById('filter-quiz-grade').value : '';
            let st = document.getElementById('filter-quiz-status') ? document.getElementById('filter-quiz-status').value : '';

            let url = '/admin/quizzes?';
            if (subj) url += 'subject_id=' + encodeURIComponent(subj) + '&';
            if (gr) url += 'grade=' + encodeURIComponent(gr) + '&';
            if (st) url += 'status=' + encodeURIComponent(st) + '&';

            let container = document.getElementById('quizzes-list');
            if (!container) return;
            container.innerHTML = '<div class="text-center py-3 text-muted">Đang tải danh sách bộ đề...</div>';

            try {
                let res = await fetch(url, {
                    headers: { 'Authorization': 'Bearer ' + token }
                });

                if (res.status === 401) {
                    handleLogout();
                    showAuthAlert('Phiên làm việc hết hạn. Vui lòng đăng nhập lại.');
                    return;
                }

                if (res.status === 403) {
                    container.innerHTML = '<div class="alert alert-danger text-center">Tài khoản của bạn không có quyền xem danh sách bộ đề.</div>';
                    return;
                }

                if (!res.ok) {
                    container.innerHTML = '<div class="alert alert-warning text-center">Không thể tải danh sách bộ đề (HTTP ' + res.status + ').</div>';
                    return;
                }

                let quizzes = await res.json();
                renderQuizzes(quizzes);
            } catch (err) {
                container.innerHTML = '<div class="alert alert-danger text-center">Lỗi kết nối khi tải bộ đề: ' + escapeHtml(err.message || 'Lỗi mạng') + '</div>';
            }
        }

        function renderQuizzes(quizzes) {
            let container = document.getElementById('quizzes-list');
            if (!container) return;
            container.innerHTML = '';

            if (!Array.isArray(quizzes) || quizzes.length === 0) {
                let emptyCard = document.createElement('div');
                emptyCard.className = 'alert alert-secondary text-center';
                emptyCard.textContent = 'Không tìm thấy bộ đề nào phù hợp.';
                container.appendChild(emptyCard);
                return;
            }

            quizzes.forEach(quiz => {
                let card = document.createElement('div');
                card.className = 'card p-3 mb-3';
                let isPub = quiz.status === 'PUBLISHED';

                let headerRow = document.createElement('div');
                headerRow.className = 'd-flex justify-content-between align-items-center mb-2';

                let badgeGroup = document.createElement('div');
                badgeGroup.innerHTML = `
                    <span class="badge bg-secondary">${escapeHtml(quiz.subjectId).toUpperCase()}</span>
                    <span class="badge bg-dark">Lớp ${quiz.grade}</span>
                    <span class="badge ${isPub ? 'badge-published' : 'badge-draft'}">${escapeHtml(quiz.status)}</span>
                    <span class="badge bg-info text-dark">${quiz.questionCount || 0} câu hỏi</span>
                    <span class="badge bg-light text-muted border">${escapeHtml(quiz.difficulty || 'MEDIUM')}</span>
                `;

                let actionGroup = document.createElement('div');
                actionGroup.className = 'd-flex gap-2';

                let btnEdit = document.createElement('button');
                btnEdit.className = 'btn btn-sm btn-outline-primary';
                btnEdit.textContent = '✏️ Sửa';
                btnEdit.onclick = () => openQuizModal(quiz);

                let btnToggle = document.createElement('button');
                btnToggle.className = `btn btn-sm ${isPub ? 'btn-outline-warning' : 'btn-outline-success'}`;
                btnToggle.textContent = isPub ? 'Ẩn bản nháp' : '🚀 Xuất bản';
                btnToggle.onclick = () => toggleQuizPublish(quiz.id, !isPub);

                actionGroup.appendChild(btnEdit);
                actionGroup.appendChild(btnToggle);
                headerRow.appendChild(badgeGroup);
                headerRow.appendChild(actionGroup);

                let titleEl = document.createElement('h5');
                titleEl.className = 'fw-bold mb-1';
                titleEl.textContent = quiz.title;

                let metaEl = document.createElement('div');
                metaEl.className = 'text-muted small mb-2';
                let metaParts = [];
                if (quiz.chapter) metaParts.push('Chương: ' + quiz.chapter);
                if (quiz.topic) metaParts.push('Chủ đề: ' + quiz.topic);
                if (quiz.createdAt) metaParts.push('Tạo lúc: ' + quiz.createdAt);
                metaEl.textContent = metaParts.join(' | ') || 'Chưa có thông tin chương/chủ đề';

                card.appendChild(headerRow);
                card.appendChild(titleEl);
                card.appendChild(metaEl);
                container.appendChild(card);
            });
        }

        function setQuizFormDisabled(disabled) {
            const fieldIds = ['quiz-title', 'quiz-subject', 'quiz-grade', 'quiz-difficulty', 'quiz-chapter', 'quiz-topic'];
            fieldIds.forEach(id => {
                const el = document.getElementById(id);
                if (el) el.disabled = disabled;
            });
            const candContainer = document.getElementById('quiz-candidate-questions');
            if (candContainer && candContainer.style) {
                candContainer.style.pointerEvents = disabled ? 'none' : '';
                candContainer.style.opacity = disabled ? '0.6' : '';
            }
        }

        function handleModalDismiss(targetGen = null) {
            if (targetGen !== null && targetGen !== quizModalActiveGen) {
                return;
            }
            if (!quizModalActiveGen) {
                return;
            }
            quizModalOpenGen++;
            quizModalActiveGen = 0;
            modalClosingGen = 0;
            candidateLoadSeq++;
            isCandidateLoading = false;
            isSubmittingQuiz = false;
            submittingModalGen = null;
            setQuizFormDisabled(false);
        }

        function closeQuizModal() {
            handleModalDismiss(quizModalActiveGen);
            if (quizModalObj) {
                quizModalObj.hide();
            } else {
                let modalEl = document.getElementById('quizModal');
                if (modalEl) modalEl.style.display = 'none';
            }
        }

        async function openQuizModal(quizData) {
            quizModalOpenGen++;
            quizModalActiveGen = quizModalOpenGen;
            modalClosingGen = 0;
            candidateLoadSeq++;
            isSubmittingQuiz = false;
            submittingModalGen = null;
            setQuizFormDisabled(false);

            let errBox = document.getElementById('quiz-modal-error');
            if (errBox) errBox.style.display = 'none';

            let titleEl = document.getElementById('quiz-modal-title');
            let idEl = document.getElementById('quiz-id');
            let nameEl = document.getElementById('quiz-title');
            let subjEl = document.getElementById('quiz-subject');
            let grEl = document.getElementById('quiz-grade');
            let diffEl = document.getElementById('quiz-difficulty');
            let chEl = document.getElementById('quiz-chapter');
            let topEl = document.getElementById('quiz-topic');

            if (quizData) {
                if (titleEl) titleEl.textContent = 'Chỉnh sửa Bộ đề';
                if (idEl) idEl.value = quizData.id || '';
                if (nameEl) nameEl.value = quizData.title || '';
                quizModalSubject = quizData.subjectId || 'physics';
                quizModalGrade = String(quizData.grade || 10);
                if (diffEl) diffEl.value = quizData.difficulty || 'MEDIUM';
                if (chEl) chEl.value = quizData.chapter || '';
                if (topEl) topEl.value = quizData.topic || '';
                selectedQuizQuestionIds = new Set(quizData.questionIds || []);
            } else {
                if (titleEl) titleEl.textContent = 'Tạo Bộ đề mới';
                if (idEl) idEl.value = '';
                if (nameEl) nameEl.value = '';
                quizModalSubject = 'physics';
                quizModalGrade = '10';
                if (diffEl) diffEl.value = 'MEDIUM';
                if (chEl) chEl.value = '';
                if (topEl) topEl.value = '';
                selectedQuizQuestionIds = new Set();
            }

            if (subjEl) subjEl.value = quizModalSubject;
            if (grEl) grEl.value = quizModalGrade;

            candidateQuestionsList = [];
            updateSelectedQuizCount();

            if (quizModalObj) {
                quizModalObj.show();
            } else {
                let modalEl = document.getElementById('quizModal');
                if (modalEl) modalEl.style.display = 'block';
            }

            await loadCandidateQuestions();
        }

        function handleQuizSubjectGradeChange() {
            let subjEl = document.getElementById('quiz-subject');
            let grEl = document.getElementById('quiz-grade');
            if (!subjEl || !grEl) return;

            let newSubj = subjEl.value;
            let newGrade = grEl.value;

            // If no change compared to confirmed active subject & grade, do nothing
            if (newSubj === quizModalSubject && newGrade === quizModalGrade) {
                return;
            }

            // If no questions selected, apply immediately
            if (selectedQuizQuestionIds.size === 0) {
                quizModalSubject = newSubj;
                quizModalGrade = newGrade;
                loadCandidateQuestions();
                return;
            }

            // Questions are currently selected: prompt user confirmation
            let confirmed = window.confirm('Thay đổi môn học hoặc khối lớp sẽ bỏ các câu hỏi đang chọn. Bạn có chắc chắn muốn đổi không?');
            if (confirmed) {
                selectedQuizQuestionIds.clear();
                updateSelectedQuizCount();
                quizModalSubject = newSubj;
                quizModalGrade = newGrade;
                loadCandidateQuestions();
            } else {
                // Cancelled: Revert both dropdowns back to active subject and grade
                subjEl.value = quizModalSubject;
                grEl.value = quizModalGrade;
            }
        }

        async function loadCandidateQuestions() {
            let container = document.getElementById('quiz-candidate-questions');
            if (!container) return;

            candidateLoadSeq++;
            const currentSeq = candidateLoadSeq;
            const currentGen = quizModalOpenGen;
            const targetSubj = quizModalSubject;
            const targetGrade = quizModalGrade;

            isCandidateLoading = true;

            let btnDraft = document.getElementById('btn-quiz-save-draft');
            let btnPublish = document.getElementById('btn-quiz-save-publish');
            if (btnDraft) btnDraft.disabled = true;
            if (btnPublish) btnPublish.disabled = true;

            container.innerHTML = '<div class="text-muted text-center py-3">⏳ Đang tải câu hỏi ' + escapeHtml(targetSubj) + ' lớp ' + escapeHtml(targetGrade) + '...</div>';

            let res;
            try {
                res = await fetch('/admin/questions?subject_id=' + encodeURIComponent(targetSubj) + '&grade=' + encodeURIComponent(targetGrade), {
                    headers: { 'Authorization': 'Bearer ' + token }
                });
            } catch (err) {
                if (currentSeq !== candidateLoadSeq || currentGen !== quizModalOpenGen) return;
                isCandidateLoading = false;
                container.innerHTML = `
                    <div class="alert alert-danger text-center my-2 p-2">
                        <div>⚠️ Lỗi kết nối khi tải danh sách câu hỏi: ${escapeHtml(err.message || 'Lỗi mạng')}</div>
                        <button type="button" class="btn btn-sm btn-outline-danger mt-2" onclick="loadCandidateQuestions()">🔄 Thử lại</button>
                    </div>
                `;
                if (btnDraft) btnDraft.disabled = false;
                if (btnPublish) btnPublish.disabled = false;
                return;
            }

            if (currentSeq !== candidateLoadSeq || currentGen !== quizModalOpenGen) return;

            if (!res.ok) {
                isCandidateLoading = false;
                container.innerHTML = `
                    <div class="alert alert-warning text-center my-2 p-2">
                        <div>⚠️ Không thể tải danh sách câu hỏi (HTTP ${res.status}).</div>
                        <button type="button" class="btn btn-sm btn-outline-warning mt-2" onclick="loadCandidateQuestions()">🔄 Thử lại</button>
                    </div>
                `;
                if (btnDraft) btnDraft.disabled = false;
                if (btnPublish) btnPublish.disabled = false;
                return;
            }

            let questions;
            try {
                questions = await res.json();
            } catch (err) {
                if (currentSeq !== candidateLoadSeq || currentGen !== quizModalOpenGen) return;
                isCandidateLoading = false;
                container.innerHTML = `
                    <div class="alert alert-danger text-center my-2 p-2">
                        <div>⚠️ Dữ liệu câu hỏi từ máy chủ không hợp lệ.</div>
                        <button type="button" class="btn btn-sm btn-outline-danger mt-2" onclick="loadCandidateQuestions()">🔄 Thử lại</button>
                    </div>
                `;
                if (btnDraft) btnDraft.disabled = false;
                if (btnPublish) btnPublish.disabled = false;
                return;
            }

            if (currentSeq !== candidateLoadSeq || currentGen !== quizModalOpenGen) return;

            candidateQuestionsList = Array.isArray(questions) ? questions : [];
            isCandidateLoading = false;

            renderCandidateQuestions();

            if (btnDraft) btnDraft.disabled = false;
            if (btnPublish) btnPublish.disabled = false;
        }

        function renderCandidateQuestions() {
            let container = document.getElementById('quiz-candidate-questions');
            if (!container) return;
            container.innerHTML = '';

            const candidateIdMap = new Map();
            (candidateQuestionsList || []).forEach(q => candidateIdMap.set(q.id, q));

            // Check for missing questions (questions currently selected but not in candidate list)
            const missingIds = [];
            selectedQuizQuestionIds.forEach(id => {
                if (!candidateIdMap.has(id)) {
                    missingIds.push(id);
                }
            });

            if (missingIds.length > 0) {
                let warnDiv = document.createElement('div');
                warnDiv.className = 'alert alert-warning p-2 mb-2';

                let warnHeader = document.createElement('div');
                warnHeader.className = 'fw-bold text-dark';
                warnHeader.textContent = `⚠️ Có ${missingIds.length} câu hỏi đã chọn không có trong danh sách môn ${quizModalSubject} lớp ${quizModalGrade}:`;

                let warnBody = document.createElement('div');
                warnBody.className = 'small text-muted mb-2 font-monospace';
                warnBody.textContent = missingIds.join(', ');

                let btnRemove = document.createElement('button');
                btnRemove.type = 'button';
                btnRemove.className = 'btn btn-sm btn-outline-danger';
                btnRemove.id = 'btn-remove-missing-questions';
                btnRemove.textContent = '🗑️ Bỏ các câu không tìm thấy';
                btnRemove.onclick = () => {
                    missingIds.forEach(id => selectedQuizQuestionIds.delete(id));
                    updateSelectedQuizCount();
                    renderCandidateQuestions();
                };

                warnDiv.appendChild(warnHeader);
                warnDiv.appendChild(warnBody);
                warnDiv.appendChild(btnRemove);
                container.appendChild(warnDiv);
            }

            if (!Array.isArray(candidateQuestionsList) || candidateQuestionsList.length === 0) {
                let emptyEl = document.createElement('div');
                emptyEl.className = 'text-muted text-center py-2';
                emptyEl.textContent = 'Chưa có câu hỏi nào cho môn và lớp đã chọn. Hãy import hoặc tạo thêm câu hỏi.';
                container.appendChild(emptyEl);
                return;
            }

            candidateQuestionsList.forEach(q => {
                let row = document.createElement('div');
                row.className = 'form-check border-bottom py-2 d-flex align-items-start gap-2';

                let isChecked = selectedQuizQuestionIds.has(q.id);
                let isPub = q.status === 'PUBLISHED';

                let chk = document.createElement('input');
                chk.type = 'checkbox';
                chk.className = 'form-check-input mt-1';
                chk.id = 'chk-q-' + q.id;
                chk.checked = isChecked;
                chk.onchange = () => toggleCandidateQuestion(q.id, chk.checked);

                let label = document.createElement('label');
                label.className = 'form-check-label w-100 cursor-pointer';
                label.htmlFor = 'chk-q-' + q.id;

                let badgeClass = isPub ? 'badge-published' : 'badge-draft';
                let draftNotice = !isPub ? ' <span class="text-warning small fw-bold">(Bản nháp - cần xuất bản câu hỏi để xuất bản bộ đề)</span>' : '';

                label.innerHTML = `
                    <div class="d-flex justify-content-between align-items-center mb-1">
                        <div>
                            <code>${escapeHtml(q.code)}</code>
                            <span class="badge ${badgeClass} ms-1">${escapeHtml(q.status)}</span>
                            <span class="badge bg-light text-muted border ms-1">${escapeHtml(q.difficulty || 'MEDIUM')}</span>
                            ${draftNotice}
                        </div>
                    </div>
                    <div class="small text-dark">${escapeHtml(q.prompt)}</div>
                `;

                row.appendChild(chk);
                row.appendChild(label);
                container.appendChild(row);
            });
        }

        function toggleCandidateQuestion(qid, checked) {
            if (checked) {
                selectedQuizQuestionIds.add(qid);
            } else {
                selectedQuizQuestionIds.delete(qid);
            }
            updateSelectedQuizCount();
        }

        function updateSelectedQuizCount() {
            let el = document.getElementById('quiz-selected-count');
            if (el) el.textContent = String(selectedQuizQuestionIds.size);
        }

        async function saveQuiz(targetStatus) {
            if (isSubmittingQuiz) return;

            let errBox = document.getElementById('quiz-modal-error');
            if (errBox) errBox.style.display = 'none';

            if (isCandidateLoading) {
                let msg = 'Đang tải danh sách câu hỏi, vui lòng đợi hoàn tất trước khi lưu bộ đề!';
                if (errBox) { errBox.textContent = msg; errBox.style.display = 'block'; }
                else { alert(msg); }
                return;
            }

            let titleEl = document.getElementById('quiz-title');
            let title = titleEl ? titleEl.value.trim() : '';
            if (!title) {
                let msg = 'Vui lòng nhập tiêu đề bộ đề!';
                if (errBox) {
                    errBox.textContent = msg;
                    errBox.style.display = 'block';
                } else {
                    alert(msg);
                }
                return;
            }

            if (selectedQuizQuestionIds.size === 0) {
                let msg = 'Vui lòng chọn ít nhất 1 câu hỏi vào bộ đề!';
                if (errBox) {
                    errBox.textContent = msg;
                    errBox.style.display = 'block';
                } else {
                    alert(msg);
                }
                return;
            }

            // Check if there are missing IDs that do not belong to current candidate list
            const candidateIdMap = new Map();
            (candidateQuestionsList || []).forEach(q => candidateIdMap.set(q.id, q));
            const missingIds = [];
            selectedQuizQuestionIds.forEach(id => {
                if (!candidateIdMap.has(id)) {
                    missingIds.push(id);
                }
            });

            if (missingIds.length > 0) {
                let msg = `Bộ đề chứa ${missingIds.length} câu hỏi không có trong danh sách môn ${quizModalSubject} lớp ${quizModalGrade}. Vui lòng bấm "Bỏ các câu không tìm thấy" trước khi lưu!`;
                if (errBox) {
                    errBox.textContent = msg;
                    errBox.style.display = 'block';
                } else {
                    alert(msg);
                }
                return;
            }

            let quizId = document.getElementById('quiz-id') ? document.getElementById('quiz-id').value.trim() : '';
            let subj = quizModalSubject;
            let gr = parseInt(quizModalGrade, 10);
            let diff = document.getElementById('quiz-difficulty') ? document.getElementById('quiz-difficulty').value : 'MEDIUM';
            let chapter = document.getElementById('quiz-chapter') ? document.getElementById('quiz-chapter').value.trim() : null;
            let topic = document.getElementById('quiz-topic') ? document.getElementById('quiz-topic').value.trim() : null;

            let payload = {
                id: quizId || null,
                title: title,
                subjectId: subj,
                grade: gr,
                difficulty: diff,
                chapter: chapter || null,
                topic: topic || null,
                questionIds: Array.from(selectedQuizQuestionIds),
                status: targetStatus
            };

            let btnDraft = document.getElementById('btn-quiz-save-draft');
            let btnPublish = document.getElementById('btn-quiz-save-publish');

            isSubmittingQuiz = true;
            const currentGen = quizModalOpenGen;
            submittingModalGen = currentGen;

            setQuizFormDisabled(true);
            if (btnDraft) btnDraft.disabled = true;
            if (btnPublish) btnPublish.disabled = true;

            try {
                let res;
                try {
                    res = await fetch('/admin/quizzes', {
                        method: 'POST',
                        headers: {
                            'Authorization': 'Bearer ' + token,
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(payload)
                    });
                } catch (err) {
                    if (currentGen !== quizModalOpenGen) return;
                    let msg = 'Lỗi kết nối khi lưu bộ đề: ' + (err.message || 'Lỗi mạng');
                    if (errBox) {
                        errBox.textContent = msg;
                        errBox.style.display = 'block';
                    } else {
                        alert(msg);
                    }
                    return;
                }

                if (currentGen !== quizModalOpenGen) return;

                if (res.status === 401) {
                    handleLogout();
                    alert('Phiên làm việc đã hết hạn (HTTP 401). Vui lòng đăng nhập lại.');
                    return;
                }

                if (res.status === 403) {
                    let msg = 'Tài khoản của bạn không có quyền quản lý bộ đề (HTTP 403)!';
                    if (errBox) { errBox.textContent = msg; errBox.style.display = 'block'; }
                    else { alert(msg); }
                    return;
                }

                if (!res.ok) {
                    let errMsg;
                    try {
                        errMsg = await parseApiErrorMessage(res, 'Lỗi lưu bộ đề');
                    } catch(e) {
                        errMsg = 'Lỗi lưu bộ đề (HTTP ' + res.status + ')';
                    }

                    if (currentGen !== quizModalOpenGen) return;

                    if (errBox) {
                        errBox.textContent = errMsg;
                        errBox.style.display = 'block';
                    } else {
                        alert(errMsg);
                    }
                    return;
                }

                let savedQuiz;
                try {
                    savedQuiz = await res.json();
                } catch(e) {
                    if (currentGen !== quizModalOpenGen) return;
                    let msg = 'Máy chủ trả kết quả không hợp lệ hoặc không khớp yêu cầu. Chưa xác nhận được trạng thái lưu; hãy kiểm tra danh sách bộ đề trước khi thử lại.';
                    if (errBox) { errBox.textContent = msg; errBox.style.display = 'block'; }
                    else { alert(msg); }
                    return;
                }

                if (currentGen !== quizModalOpenGen) return;

                const isObj = (savedQuiz && typeof savedQuiz === 'object' && !Array.isArray(savedQuiz));
                const resId = (isObj && typeof savedQuiz.id === 'string') ? savedQuiz.id.trim() : '';
                const isIdValid = resId.length > 0;
                const isIdMatch = (!quizId || resId === quizId);
                const isStatusValid = (isObj && (savedQuiz.status === 'DRAFT' || savedQuiz.status === 'PUBLISHED'));
                const isStatusMatch = (isObj && savedQuiz.status === targetStatus);

                if (!isObj || !isIdValid || !isIdMatch || !isStatusValid || !isStatusMatch) {
                    let msg = 'Máy chủ trả kết quả không hợp lệ hoặc không khớp yêu cầu. Chưa xác nhận được trạng thái lưu; hãy kiểm tra danh sách bộ đề trước khi thử lại.';
                    if (errBox) { errBox.textContent = msg; errBox.style.display = 'block'; }
                    else { alert(msg); }
                    return;
                }

                if (quizModalObj) {
                    quizModalObj.hide();
                } else {
                    let modalEl = document.getElementById('quizModal');
                    if (modalEl) modalEl.style.display = 'none';
                }

                alert('Đã lưu thành công bộ đề: "' + escapeHtml(savedQuiz.title || title) + '"! Trạng thái: ' + (savedQuiz.status === 'PUBLISHED' ? 'Đã xuất bản' : 'Bản nháp'));
                loadQuizzes();
            } finally {
                if (submittingModalGen === currentGen) {
                    submittingModalGen = null;
                    isSubmittingQuiz = false;
                }
                if (currentGen === quizModalOpenGen) {
                    setQuizFormDisabled(false);
                    if (!isCandidateLoading) {
                        if (btnDraft) btnDraft.disabled = false;
                        if (btnPublish) btnPublish.disabled = false;
                    }
                }
            }
        }

        async function toggleQuizPublish(quizId, publish) {
            if (isPublishingQuiz) return;
            isPublishingQuiz = true;

            try {
                let res = await fetch('/admin/quizzes/' + encodeURIComponent(quizId) + '/publish?publish=' + publish, {
                    method: 'POST',
                    headers: { 'Authorization': 'Bearer ' + token }
                });

                if (res.status === 401) {
                    handleLogout();
                    alert('Phiên làm việc đã hết hạn (HTTP 401). Vui lòng đăng nhập lại.');
                    return;
                }

                if (res.status === 403) {
                    alert('Tài khoản của bạn không có quyền xuất bản bộ đề (HTTP 403)!');
                    return;
                }

                if (!res.ok) {
                    let errMsg = await parseApiErrorMessage(res, 'Không thể cập nhật trạng thái bộ đề');
                    alert(errMsg);
                    return;
                }

                loadQuizzes();
            } catch (err) {
                alert('Lỗi kết nối khi cập nhật trạng thái bộ đề: ' + (err.message || 'Lỗi mạng'));
            } finally {
                isPublishingQuiz = false;
            }
        }

        // =========================================================================
        // EXAM MANAGEMENT JAVASCRIPT LOGIC
        // =========================================================================

        function isExamModalSessionActive(session) {
            if (!session) return false;
            return (
                session.gen === examModalOpenGen &&
                session.gen === examModalActiveGen
            );
        }

        async function loadExams() {
            if (!token) return;
            let subj = document.getElementById('filter-exam-subject') ? document.getElementById('filter-exam-subject').value : '';
            let gr = document.getElementById('filter-exam-grade') ? document.getElementById('filter-exam-grade').value : '';
            let et = document.getElementById('filter-exam-type') ? document.getElementById('filter-exam-type').value : '';
            let sem = document.getElementById('filter-exam-semester') ? document.getElementById('filter-exam-semester').value : '';
            let st = document.getElementById('filter-exam-status') ? document.getElementById('filter-exam-status').value : '';

            let url = '/admin/exams?';
            if (subj) url += 'subject_id=' + encodeURIComponent(subj) + '&';
            if (gr) url += 'grade=' + encodeURIComponent(gr) + '&';
            if (et) url += 'exam_type=' + encodeURIComponent(et) + '&';
            if (sem) url += 'semester=' + encodeURIComponent(sem) + '&';
            if (st) url += 'status=' + encodeURIComponent(st) + '&';

            let container = document.getElementById('exams-list');
            if (!container) return;
            container.innerHTML = '<div class="text-center py-3 text-muted">Đang tải danh sách đề thi...</div>';

            try {
                let res = await fetch(url, {
                    headers: { 'Authorization': 'Bearer ' + token }
                });

                if (res.status === 401) {
                    handleLogout();
                    showAuthAlert('Phiên làm việc hết hạn. Vui lòng đăng nhập lại.');
                    return;
                }

                if (!res.ok) {
                    container.innerHTML = '<div class="alert alert-warning text-center">Không thể tải danh sách đề thi (HTTP ' + res.status + ').</div>';
                    return;
                }

                let exams = await res.json();
                container.innerHTML = '';

                if (!Array.isArray(exams) || exams.length === 0) {
                    container.innerHTML = '<div class="alert alert-secondary text-center">Chưa có đề thi nào phù hợp bộ lọc.</div>';
                    return;
                }

                exams.forEach(ex => {
                    let card = document.createElement('div');
                    card.className = 'card p-3 mb-3';
                    let isPub = ex.status === 'PUBLISHED';
                    let examTypeLabel = ex.examType === 'MIDTERM' ? 'Giữa kỳ' : 'Cuối kỳ';
                    let semesterLabel = ex.semester === 1 ? 'HK1' : 'HK2';

                    let headerRow = document.createElement('div');
                    headerRow.className = 'd-flex justify-content-between align-items-center mb-2';

                    let badgeGroup = document.createElement('div');
                    badgeGroup.innerHTML = `
                        <span class="badge bg-secondary">${escapeHtml(ex.subjectId).toUpperCase()}</span>
                        <span class="badge bg-dark">Lớp ${ex.grade}</span>
                        <span class="badge bg-info text-dark">${examTypeLabel} (${semesterLabel})</span>
                        <span class="badge ${isPub ? 'badge-published' : 'badge-draft'}">${escapeHtml(ex.status)}</span>
                        <span class="badge bg-light text-dark border">⏱️ ${ex.durationMinutes} phút</span>
                        <span class="badge bg-primary">📝 ${ex.questionCount} câu (${ex.totalPoints} đ)</span>
                        <small class="text-muted ms-2">v${ex.version || 1}</small>
                    `;

                    let actionGroup = document.createElement('div');
                    actionGroup.className = 'd-flex align-items-center gap-2';

                    let btnEdit = document.createElement('button');
                    btnEdit.className = 'btn btn-sm btn-outline-primary';
                    btnEdit.textContent = '✏️ Sửa';
                    btnEdit.addEventListener('click', () => openExamModal(ex));

                    let btnToggle = document.createElement('button');
                    btnToggle.className = `btn btn-sm ${isPub ? 'btn-outline-warning' : 'btn-outline-success'}`;
                    btnToggle.textContent = isPub ? 'Ẩn bản nháp' : '🚀 Xuất bản';
                    btnToggle.addEventListener('click', () => toggleExamPublish(ex.id, !isPub));

                    actionGroup.appendChild(btnEdit);
                    actionGroup.appendChild(btnToggle);

                    headerRow.appendChild(badgeGroup);
                    headerRow.appendChild(actionGroup);

                    let titleEl = document.createElement('h5');
                    titleEl.className = 'fw-bold mb-1';
                    titleEl.textContent = ex.title;

                    card.appendChild(headerRow);
                    card.appendChild(titleEl);
                    container.appendChild(card);
                });
            } catch(e) {
                container.innerHTML = '<div class="alert alert-danger text-center">Lỗi kết nối khi tải danh sách đề thi.</div>';
            }
        }

        async function openExamModal(examData) {
            examModalOpenGen++;
            examModalActiveGen = examModalOpenGen;
            examModalClosingGen = 0;
            examCandidateLoadSeq++;
            isSubmittingExam = false;
            activeExamSaveOwner = null;

            let btnDraft = document.getElementById('btn-exam-save-draft');
            let btnPub = document.getElementById('btn-exam-save-publish');
            if (btnDraft) { btnDraft.disabled = false; btnDraft.textContent = '💾 Lưu bản nháp (Draft)'; }
            if (btnPub) { btnPub.disabled = false; btnPub.textContent = '🚀 Xuất bản ngay (Publish)'; }

            let errBox = document.getElementById('exam-modal-error');
            if (errBox) errBox.style.display = 'none';

            let titleEl = document.getElementById('exam-modal-title');
            let idEl = document.getElementById('exam-id');
            let verEl = document.getElementById('exam-version');
            let nameEl = document.getElementById('exam-title');
            let subjEl = document.getElementById('exam-subject');
            let grEl = document.getElementById('exam-grade');
            let typeEl = document.getElementById('exam-type');
            let semEl = document.getElementById('exam-semester');
            let durEl = document.getElementById('exam-duration');

            if (examData) {
                if (titleEl) titleEl.textContent = `Chỉnh sửa Đề thi [v${examData.version || 1}]`;
                if (idEl) idEl.value = examData.id || '';
                if (verEl) verEl.value = examData.version != null ? examData.version : 1;
                if (nameEl) nameEl.value = examData.title || '';
                examModalSubject = examData.subjectId || 'physics';
                examModalGrade = String(examData.grade || 10);
                if (typeEl) typeEl.value = examData.examType || 'MIDTERM';
                if (semEl) semEl.value = String(examData.semester || 1);
                if (durEl) durEl.value = examData.durationMinutes || 45;
                selectedExamQuestionIds = new Set(examData.questionIds || []);
                examModalActiveId = examData.id || '';
            } else {
                if (titleEl) titleEl.textContent = 'Tạo Đề thi mới';
                if (idEl) idEl.value = '';
                if (verEl) verEl.value = '1';
                if (nameEl) nameEl.value = '';
                examModalSubject = 'physics';
                examModalGrade = '10';
                if (typeEl) typeEl.value = 'MIDTERM';
                if (semEl) semEl.value = '1';
                if (durEl) durEl.value = 45;
                selectedExamQuestionIds = new Set();
                examModalActiveId = '';
            }

            if (subjEl) subjEl.value = examModalSubject;
            if (grEl) grEl.value = examModalGrade;

            candidateExamQuestionsList = [];
            updateSelectedExamCount();

            if (examModalObj) {
                examModalObj.show();
            } else {
                let modalEl = document.getElementById('examModal');
                if (modalEl) modalEl.style.display = 'block';
            }

            await loadExamCandidateQuestions(examModalSubject, examModalGrade, selectedExamQuestionIds);
        }

        function closeExamModal() {
            examModalActiveGen = 0;
            examModalActiveId = null;
            if (examModalObj) {
                examModalObj.hide();
            } else {
                let modalEl = document.getElementById('examModal');
                if (modalEl) modalEl.style.display = 'none';
            }
        }

        async function handleExamSubjectGradeChange() {
            let subjEl = document.getElementById('exam-subject');
            let grEl = document.getElementById('exam-grade');
            let newSubj = subjEl ? subjEl.value : 'physics';
            let newGr = grEl ? grEl.value : '10';

            if (newSubj === examModalSubject && newGr === examModalGrade) return;

            if (selectedExamQuestionIds.size > 0) {
                if (!confirm('Thay đổi Môn hoặc Khối lớp sẽ bỏ chọn các câu hỏi hiện tại. Bạn có chắc chắn muốn đổi không?')) {
                    if (subjEl) subjEl.value = examModalSubject;
                    if (grEl) grEl.value = examModalGrade;
                    return;
                }
                selectedExamQuestionIds.clear();
            }

            examModalSubject = newSubj;
            examModalGrade = newGr;
            updateSelectedExamCount();
            await loadExamCandidateQuestions(examModalSubject, examModalGrade, selectedExamQuestionIds);
        }

        async function loadExamCandidateQuestions(subj, gr, preselectedIds) {
            let container = document.getElementById('exam-candidate-questions');
            if (!container) return;

            examCandidateLoadSeq++;
            const thisSeq = examCandidateLoadSeq;
            isCandidateExamLoading = true;
            container.innerHTML = '<div class="text-center py-2 text-muted">Đang tải câu hỏi trong kho...</div>';

            try {
                let res = await fetch(`/admin/questions?subject_id=${encodeURIComponent(subj)}&grade=${encodeURIComponent(gr)}&status=PUBLISHED`, {
                    headers: { 'Authorization': 'Bearer ' + token }
                });

                if (thisSeq !== examCandidateLoadSeq || examModalActiveGen === 0) return;

                if (!res.ok) {
                    container.innerHTML = '<div class="text-danger text-center py-2">Không thể tải danh sách câu hỏi ứng viên (HTTP ' + res.status + ').</div>';
                    return;
                }

                let questions = await res.json();
                if (thisSeq !== examCandidateLoadSeq || examModalActiveGen === 0) return;

                candidateExamQuestionsList = Array.isArray(questions) ? questions : [];
                container.innerHTML = '';

                if (candidateExamQuestionsList.length === 0) {
                    container.innerHTML = '<div class="text-muted text-center py-2">Chưa có câu hỏi ĐÃ XUẤT BẢN nào cho môn và lớp này. Vui lòng xuất bản câu hỏi trước khi tạo đề.</div>';
                    updateSelectedExamCount();
                    return;
                }

                candidateExamQuestionsList.forEach(q => {
                    let div = document.createElement('div');
                    div.className = 'form-check border-bottom py-2';

                    let chk = document.createElement('input');
                    chk.type = 'checkbox';
                    chk.className = 'form-check-input';
                    chk.id = 'chk-exam-q-' + q.id;
                    chk.value = q.id;
                    chk.checked = preselectedIds.has(q.id);

                    chk.addEventListener('change', () => {
                        if (chk.checked) selectedExamQuestionIds.add(q.id);
                        else selectedExamQuestionIds.delete(q.id);
                        updateSelectedExamCount();
                    });

                    let label = document.createElement('label');
                    label.className = 'form-check-label d-block';
                    label.htmlFor = chk.id;
                    label.innerHTML = `
                        <div class="d-flex justify-content-between">
                            <strong>${escapeHtml(q.code || q.id)}</strong>
                            <span class="badge bg-light text-dark border">${q.points || 10} điểm</span>
                        </div>
                        <div class="text-muted small text-truncate">${escapeHtml(q.prompt)}</div>
                    `;

                    div.appendChild(chk);
                    div.appendChild(label);
                    container.appendChild(div);
                });

                updateSelectedExamCount();
            } catch(e) {
                if (thisSeq === examCandidateLoadSeq && examModalActiveGen !== 0) {
                    container.innerHTML = '<div class="text-danger text-center py-2">Lỗi kết nối khi tải danh sách câu hỏi.</div>';
                }
            } finally {
                if (thisSeq === examCandidateLoadSeq) {
                    isCandidateExamLoading = false;
                }
            }
        }

        function updateSelectedExamCount() {
            let countEl = document.getElementById('exam-selected-count');
            let pointsEl = document.getElementById('exam-selected-points');
            if (countEl) countEl.textContent = selectedExamQuestionIds.size;

            if (pointsEl) {
                let totalPts = 0;
                candidateExamQuestionsList.forEach(q => {
                    if (selectedExamQuestionIds.has(q.id)) {
                        totalPts += (q.points || 10);
                    }
                });
                pointsEl.textContent = totalPts;
            }
        }

        async function saveExam(targetStatus) {
            currentExamSaveId++;
            const thisSaveId = currentExamSaveId;
            const currentGen = examModalOpenGen;

            if (activeExamSaveOwner !== null) return;
            if (examModalActiveGen === 0 || currentGen !== examModalActiveGen) return;

            let errBox = document.getElementById('exam-modal-error');
            if (errBox) errBox.style.display = 'none';

            let titleEl = document.getElementById('exam-title');
            let title = titleEl ? titleEl.value.trim() : '';
            if (!title) {
                let msg = 'Vui lòng nhập tiêu đề đề thi!';
                if (errBox) { errBox.textContent = msg; errBox.style.display = 'block'; }
                else { alert(msg); }
                return;
            }

            if (selectedExamQuestionIds.size === 0) {
                let msg = 'Vui lòng chọn ít nhất 1 câu hỏi cho đề thi!';
                if (errBox) { errBox.textContent = msg; errBox.style.display = 'block'; }
                else { alert(msg); }
                return;
            }

            let durEl = document.getElementById('exam-duration');
            let duration = durEl ? parseInt(durEl.value, 10) : 45;
            if (isNaN(duration) || duration < 5 || duration > 300) {
                let msg = 'Thời lượng làm bài phải từ 5 đến 300 phút!';
                if (errBox) { errBox.textContent = msg; errBox.style.display = 'block'; }
                else { alert(msg); }
                return;
            }

            let idEl = document.getElementById('exam-id');
            let verEl = document.getElementById('exam-version');
            let subjEl = document.getElementById('exam-subject');
            let grEl = document.getElementById('exam-grade');
            let typeEl = document.getElementById('exam-type');
            let semEl = document.getElementById('exam-semester');

            let examId = idEl ? idEl.value : '';
            let expectedVer = (verEl && verEl.value) ? parseInt(verEl.value, 10) : null;

            let payload = {
                id: examId || null,
                title: title,
                subjectId: subjEl ? subjEl.value : 'physics',
                grade: grEl ? parseInt(grEl.value, 10) : 10,
                examType: typeEl ? typeEl.value : 'MIDTERM',
                semester: semEl ? parseInt(semEl.value, 10) : 1,
                durationMinutes: duration,
                questionIds: Array.from(selectedExamQuestionIds),
                status: targetStatus,
                expectedVersion: expectedVer
            };

            const saveSession = {
                saveId: thisSaveId,
                gen: currentGen,
                examId: examId,
                expectedVer: expectedVer
            };

            activeExamSaveOwner = saveSession;
            let btnDraft = document.getElementById('btn-exam-save-draft');
            let btnPub = document.getElementById('btn-exam-save-publish');
            if (btnDraft) { btnDraft.disabled = true; btnDraft.textContent = '⏳ Đang lưu...'; }
            if (btnPub) { btnPub.disabled = true; btnPub.textContent = '⏳ Đang lưu...'; }

            try {
                let res;
                try {
                    res = await fetch('/admin/exams', {
                        method: 'POST',
                        headers: {
                            'Authorization': 'Bearer ' + token,
                            'Content-Type': 'application/json',
                            'If-Match': expectedVer != null ? `"${expectedVer}"` : ''
                        },
                        body: JSON.stringify(payload)
                    });
                } catch(netErr) {
                    if (!isExamModalSessionActive(saveSession)) return;
                    if (errBox) {
                        errBox.textContent = 'Lỗi kết nối mạng: Không thể lưu đề thi. Hãy thử lại.';
                        errBox.style.display = 'block';
                    }
                    return;
                }

                if (!isExamModalSessionActive(saveSession)) return;

                if (res.status === 401) {
                    handleLogout();
                    alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
                    return;
                }

                if (res.status === 403) {
                    if (errBox) {
                        errBox.textContent = 'Tài khoản không có quyền tạo/chỉnh sửa đề thi (HTTP 403).';
                        errBox.style.display = 'block';
                    }
                    return;
                }

                if (!res.ok) {
                    let errMsg;
                    try {
                        errMsg = await parseApiErrorMessage(res, 'Lỗi lưu đề thi');
                    } catch(e) {
                        errMsg = 'Lỗi lưu đề thi (HTTP ' + res.status + ')';
                    }

                    if (!isExamModalSessionActive(saveSession)) return;
                    if (errBox) {
                        errBox.textContent = errMsg;
                        errBox.style.display = 'block';
                    }
                    return;
                }

                let updatedExam;
                try {
                    updatedExam = await res.json();
                } catch(jsonErr) {
                    if (!isExamModalSessionActive(saveSession)) return;
                    if (errBox) {
                        errBox.textContent = 'Máy chủ trả dữ liệu JSON không hợp lệ.';
                        errBox.style.display = 'block';
                    }
                    return;
                }

                if (!isExamModalSessionActive(saveSession)) return;

                if (!updatedExam || typeof updatedExam !== 'object' || !updatedExam.id || updatedExam.version == null) {
                    if (errBox) {
                        errBox.textContent = 'Phản hồi từ máy chủ không hợp lệ.';
                        errBox.style.display = 'block';
                    }
                    return;
                }

                closeExamModal();
                alert(`Đã lưu thành công đề thi [${updatedExam.title}] (${updatedExam.status === 'PUBLISHED' ? 'Đã xuất bản' : 'Bản nháp'}, v${updatedExam.version})!`);
                await loadExams();

            } catch(err) {
                if (!isExamModalSessionActive(saveSession)) return;
                if (errBox) {
                    errBox.textContent = 'Lỗi không xác định: ' + (err.message || 'Lỗi thao tác');
                    errBox.style.display = 'block';
                }
            } finally {
                if (activeExamSaveOwner && activeExamSaveOwner.saveId === thisSaveId) {
                    activeExamSaveOwner = null;
                    let curDraft = document.getElementById('btn-exam-save-draft');
                    let curPub = document.getElementById('btn-exam-save-publish');
                    if (curDraft) { curDraft.disabled = false; curDraft.textContent = '💾 Lưu bản nháp (Draft)'; }
                    if (curPub) { curPub.disabled = false; curPub.textContent = '🚀 Xuất bản ngay (Publish)'; }
                }
            }
        }

        async function toggleExamPublish(examId, publish) {
            try {
                let res = await fetch(`/admin/exams/${examId}/publish?publish=${publish}`, {
                    method: 'POST',
                    headers: { 'Authorization': 'Bearer ' + token }
                });
                if (!res.ok) {
                    let errMsg = await parseApiErrorMessage(res, 'Lỗi cập nhật trạng thái xuất bản đề thi');
                    alert(errMsg);
                    return;
                }
                loadExams();
            } catch(e) {
                alert('Lỗi kết nối khi cập nhật trạng thái xuất bản đề thi');
            }
        }
    </script>
</body>
</html>
"""
