-- ==============================================================================
-- AI Mentor - Bản Thảo Tham Khảo Cấu Trúc Bảng PostgreSQL cho Supabase
-- Tệp: docs/backend/supabase_schema_reference.sql
-- ==============================================================================
-- CẢNH BÁO KỸ THUẬT QUAN TRỌNG:
-- ĐÂY LÀ BẢN THẢO THAM KHẢO CẤU TRÚC CHƯA QUA KIỂM CHỨNG (UNVALIDATED DRAFT REFERENCE).
-- Tệp DDL này CHƯA TỪNG được kiểm chứng trên một phiên bản PostgreSQL hoặc dự án
-- Supabase thực tế nào.
-- TUYỆT ĐỐI KHÔNG TỰ Ý CHẠY (MUST NOT BE RUN) trên bất kỳ cơ sở dữ liệu nào khi chưa
-- có môi trường PostgreSQL thật và chưa được người dùng phê duyệt chính thức.
--
-- Mục đích: Minh họa sơ bộ tương quan cấu trúc bảng, kiểu dữ liệu và ràng buộc đối ứng
-- từ 26 bảng hiện tại của SQLite (aimentor.sqlite3) để làm căn cứ kỹ thuật cho tương lai.
--
-- ĐIỀU KIỆN TIÊN QUYẾT TRƯỚC KHI THỰC THI TRONG TƯƠNG LAI:
-- 1. Phải có dự án Supabase / PostgreSQL thật được tạo và phê duyệt.
-- 2. Phải kiểm chứng cú pháp DDL, ràng buộc khóa ngoại và kiểu dữ liệu trên PostgreSQL thật.
-- 3. Phải rà soát phân quyền an toàn (Security & RLS Review) để đảm bảo không lộ Data API.
-- 4. Phải xây dựng công cụ di trú chuyên nghiệp (như Alembic) thay vì chạy DDL thủ công.
-- ==============================================================================

-- [DRAFT / UNVALIDATED] Extension hỗ trợ so khớp chuỗi không phân biệt hoa thường
CREATE EXTENSION IF NOT EXISTS citext;

-- [DRAFT / UNVALIDATED] Khuyến nghị phân vùng schema riêng biệt cho ứng dụng
-- CREATE SCHEMA IF NOT EXISTS aimentor;
-- SET search_path TO aimentor, public;

-- ------------------------------------------------------------------------------
-- 1. [DRAFT] Bảng users (Tài khoản người dùng)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username CITEXT NOT NULL UNIQUE,
    email CITEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    nickname TEXT NOT NULL,
    bio TEXT NOT NULL DEFAULT '',
    avatar_id TEXT,
    grade INTEGER NOT NULL DEFAULT 0,
    verified INTEGER NOT NULL DEFAULT 0,
    revision INTEGER NOT NULL DEFAULT 1,
    created_at DOUBLE PRECISION NOT NULL,
    updated_at DOUBLE PRECISION NOT NULL,
    rank_tier TEXT NOT NULL DEFAULT 'BRONZE',
    qr_code_token TEXT,
    role TEXT NOT NULL DEFAULT 'STUDENT'
);

-- ------------------------------------------------------------------------------
-- 2. [DRAFT] Bảng sessions (Phiên đăng nhập người dùng)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sessions (
    id TEXT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_hash TEXT NOT NULL UNIQUE,
    refresh_hash TEXT NOT NULL UNIQUE,
    access_expires DOUBLE PRECISION NOT NULL,
    refresh_expires DOUBLE PRECISION NOT NULL,
    revoked INTEGER NOT NULL DEFAULT 0
);

-- ------------------------------------------------------------------------------
-- 3. [DRAFT] Bảng resources (Tài nguyên: Chat, Dự án, Tin nhắn, Metadata tệp đính kèm)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resources (
    id TEXT PRIMARY KEY,
    kind TEXT NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    revision INTEGER NOT NULL DEFAULT 1,
    updated_at DOUBLE PRECISION NOT NULL,
    deleted_at DOUBLE PRECISION
);
CREATE INDEX IF NOT EXISTS resource_owner_kind ON resources(user_id, kind, updated_at);

-- ------------------------------------------------------------------------------
-- 4. [DRAFT] Bảng enrollments (Đăng ký môn học theo khối lớp)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS enrollments (
    id TEXT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id TEXT NOT NULL,
    grade INTEGER NOT NULL CHECK(grade BETWEEN 10 AND 12),
    curriculum_version TEXT NOT NULL,
    placement TEXT,
    revision INTEGER NOT NULL DEFAULT 1,
    updated_at DOUBLE PRECISION NOT NULL,
    UNIQUE(user_id, subject_id, grade)
);

-- ------------------------------------------------------------------------------
-- 5. [DRAFT] Bảng attempts (Phiên làm bài học / placement)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attempts (
    id TEXT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    enrollment_id TEXT NOT NULL REFERENCES enrollments(id),
    unit_id TEXT NOT NULL,
    kind TEXT NOT NULL,
    curriculum_version TEXT NOT NULL,
    step INTEGER NOT NULL DEFAULT 0,
    answers TEXT NOT NULL DEFAULT '[]',
    state TEXT NOT NULL DEFAULT 'IN_PROGRESS',
    revision INTEGER NOT NULL DEFAULT 1,
    updated_at DOUBLE PRECISION NOT NULL,
    unit_snapshot TEXT
);

-- ------------------------------------------------------------------------------
-- 6. [DRAFT] Bảng completions (Kết quả hoàn thành bài học và điểm thưởng)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS completions (
    id TEXT PRIMARY KEY,
    attempt_id TEXT NOT NULL UNIQUE REFERENCES attempts(id),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    xp INTEGER NOT NULL,
    day TEXT NOT NULL,
    result TEXT NOT NULL,
    completed_at DOUBLE PRECISION NOT NULL
);

-- ------------------------------------------------------------------------------
-- 7. [DRAFT] Bảng awards (Phần thưởng tích lũy theo môn và bài học)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS awards (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id TEXT NOT NULL,
    grade INTEGER NOT NULL,
    reward_unit_id TEXT NOT NULL,
    xp INTEGER NOT NULL,
    day TEXT NOT NULL,
    created_at DOUBLE PRECISION NOT NULL,
    PRIMARY KEY(user_id, subject_id, grade, reward_unit_id)
);

-- ------------------------------------------------------------------------------
-- 8. [DRAFT] Bảng learning_days (Ghi nhận chuỗi ngày học)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learning_days (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day TEXT NOT NULL,
    PRIMARY KEY(user_id, day)
);

-- ------------------------------------------------------------------------------
-- 9. [DRAFT] Bảng friendships (Quan hệ bạn bè)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS friendships (
    id TEXT PRIMARY KEY,
    requester BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    state TEXT NOT NULL,
    revision INTEGER NOT NULL DEFAULT 1,
    updated_at DOUBLE PRECISION NOT NULL,
    CHECK(requester <> recipient),
    UNIQUE(requester, recipient)
);

-- ------------------------------------------------------------------------------
-- 10. [DRAFT] Bảng events (Sự kiện đồng bộ realtime / SSE)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
    seq BIGSERIAL PRIMARY KEY,
    owner BIGINT,
    resource_type TEXT NOT NULL,
    resource_id TEXT NOT NULL,
    revision INTEGER NOT NULL,
    operation TEXT NOT NULL,
    created_at DOUBLE PRECISION NOT NULL
);

-- ------------------------------------------------------------------------------
-- 11. [DRAFT] Bảng idempotency (Khóa chống gửi trùng lặp tác vụ)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS idempotency (
    owner BIGINT NOT NULL,
    operation TEXT NOT NULL,
    key TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    response TEXT NOT NULL,
    PRIMARY KEY(owner, operation, key)
);

-- ------------------------------------------------------------------------------
-- 12. [DRAFT] Bảng recovery_tokens (Token đặt lại mật khẩu)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recovery_tokens (
    token_hash TEXT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose TEXT NOT NULL,
    expires_at DOUBLE PRECISION NOT NULL,
    consumed INTEGER NOT NULL DEFAULT 0
);

-- ------------------------------------------------------------------------------
-- 13. [DRAFT] Bảng rate_limits (Giới hạn tần suất gọi API)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rate_limits (
    bucket TEXT NOT NULL,
    slot BIGINT NOT NULL,
    count INTEGER NOT NULL,
    PRIMARY KEY(bucket, slot)
);

-- ------------------------------------------------------------------------------
-- 14. [DRAFT] Bảng badges (Danh mục huy hiệu)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS badges (
    id TEXT PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    icon_url TEXT NOT NULL,
    month_period TEXT NOT NULL,
    created_at DOUBLE PRECISION NOT NULL
);

-- ------------------------------------------------------------------------------
-- 15. [DRAFT] Bảng user_badges (Huy hiệu người dùng đã mở khóa)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_badges (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id TEXT NOT NULL REFERENCES badges(id),
    unlocked_at DOUBLE PRECISION NOT NULL,
    PRIMARY KEY(user_id, badge_id)
);

-- ------------------------------------------------------------------------------
-- 16. [DRAFT] Bảng friend_streaks (Chuỗi ngày cùng học với bạn bè)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS friend_streaks (
    id TEXT PRIMARY KEY,
    user1_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    streak_days INTEGER NOT NULL DEFAULT 0,
    last_shared_day TEXT NOT NULL,
    updated_at DOUBLE PRECISION NOT NULL,
    UNIQUE(user1_id, user2_id)
);

-- ------------------------------------------------------------------------------
-- 17. [DRAFT] Bảng questions (Ngân hàng câu hỏi trắc nghiệm)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS questions (
    id TEXT PRIMARY KEY,
    code TEXT UNIQUE,
    version INTEGER NOT NULL DEFAULT 1,
    subject_id TEXT NOT NULL,
    grade INTEGER NOT NULL CHECK(grade BETWEEN 10 AND 12),
    chapter TEXT NOT NULL,
    topic TEXT NOT NULL,
    textbook TEXT,
    prompt TEXT NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_option TEXT NOT NULL,
    explanation TEXT NOT NULL,
    difficulty TEXT NOT NULL DEFAULT 'MEDIUM',
    points INTEGER NOT NULL DEFAULT 10,
    image_url TEXT,
    source TEXT,
    author TEXT,
    license TEXT,
    status TEXT NOT NULL DEFAULT 'PUBLISHED',
    created_by BIGINT REFERENCES users(id),
    created_at DOUBLE PRECISION NOT NULL,
    updated_at DOUBLE PRECISION NOT NULL
);

-- ------------------------------------------------------------------------------
-- 18. [DRAFT] Bảng library_quizzes (Bộ đề ôn tập thư viện câu hỏi)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS library_quizzes (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    subject_id TEXT NOT NULL,
    grade INTEGER NOT NULL CHECK(grade BETWEEN 10 AND 12),
    chapter TEXT,
    topic TEXT,
    difficulty TEXT NOT NULL DEFAULT 'MEDIUM',
    question_ids TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PUBLISHED',
    created_at DOUBLE PRECISION NOT NULL
);

-- ------------------------------------------------------------------------------
-- 19. [DRAFT] Bảng practice_sessions (Phiên luyện tập đề thi thư viện)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS practice_sessions (
    id TEXT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quiz_id TEXT NOT NULL,
    subject_id TEXT NOT NULL,
    grade INTEGER NOT NULL,
    question_snapshots TEXT NOT NULL,
    step INTEGER NOT NULL DEFAULT 0,
    answers TEXT NOT NULL DEFAULT '{}',
    state TEXT NOT NULL DEFAULT 'IN_PROGRESS',
    score INTEGER NOT NULL DEFAULT 0,
    correct_count INTEGER NOT NULL DEFAULT 0,
    total_count INTEGER NOT NULL DEFAULT 0,
    created_at DOUBLE PRECISION NOT NULL,
    updated_at DOUBLE PRECISION NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_practice_sessions_user_quiz ON practice_sessions(user_id, quiz_id, state, updated_at);

-- ------------------------------------------------------------------------------
-- 20. [DRAFT] Bảng wrong_questions_notebook (Sổ tay câu hỏi sai)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wrong_questions_notebook (
    id TEXT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id TEXT NOT NULL,
    subject_id TEXT NOT NULL,
    grade INTEGER NOT NULL,
    selected_option TEXT NOT NULL,
    correct_option TEXT NOT NULL,
    explanation TEXT NOT NULL,
    question_snapshot TEXT,
    state TEXT NOT NULL DEFAULT 'NEEDS_REVIEW',
    created_at DOUBLE PRECISION NOT NULL,
    updated_at DOUBLE PRECISION,
    source TEXT NOT NULL DEFAULT 'LIBRARY',
    UNIQUE(user_id, question_id)
);
CREATE INDEX IF NOT EXISTS idx_wrong_questions_user_subj ON wrong_questions_notebook(user_id, subject_id, grade, state);

-- ------------------------------------------------------------------------------
-- 21. [DRAFT] Bảng role_audit_logs (Nhật ký phân quyền tài khoản)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role_audit_logs (
    id TEXT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    previous_role TEXT NOT NULL,
    new_role TEXT NOT NULL,
    source TEXT NOT NULL DEFAULT 'CLI_MANAGE_ROLES',
    changed_at DOUBLE PRECISION NOT NULL
);

-- ------------------------------------------------------------------------------
-- 22. [DRAFT] Bảng exams (Đề thi giữa kỳ, cuối kỳ)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exams (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    subject_id TEXT NOT NULL,
    grade INTEGER NOT NULL CHECK(grade BETWEEN 10 AND 12),
    exam_type TEXT NOT NULL CHECK(exam_type IN ('MIDTERM', 'FINAL')),
    semester INTEGER NOT NULL CHECK(semester IN (1, 2)),
    duration_minutes INTEGER NOT NULL CHECK(duration_minutes BETWEEN 5 AND 300),
    question_ids TEXT NOT NULL,
    total_points INTEGER NOT NULL DEFAULT 0,
    question_count INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT', 'PUBLISHED')),
    version INTEGER NOT NULL DEFAULT 1,
    created_by BIGINT REFERENCES users(id),
    created_at DOUBLE PRECISION NOT NULL,
    updated_at DOUBLE PRECISION NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_exams_filter ON exams(subject_id, grade, exam_type, semester, status);

-- ------------------------------------------------------------------------------
-- 23. [DRAFT] Bảng exam_sessions (Phiên làm bài thi khảo thí)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exam_sessions (
    id TEXT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_id TEXT NOT NULL REFERENCES exams(id),
    subject_id TEXT NOT NULL,
    grade INTEGER NOT NULL,
    exam_title TEXT NOT NULL,
    exam_type TEXT NOT NULL,
    semester INTEGER NOT NULL,
    duration_minutes INTEGER NOT NULL,
    question_snapshots TEXT NOT NULL,
    answers TEXT NOT NULL DEFAULT '{}',
    state TEXT NOT NULL DEFAULT 'IN_PROGRESS' CHECK(state IN ('IN_PROGRESS', 'SUBMITTED', 'TIMED_OUT')),
    score DOUBLE PRECISION NOT NULL DEFAULT 0,
    score_10 DOUBLE PRECISION NOT NULL DEFAULT 0,
    raw_score INTEGER NOT NULL DEFAULT 0,
    total_points INTEGER NOT NULL DEFAULT 0,
    correct_count INTEGER NOT NULL DEFAULT 0,
    total_count INTEGER NOT NULL DEFAULT 0,
    started_at DOUBLE PRECISION NOT NULL,
    deadline_at DOUBLE PRECISION NOT NULL,
    submitted_at DOUBLE PRECISION,
    revision INTEGER NOT NULL DEFAULT 1,
    created_at DOUBLE PRECISION NOT NULL,
    updated_at DOUBLE PRECISION NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_exam_sessions_user ON exam_sessions(user_id, exam_id, state, started_at);

-- ------------------------------------------------------------------------------
-- 24. [DRAFT] Bảng ai_practice_sessions (Phiên luyện tập với AI)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_practice_sessions (
    id TEXT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id TEXT NOT NULL,
    grade INTEGER NOT NULL CHECK(grade BETWEEN 10 AND 12),
    topic TEXT NOT NULL,
    difficulty TEXT NOT NULL CHECK(difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    question_count INTEGER NOT NULL,
    questions_snapshot TEXT NOT NULL,
    answers TEXT NOT NULL DEFAULT '{}',
    current_index INTEGER NOT NULL DEFAULT 0,
    state TEXT NOT NULL DEFAULT 'IN_PROGRESS' CHECK(state IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    summary TEXT,
    created_at DOUBLE PRECISION NOT NULL,
    updated_at DOUBLE PRECISION NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ai_practice_user_state ON ai_practice_sessions(user_id, state, created_at);

-- ------------------------------------------------------------------------------
-- 25. [DRAFT] Bảng ai_question_reports (Báo cáo phản hồi câu hỏi AI)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_question_reports (
    id TEXT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id TEXT NOT NULL REFERENCES ai_practice_sessions(id) ON DELETE CASCADE,
    question_id TEXT NOT NULL,
    reason TEXT NOT NULL,
    details TEXT,
    created_at DOUBLE PRECISION NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ai_reports_user ON ai_question_reports(user_id, created_at);

-- ------------------------------------------------------------------------------
-- 26. [DRAFT] Bảng ai_provider_operations (Kiểm soát hạn mức và idempotency gọi AI)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_provider_operations (
    id TEXT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    operation TEXT NOT NULL,
    operation_key TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    state TEXT NOT NULL,
    created_at DOUBLE PRECISION NOT NULL,
    UNIQUE(user_id, operation, operation_key)
);
CREATE INDEX IF NOT EXISTS idx_ai_provider_usage ON ai_provider_operations(user_id, operation, created_at);

-- ------------------------------------------------------------------------------
-- [DRAFT / UNVALIDATED] KHUYẾN NGHỊ RLS & BẢO MẬT (NẾU ĐẶT TRÊN SCHEMA PUBLIC)
-- ------------------------------------------------------------------------------
-- Đoạn mã tham khảo để khóa Data API tự động của Supabase (PostgREST) khi cần thiết:
-- DO $$
-- DECLARE
--     tbl text;
--     tbls text[] := ARRAY[
--         'users', 'sessions', 'resources', 'enrollments', 'attempts', 'completions',
--         'awards', 'learning_days', 'friendships', 'events', 'idempotency', 'recovery_tokens',
--         'rate_limits', 'badges', 'user_badges', 'friend_streaks', 'questions',
--         'library_quizzes', 'practice_sessions', 'wrong_questions_notebook', 'role_audit_logs',
--         'exams', 'exam_sessions', 'ai_practice_sessions', 'ai_question_reports', 'ai_provider_operations'
--     ];
-- BEGIN
--     FOREACH tbl IN ARRAY tbls LOOP
--         EXECUTE format('ALTER TABLE IF EXISTS %I ENABLE ROW LEVEL SECURITY;', tbl);
--         EXECUTE format('REVOKE ALL ON TABLE %I FROM anon, authenticated;', tbl);
--     END LOOP;
-- END $$;
