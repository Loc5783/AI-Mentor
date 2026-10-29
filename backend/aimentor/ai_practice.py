import hashlib
import json
import logging
import os
import re
import time
import urllib.error
import urllib.request
import uuid
from abc import ABC, abstractmethod
from typing import Any

from fastapi import HTTPException
from aimentor.database import Database, dump

logger = logging.getLogger("aimentor.ai_practice")

VALID_SUBJECTS = {
    "math", "physics", "chemistry", "biology", "literature",
    "history", "geography", "english", "economics_law"
}

SUBJECT_ALIASES = {
    "civics": "economics_law",
    "economics-law": "economics_law",
}

SUBJECT_NAMES_VI = {
    "math": "Toán học",
    "physics": "Vật lí",
    "chemistry": "Hóa học",
    "biology": "Sinh học",
    "literature": "Ngữ văn",
    "history": "Lịch sử",
    "geography": "Địa lí",
    "english": "Tiếng Anh",
    "economics_law": "Giáo dục kinh tế và pháp luật",
}

VALID_DIFFICULTIES = {"EASY", "MEDIUM", "HARD"}
MIN_QUESTIONS = 1
MAX_QUESTIONS = 10
DEFAULT_QUESTIONS = 5
MAX_TOPIC_LENGTH = 100
# Policy: Google Gemini Developer API model availability depends on the account and project tier.
# For example, legacy projects may access gemini-2.5-flash, while new projects use gemini-3.8-flash
# or gemini-3.5-flash-lite. GEMINI_MODEL must be explicitly configured when enabling Gemini.
DEFAULT_GEMINI_MODEL = None


class AiProviderError(Exception):
    def __init__(self, code: str, message: str, status_code: int = 502):
        super().__init__(message)
        self.code = code
        self.message = message
        self.status_code = status_code


class AiProvider(ABC):
    @abstractmethod
    def is_configured(self) -> bool:
        pass

    @abstractmethod
    def generate_questions(
        self, subject: str, grade: int, topic: str, difficulty: str, count: int
    ) -> list[dict[str, Any]]:
        pass

    @abstractmethod
    def explain_more(
        self, question: dict[str, Any], user_selection: str, student_query: str
    ) -> str:
        pass


class UnconfiguredAiProvider(AiProvider):
    def is_configured(self) -> bool:
        return False

    def generate_questions(
        self, subject: str, grade: int, topic: str, difficulty: str, count: int
    ) -> list[dict[str, Any]]:
        raise AiProviderError(
            "AI_PROVIDER_NOT_CONFIGURED",
            "Dịch vụ AI chưa được cấu hình khóa API. Vui lòng liên hệ quản trị viên.",
            status_code=503,
        )

    def explain_more(
        self, question: dict[str, Any], user_selection: str, student_query: str
    ) -> str:
        raise AiProviderError(
            "AI_PROVIDER_NOT_CONFIGURED",
            "Dịch vụ AI chưa được cấu hình khóa API.",
            status_code=503,
        )


class FakeAiProvider(AiProvider):
    """Deterministic, offline provider for automated testing and demo."""

    def __init__(self, demo_mode: bool = False, failure_mode: str | None = None):
        self.demo_mode = demo_mode
        self.failure_mode = failure_mode  # "TIMEOUT", "RATE_LIMIT", "MALFORMED_JSON", "UNCONFIGURED"

    def is_configured(self) -> bool:
        return self.failure_mode != "UNCONFIGURED"

    def generate_questions(
        self, subject: str, grade: int, topic: str, difficulty: str, count: int
    ) -> list[dict[str, Any]]:
        if self.failure_mode == "UNCONFIGURED":
            raise AiProviderError("AI_PROVIDER_NOT_CONFIGURED", "Provider unconfigured.", 503)
        if self.failure_mode == "TIMEOUT":
            raise AiProviderError("AI_TIMEOUT", "Yêu cầu tới AI quá thời gian chờ.", 504)
        if self.failure_mode == "RATE_LIMIT":
            raise AiProviderError("AI_RATE_LIMIT", "Hệ thống AI đang quá tải.", 429)
        if self.failure_mode == "MALFORMED_JSON":
            raise AiProviderError("AI_MALFORMED_OUTPUT", "Dữ liệu AI trả về sai định dạng.", 502)

        subj_name = SUBJECT_NAMES_VI.get(subject, subject)
        questions = []
        options_keys = ["A", "B", "C", "D"]
        for idx in range(count):
            correct_opt = options_keys[idx % 4]
            q_num = idx + 1
            questions.append({
                "prompt": f"Câu hỏi {q_num} về {topic} ({subj_name} lớp {grade}, mức độ {difficulty}): Nhận định nào sau đây là chính xác?",
                "options": {
                    "A": f"Phương án A cho câu {q_num}: Khái niệm cơ bản về {topic}",
                    "B": f"Phương án B cho câu {q_num}: Quy tắc áp dụng cho {topic}",
                    "C": f"Phương án C cho câu {q_num}: Tính chất đặc trưng của {topic}",
                    "D": f"Phương án D cho câu {q_num}: Hệ quả quan trọng liên quan đến {topic}",
                },
                "correctOption": correct_opt,
                "explanation": f"Giải thích chi tiết cho câu {q_num}: Đáp án {correct_opt} là đáp án chính xác theo chương trình {subj_name} {grade}. Các phương án còn lại chưa đầy đủ hoặc không phù hợp với giả thiết về {topic}.",
                "hint": f"Gợi ý: Hãy nhớ lại định nghĩa và tính chất cơ bản của {topic}.",
            })
        return questions

    def explain_more(
        self, question: dict[str, Any], user_selection: str, student_query: str
    ) -> str:
        if self.failure_mode == "UNCONFIGURED":
            raise AiProviderError("AI_PROVIDER_NOT_CONFIGURED", "Provider unconfigured.", 503)
        if self.failure_mode == "TIMEOUT":
            raise AiProviderError("AI_TIMEOUT", "Timeout explaining question.", 504)

        return (
            f"Gia sư AI giải thích thêm: Đối với câu hỏi này và câu hỏi của bạn ('{student_query}'), "
            f"khi bạn chọn '{user_selection}', hãy chú ý đến điều kiện lý thuyết của bài toán. "
            f"Đáp án đúng là '{question.get('correctOption', '')}' vì thỏa mãn đầy đủ các quy tắc đã học."
        )


class GeminiAiProvider(AiProvider):
    """Google Gemini AI implementation for question generation and interactive tutor assistance."""

    def __init__(self, api_key: str, model: str, timeout_seconds: float = 15.0):
        cleaned_key = (api_key or "").strip()
        if not cleaned_key:
            raise AiProviderError("AI_PROVIDER_NOT_CONFIGURED", "API Key cho Gemini không được để trống hoặc chỉ chứa khoảng trắng.", 503)

        cleaned_model = (model or "").strip()
        if not cleaned_model:
            raise AiProviderError("AI_CONFIG_ERROR", "Tên model Gemini (GEMINI_MODEL) không được để trống hoặc chỉ chứa khoảng trắng.", 500)

        self.api_key = cleaned_key
        self.model = cleaned_model
        self.timeout_seconds = timeout_seconds

    def is_configured(self) -> bool:
        return bool(self.api_key and self.model)

    def _call_gemini_api(self, prompt_text: str, response_mime_type: str = "application/json") -> str:
        if not self.is_configured():
            raise AiProviderError("AI_PROVIDER_NOT_CONFIGURED", "Chưa cấu hình API Key hoặc model cho Gemini.", 503)

        url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.model}:generateContent"
        payload = {
            "contents": [
                {
                    "parts": [{"text": prompt_text}]
                }
            ],
            "generationConfig": {
                "temperature": 0.4,
                "maxOutputTokens": 2048,
                "responseMimeType": response_mime_type
            }
        }
        data_bytes = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            url,
            data=data_bytes,
            headers={
                "Content-Type": "application/json",
                "x-goog-api-key": self.api_key
            },
            method="POST"
        )
        try:
            with urllib.request.urlopen(req, timeout=self.timeout_seconds) as resp:
                resp_bytes = resp.read()
                resp_json = json.loads(resp_bytes.decode("utf-8"))
                candidates = resp_json.get("candidates", [])
                if not candidates:
                    raise AiProviderError("AI_MALFORMED_OUTPUT", "Mô hình không trả về nội dung.", 502)
                candidate = candidates[0]
                finish_reason = candidate.get("finishReason", "")
                if finish_reason in ("SAFETY", "BLOCKLIST", "PROHIBITED_CONTENT"):
                    raise AiProviderError("AI_SAFETY_BLOCKED", "Nội dung bị chặn bởi bộ lọc an toàn của AI.", 502)
                part = candidate.get("content", {}).get("parts", [{}])[0]
                text = part.get("text", "")
                if not text:
                    raise AiProviderError("AI_MALFORMED_OUTPUT", "Mô hình không trả về nội dung văn bản.", 502)
                return text
        except AiProviderError:
            raise
        except urllib.error.HTTPError as http_err:
            if http_err.code == 429:
                raise AiProviderError("AI_RATE_LIMIT", "Dịch vụ AI đang quá tải (429). Vui lòng thử lại.", 429)
            elif http_err.code in (401, 403):
                raise AiProviderError("AI_AUTH_FAILURE", "Xác thực API Key AI thất bại.", 502)
            else:
                raise AiProviderError("AI_UPSTREAM_ERROR", f"Lỗi dịch vụ AI (HTTP {http_err.code}).", 502)
        except (urllib.error.URLError, TimeoutError) as net_err:
            raise AiProviderError("AI_TIMEOUT", "Kết nối tới dịch vụ AI quá thời gian chờ.", 504)
        except Exception as ex:
            raise AiProviderError("AI_UPSTREAM_ERROR", f"Lỗi không xác định khi gọi AI: {str(ex)}", 502)

    def generate_questions(
        self, subject: str, grade: int, topic: str, difficulty: str, count: int
    ) -> list[dict[str, Any]]:
        subj_name = SUBJECT_NAMES_VI.get(subject, subject)
        prompt = f"""
Bạn là chuyên gia khảo thí giáo dục phổ thông Việt Nam. Hãy tạo đúng {count} câu hỏi trắc nghiệm khách quan 4 lựa chọn (A, B, C, D) cho học sinh THPT:
- Môn học: {subj_name}
- Lớp: {grade}
- Chủ đề: {topic}
- Mức độ: {difficulty} (EASY: Nhận biết, MEDIUM: Thông hiểu, HARD: Vận dụng)
- Ngôn ngữ: Tiếng Việt chuẩn mực, sư phạm.
- Yêu cầu cấu trúc: Trả về duy nhất một mảng JSON (JSON array) gồm đúng {count} phần tử.
Mỗi phần tử có cấu trúc:
{{
  "prompt": "Nội dung câu hỏi rõ ràng, không mập mờ...",
  "options": {{
    "A": "Lựa chọn A...",
    "B": "Lựa chọn B...",
    "C": "Lựa chọn C...",
    "D": "Lựa chọn D..."
  }},
  "correctOption": "A", // hoặc B, C, D
  "explanation": "Giải thích chi tiết tại sao đáp án đúng và chỉ ra sai lầm thường gặp...",
  "hint": "Gợi ý định hướng giải..."
}}
Lưu ý: Chỉ có đúng 1 đáp án đúng trong 4 phương án. Các phương án nhiễu phải hợp lý. Tuyệt đối không chèn mã script/HTML độc hại.
"""
        raw_text = self._call_gemini_api(prompt, response_mime_type="application/json")
        try:
            cleaned = raw_text.strip()
            match = re.search(r"```(?:json)?\s*(.*?)\s*```", cleaned, re.DOTALL)
            if match:
                cleaned = match.group(1).strip()
            else:
                cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned, flags=re.MULTILINE)
                cleaned = re.sub(r"\s*```$", "", cleaned, flags=re.MULTILINE).strip()
            parsed = json.loads(cleaned)
            if isinstance(parsed, dict) and "questions" in parsed:
                parsed = parsed["questions"]
            if not isinstance(parsed, list):
                raise ValueError("Output is not a list")
            return parsed
        except Exception as e:
            logger.warning("Failed to parse Gemini output: %s. Raw: %s", e, raw_text)
            raise AiProviderError("AI_MALFORMED_OUTPUT", "Dữ liệu trả về từ mô hình AI không đúng cấu trúc yêu cầu.", 502)

    def explain_more(
        self, question: dict[str, Any], user_selection: str, student_query: str
    ) -> str:
        prompt = f"""
Bạn là Gia sư AI tận tâm, khuyến khích học sinh học tập.
Dưới đây là câu hỏi học sinh đang luyện:
Câu hỏi: {question.get('prompt', '')}
Lựa chọn của học sinh: {user_selection}
Đáp án chính xác: {question.get('correctOption', '')}
Lời giải gốc: {question.get('explanation', '')}

Câu hỏi/thắc mắc thêm của học sinh: "{student_query}"

Hãy trả lời thắc mắc của học sinh bằng tiếng Việt ngắn gọn (dưới 200 từ), ân cần, giải thích cặn kẽ bản chất kiến thức và phương pháp suy luận. Không chế giễu học sinh.
"""
        raw = self._call_gemini_api(prompt, response_mime_type="text/plain")
        return sanitize_text(raw, max_len=2000)


def sanitize_text(text: Any, max_len: int = 2000) -> str:
    if not isinstance(text, str):
        return ""
    # Strip script/html dangerous tags, objects, embeds and active event handlers
    cleaned = re.sub(r"<\s*script[^>]*>.*?<\s*/\s*script\s*>", "", text, flags=re.DOTALL | re.IGNORECASE)
    cleaned = re.sub(r"<\s*iframe[^>]*>.*?<\s*/\s*iframe\s*>", "", cleaned, flags=re.DOTALL | re.IGNORECASE)
    cleaned = re.sub(r"<\s*(?:object|embed|applet)[^>]*>.*?<\s*/\s*(?:object|embed|applet)\s*>", "", cleaned, flags=re.DOTALL | re.IGNORECASE)
    cleaned = re.sub(r"javascript\s*:", "", cleaned, flags=re.IGNORECASE)
    cleaned = re.sub(r"on\w+\s*=", "", cleaned, flags=re.IGNORECASE)
    return cleaned.strip()[:max_len]


def validate_and_sanitize_questions(
    raw_list: list[dict[str, Any]], expected_count: int, session_id: str
) -> list[dict[str, Any]]:
    if not isinstance(raw_list, list) or len(raw_list) != expected_count:
        raise AiProviderError(
            "AI_VALIDATION_FAILED",
            f"Mô hình AI tạo {len(raw_list) if isinstance(raw_list, list) else 0} câu hỏi, khác số lượng yêu cầu ({expected_count}).",
            status_code=502,
        )

    validated = []
    seen_prompts = set()
    seen_qids = set()

    for idx, raw in enumerate(raw_list):
        if not isinstance(raw, dict):
            raise AiProviderError("AI_VALIDATION_FAILED", f"Câu hỏi {idx + 1} không phải đối tượng JSON hợp lệ.", 502)

        prompt = sanitize_text(raw.get("prompt", ""), max_len=2000)
        if len(prompt) < 10:
            raise AiProviderError("AI_VALIDATION_FAILED", f"Câu hỏi {idx + 1} có nội dung quá ngắn.", 502)

        norm_prompt = re.sub(r"\s+", " ", prompt).strip().lower()
        if norm_prompt in seen_prompts:
            raise AiProviderError("AI_VALIDATION_FAILED", f"Phát hiện câu hỏi trùng lặp trong bộ câu hỏi (câu {idx + 1}).", 502)
        seen_prompts.add(norm_prompt)

        options = raw.get("options")
        if not isinstance(options, dict) or set(options.keys()) != {"A", "B", "C", "D"}:
            raise AiProviderError("AI_VALIDATION_FAILED", f"Câu hỏi {idx + 1} phải có đúng 4 phương án A, B, C, D.", 502)

        cleaned_options = {}
        seen_opts = set()
        for k in ("A", "B", "C", "D"):
            opt_text = sanitize_text(options.get(k, ""), max_len=500)
            if not opt_text:
                raise AiProviderError("AI_VALIDATION_FAILED", f"Câu hỏi {idx + 1} có phương án {k} rỗng.", 502)
            norm_opt = re.sub(r"\s+", " ", opt_text).strip().lower()
            if norm_opt in seen_opts:
                raise AiProviderError("AI_VALIDATION_FAILED", f"Câu hỏi {idx + 1} có các phương án trùng nhau sau khi chuẩn hóa.", 502)
            seen_opts.add(norm_opt)
            cleaned_options[k] = opt_text

        correct_opt = str(raw.get("correctOption", "")).strip().upper()
        if correct_opt not in ("A", "B", "C", "D"):
            raise AiProviderError("AI_VALIDATION_FAILED", f"Câu hỏi {idx + 1} có đáp án đúng không hợp lệ ({correct_opt}).", 502)

        explanation = sanitize_text(raw.get("explanation", ""), max_len=2000)
        if not explanation:
            raise AiProviderError("AI_VALIDATION_FAILED", f"Câu hỏi {idx + 1} có lời giải rỗng.", 502)

        hint = sanitize_text(raw.get("hint", ""), max_len=500)
        if not hint:
            hint = "Gợi ý: Áp dụng định nghĩa và công thức cơ bản."

        qid = f"ai_q_{session_id[:8]}_{idx + 1}"
        if qid in seen_qids:
            raise AiProviderError("AI_VALIDATION_FAILED", f"Phát hiện trùng lặp mã câu hỏi {qid}.", 502)
        seen_qids.add(qid)

        validated.append({
            "id": qid,
            "prompt": prompt,
            "options": cleaned_options,
            "correctOption": correct_opt,
            "explanation": explanation,
            "hint": hint,
            "order": idx + 1,
        })

    return validated


class AiPracticeService:
    def __init__(self, db: Database, provider: AiProvider | None = None):
        self.db = db
        self.provider = provider or UnconfiguredAiProvider()

    def set_provider(self, provider: AiProvider):
        self.provider = provider

    def _claim_provider(self, user_id, operation, key, request_hash):
        """Reserve a paid call durably without holding a transaction during network IO.

        Old PENDING calls remain unknown after a crash: never replay them automatically.
        Their concurrency reservation expires after 120s, but they still consume quota.
        Failed provider calls also consume quota to bound repeated expensive failures.
        """
        now = time.time()
        with self.db.transaction() as db:
            if operation == 'GENERATE':
                cached = db.execute("SELECT request_hash,response FROM idempotency WHERE owner=? AND operation='AI_PRACTICE_CREATE' AND key=?", (user_id, key)).fetchone()
                if cached:
                    if cached['request_hash'] != request_hash:
                        raise HTTPException(409, detail={'code': 'IDEMPOTENCY_CONFLICT'})
                    return None, json.loads(cached['response'])
            previous = db.execute('SELECT * FROM ai_provider_operations WHERE user_id=? AND operation=? AND operation_key=?', (user_id, operation, key)).fetchone()
            if previous:
                code = 'IDEMPOTENCY_CONFLICT' if previous['request_hash'] != request_hash else 'AI_REQUEST_IN_PROGRESS' if previous['state'] == 'PENDING' else 'AI_REQUEST_FAILED'
                raise HTTPException(409, detail={'code': code, 'message': 'Tác vụ đã được tiếp nhận. Hãy kiểm tra lại; nếu tác vụ thất bại, bắt đầu một lượt mới.'})
            count = db.execute('SELECT COUNT(*) FROM ai_provider_operations WHERE user_id=? AND operation=? AND created_at>?', (user_id, operation, now - 86400)).fetchone()[0]
            if count >= (20 if operation == 'GENERATE' else 50):
                raise HTTPException(429, detail={'code': 'AI_QUOTA_EXCEEDED', 'message': 'Đã đạt giới hạn AI trong 24 giờ. Vui lòng thử lại sau.'})
            active = db.execute("SELECT user_id FROM ai_provider_operations WHERE state='PENDING' AND created_at>?", (now - 120,)).fetchall()
            if len(active) >= 4 or any(r['user_id'] == user_id for r in active):
                raise HTTPException(429, detail={'code': 'AI_BUSY', 'message': 'Đang xử lý yêu cầu AI khác. Vui lòng đợi.'})
            call_id = str(uuid.uuid4())
            db.execute("INSERT INTO ai_provider_operations VALUES(?,?,?,?,?,'PENDING',?)", (call_id, user_id, operation, key, request_hash, now))
            return call_id, None

    def _finish_provider(self, call_id, state):
        with self.db.transaction() as db:
            db.execute('UPDATE ai_provider_operations SET state=? WHERE id=?', (state, call_id))

    def create_session(
        self,
        user_id: int,
        subject_id: str,
        grade: int,
        topic: str,
        difficulty: str = "MEDIUM",
        question_count: int = DEFAULT_QUESTIONS,
        idempotency_key: str | None = None,
    ) -> dict[str, Any]:
        # 1. Input validation
        norm_sub = SUBJECT_ALIASES.get(subject_id.strip().lower(), subject_id.strip().lower())
        if norm_sub not in VALID_SUBJECTS:
            raise HTTPException(400, detail={"code": "INVALID_SUBJECT", "message": f"Môn học '{subject_id}' không thuộc 9 môn được hỗ trợ."})

        if grade not in (10, 11, 12):
            raise HTTPException(400, detail={"code": "INVALID_GRADE", "message": "Khối lớp phải là 10, 11 hoặc 12."})

        cleaned_topic = topic.strip()
        if not cleaned_topic:
            raise HTTPException(400, detail={"code": "INVALID_TOPIC", "message": "Chủ đề luyện tập không được để trống."})
        if len(cleaned_topic) > MAX_TOPIC_LENGTH:
            raise HTTPException(400, detail={"code": "TOPIC_TOO_LONG", "message": f"Chủ đề vượt quá {MAX_TOPIC_LENGTH} ký tự."})

        norm_diff = difficulty.strip().upper()
        if norm_diff not in VALID_DIFFICULTIES:
            raise HTTPException(400, detail={"code": "INVALID_DIFFICULTY", "message": "Độ khó phải là EASY, MEDIUM hoặc HARD."})

        if not (MIN_QUESTIONS <= question_count <= MAX_QUESTIONS):
            raise HTTPException(400, detail={"code": "INVALID_QUESTION_COUNT", "message": f"Số câu hỏi phải từ {MIN_QUESTIONS} đến {MAX_QUESTIONS} câu."})

        # 2. Check Idempotency (if key provided)
        payload_repr = {
            "subjectId": norm_sub,
            "grade": grade,
            "topic": cleaned_topic,
            "difficulty": norm_diff,
            "questionCount": question_count,
        }
        request_hash = hashlib.sha256(dump(payload_repr).encode("utf-8")).hexdigest()

        raw_key = (idempotency_key or "").strip()
        if raw_key:
            with self.db.read() as db:
                cached = db.execute(
                    "SELECT request_hash, response FROM idempotency WHERE owner=? AND operation='AI_PRACTICE_CREATE' AND key=?",
                    (user_id, raw_key)
                ).fetchone()
                if cached:
                    if cached["request_hash"] != request_hash:
                        raise HTTPException(409, detail={
                            "code": "IDEMPOTENCY_CONFLICT",
                            "message": "Idempotency-Key đã được sử dụng với cấu hình luyện tập khác."
                        })
                    return json.loads(cached["response"])

        call_id, cached = self._claim_provider(user_id, 'GENERATE', raw_key or str(uuid.uuid4()), request_hash)
        if cached is not None:
            return cached
        success = False
        try:
            # 3. Call AI Provider OUTSIDE DB transaction!
            temp_session_id = str(uuid.uuid4())
            try:
                raw_questions = self.provider.generate_questions(
                    subject=norm_sub,
                    grade=grade,
                    topic=cleaned_topic,
                    difficulty=norm_diff,
                    count=question_count
                )
            except AiProviderError as ape:
                raise HTTPException(ape.status_code, detail={"code": ape.code, "message": ape.message})

            # 4. Validate & sanitize output
            try:
                validated_questions = validate_and_sanitize_questions(raw_questions, question_count, temp_session_id)
            except AiProviderError as ape:
                raise HTTPException(ape.status_code, detail={"code": ape.code, "message": ape.message})

            now = time.time()
            session_id = temp_session_id

            # 5. Persist Session & Idempotency in DB transaction
            with self.db.transaction() as db:
                # Check idempotency again in case of race condition
                if raw_key:
                    cached = db.execute(
                        "SELECT request_hash, response FROM idempotency WHERE owner=? AND operation='AI_PRACTICE_CREATE' AND key=?",
                        (user_id, raw_key)
                    ).fetchone()
                    if cached:
                        if cached["request_hash"] != request_hash:
                            raise HTTPException(409, detail={"code": "IDEMPOTENCY_CONFLICT", "message": "Idempotency-Key conflict."})
                        success = True
                        return json.loads(cached["response"])

                db.execute("""
                    INSERT INTO ai_practice_sessions (
                        id, user_id, subject_id, grade, topic, difficulty,
                        question_count, questions_snapshot, answers, current_index,
                        state, summary, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, '{}', 0, 'IN_PROGRESS', NULL, ?, ?)
                """, (
                    session_id, user_id, norm_sub, grade, cleaned_topic, norm_diff,
                    question_count, dump(validated_questions), now, now
                ))

                result_dto = self._build_session_dto(
                    session_row={
                        "id": session_id,
                        "user_id": user_id,
                        "subject_id": norm_sub,
                        "grade": grade,
                        "topic": cleaned_topic,
                        "difficulty": norm_diff,
                        "question_count": question_count,
                        "questions_snapshot": dump(validated_questions),
                        "answers": "{}",
                        "current_index": 0,
                        "state": "IN_PROGRESS",
                        "summary": None,
                        "created_at": now,
                        "updated_at": now,
                    },
                    reveal_all=False
                )

                if raw_key:
                    db.execute("""
                        INSERT INTO idempotency(owner, operation, key, request_hash, response)
                        VALUES(?, 'AI_PRACTICE_CREATE', ?, ?, ?)
                    """, (user_id, raw_key, request_hash, dump(result_dto)))

            success = True
            return result_dto
        finally:
            self._finish_provider(call_id, 'FINISHED' if success else 'FAILED')

    def get_session(self, user_id: int, session_id: str) -> dict[str, Any]:
        with self.db.read() as db:
            row = db.execute(
                "SELECT * FROM ai_practice_sessions WHERE id=?", (session_id,)
            ).fetchone()
            if not row:
                raise HTTPException(404, detail={"code": "SESSION_NOT_FOUND", "message": "Không tìm thấy phiên luyện tập."})
            if row["user_id"] != user_id:
                raise HTTPException(403, detail={"code": "ACCESS_DENIED", "message": "Bạn không có quyền truy cập phiên này."})

            reveal_all = (row["state"] == "COMPLETED")
            return self._build_session_dto(row, reveal_all=reveal_all)

    def submit_answer(
        self, user_id: int, session_id: str, question_id: str, selected_option: str,
        submission_id: str | None = None
    ) -> dict[str, Any]:
        clean_opt = selected_option.strip().upper()
        if clean_opt not in ("A", "B", "C", "D"):
            raise HTTPException(400, detail={"code": "INVALID_OPTION", "message": "Phương án đã chọn phải là A, B, C hoặc D."})

        now = time.time()
        with self.db.transaction() as db:
            operation = 'AI_ANSWER:' + session_id
            payload_hash = hashlib.sha256(dump({'questionId': question_id, 'selectedOption': clean_opt}).encode()).hexdigest()
            if submission_id:
                cached = db.execute('SELECT request_hash,response FROM idempotency WHERE owner=? AND operation=? AND key=?', (user_id, operation, submission_id)).fetchone()
                if cached:
                    if cached['request_hash'] != payload_hash:
                        raise HTTPException(409, detail={'code': 'IDEMPOTENCY_CONFLICT'})
                    return json.loads(cached['response'])
            row = db.execute(
                "SELECT * FROM ai_practice_sessions WHERE id=?", (session_id,)
            ).fetchone()
            if not row:
                raise HTTPException(404, detail={"code": "SESSION_NOT_FOUND", "message": "Không tìm thấy phiên luyện tập."})
            if row["user_id"] != user_id:
                raise HTTPException(403, detail={"code": "ACCESS_DENIED", "message": "Bạn không có quyền truy cập phiên này."})
            if row["state"] != "IN_PROGRESS":
                raise HTTPException(409, detail={"code": "SESSION_NOT_ACTIVE", "message": "Phiên luyện tập đã kết thúc."})

            questions = json.loads(row["questions_snapshot"])
            target_q = next((q for q in questions if q["id"] == question_id), None)
            if not target_q:
                raise HTTPException(404, detail={"code": "QUESTION_NOT_FOUND", "message": "Câu hỏi không tồn tại trong phiên luyện tập."})

            answers = json.loads(row["answers"])
            prev_ans = answers.get(question_id)

            if not submission_id and prev_ans and prev_ans.get('selectedOption') == clean_opt:
                return {
                    'questionId': question_id, 'isCorrect': prev_ans['isCorrect'],
                    'selectedOption': clean_opt, 'correctOption': target_q['correctOption'],
                    'explanation': target_q['explanation'], 'hint': target_q.get('hint', ''),
                    'firstTryCorrect': prev_ans.get('firstTryCorrect', False),
                    'attemptsCount': prev_ans.get('attemptsCount', 1), 'state': row['state'],
                }
            is_correct = (clean_opt == target_q["correctOption"])
            if prev_ans is None:
                first_try_correct = is_correct
                attempts_count = 1
            else:
                first_try_correct = prev_ans.get("firstTryCorrect", False)
                attempts_count = prev_ans.get("attemptsCount", 1) + 1

            answers[question_id] = {
                "selectedOption": clean_opt,
                "isCorrect": is_correct,
                "firstTryCorrect": first_try_correct,
                "attemptsCount": attempts_count,
                "revealed": True,
                "answeredAt": now,
            }

            # Update current index
            q_index = next((i for i, q in enumerate(questions) if q["id"] == question_id), 0)
            new_current = max(row["current_index"], q_index + 1)

            # Record in wrong questions notebook if wrong
            if not is_correct:
                self._record_notebook(db, row, target_q, clean_opt, now)

            db.execute("""
                UPDATE ai_practice_sessions
                SET answers=?, current_index=?, updated_at=?
                WHERE id=?
            """, (dump(answers), new_current, now, session_id))

            result = {
                "questionId": question_id,
                "isCorrect": is_correct,
                "selectedOption": clean_opt,
                "correctOption": target_q["correctOption"],
                "explanation": target_q["explanation"],
                "hint": target_q.get("hint", ""),
                "firstTryCorrect": first_try_correct,
                "attemptsCount": attempts_count,
                "state": "IN_PROGRESS",
            }

            if submission_id:
                db.execute('INSERT INTO idempotency(owner,operation,key,request_hash,response) VALUES(?,?,?,?,?)', (user_id, operation, submission_id, payload_hash, dump(result)))
            return result

    @staticmethod
    def _record_notebook(db, session, question, selected, now):
        db.execute("""
            INSERT INTO wrong_questions_notebook
            (id,user_id,question_id,subject_id,grade,selected_option,correct_option,
             explanation,question_snapshot,state,source,created_at,updated_at)
            VALUES(?,?,?,?,?,?,?,?,?,'NEEDS_REVIEW','AI_PRACTICE',?,?)
            ON CONFLICT(user_id,question_id) DO UPDATE SET
              selected_option=excluded.selected_option,correct_option=excluded.correct_option,
              explanation=excluded.explanation,question_snapshot=excluded.question_snapshot,
              state='NEEDS_REVIEW',source='AI_PRACTICE',updated_at=excluded.updated_at
        """, (str(uuid.uuid4()),session['user_id'],question['id'],session['subject_id'],
              session['grade'],selected or '',question['correctOption'],question['explanation'],
              dump(question),now,now))

    def skip_question(self, user_id: int, session_id: str, question_id: str) -> dict[str, Any]:
        now = time.time()
        with self.db.transaction() as db:
            row = db.execute(
                "SELECT * FROM ai_practice_sessions WHERE id=?", (session_id,)
            ).fetchone()
            if not row:
                raise HTTPException(404, detail={"code": "SESSION_NOT_FOUND", "message": "Không tìm thấy phiên luyện tập."})
            if row["user_id"] != user_id:
                raise HTTPException(403, detail={"code": "ACCESS_DENIED", "message": "Bạn không có quyền truy cập."})
            if row["state"] != "IN_PROGRESS":
                raise HTTPException(409, detail={"code": "SESSION_NOT_ACTIVE", "message": "Phiên luyện tập đã kết thúc."})

            questions = json.loads(row["questions_snapshot"])
            target_q = next((q for q in questions if q["id"] == question_id), None)
            if not target_q:
                raise HTTPException(404, detail={"code": "QUESTION_NOT_FOUND", "message": "Câu hỏi không tồn tại."})

            answers = json.loads(row["answers"])
            previous = answers.get(question_id)
            if previous and not previous.get('skipped'):
                raise HTTPException(409, detail={'code': 'ANSWER_ALREADY_RECORDED', 'message': 'Câu hỏi đã được trả lời. Hãy xem phản hồi hoặc chuyển câu.'})
            answers[question_id] = {
                "selectedOption": None,
                "isCorrect": False,
                "firstTryCorrect": False,
                "attemptsCount": 0,
                "skipped": True,
                "revealed": True,
                "answeredAt": now,
            }

            q_index = next((i for i, q in enumerate(questions) if q["id"] == question_id), 0)
            new_current = max(row["current_index"], q_index + 1)

            self._record_notebook(db, row, target_q, None, now)

            db.execute("""
                UPDATE ai_practice_sessions
                SET answers=?, current_index=?, updated_at=?
                WHERE id=?
            """, (dump(answers), new_current, now, session_id))

        return {
            "questionId": question_id,
            "skipped": True,
            "correctOption": target_q["correctOption"],
            "explanation": target_q["explanation"],
            "hint": target_q.get("hint", ""),
        }

    def complete_session(self, user_id: int, session_id: str) -> dict[str, Any]:
        now = time.time()
        with self.db.transaction() as db:
            row = db.execute(
                "SELECT * FROM ai_practice_sessions WHERE id=?", (session_id,)
            ).fetchone()
            if not row:
                raise HTTPException(404, detail={"code": "SESSION_NOT_FOUND", "message": "Không tìm thấy phiên luyện tập."})
            if row["user_id"] != user_id:
                raise HTTPException(403, detail={"code": "ACCESS_DENIED", "message": "Bạn không có quyền truy cập."})

            if row['state'] == 'COMPLETED':
                return self._build_session_dto(row, reveal_all=True)

            questions = json.loads(row["questions_snapshot"])
            answers = json.loads(row["answers"])

            first_try_count = 0
            assisted_count = 0
            incorrect_count = 0
            skipped_count = 0

            for q in questions:
                ans = answers.get(q["id"])
                if not ans or ans.get("skipped"):
                    skipped_count += 1
                elif ans.get("firstTryCorrect"):
                    first_try_count += 1
                elif ans.get("isCorrect"):
                    assisted_count += 1
                else:
                    incorrect_count += 1

            total = len(questions)
            accuracy = round((first_try_count / total * 100.0), 1) if total > 0 else 0.0

            summary = {
                "totalQuestions": total,
                "firstTryCorrectCount": first_try_count,
                "assistedCount": assisted_count,
                "incorrectCount": incorrect_count,
                "skippedCount": skipped_count,
                "accuracyFirstTry": accuracy,
                "completedAt": now,
            }

            db.execute("""
                UPDATE ai_practice_sessions
                SET state='COMPLETED', summary=?, updated_at=?
                WHERE id=?
            """, (dump(summary), now, session_id))

            completed_row = dict(row)
            completed_row["state"] = "COMPLETED"
            completed_row["summary"] = dump(summary)
            completed_row["updated_at"] = now

        return self._build_session_dto(completed_row, reveal_all=True)

    def report_question(
        self, user_id: int, session_id: str, question_id: str, reason: str, details: str = ""
    ) -> dict[str, Any]:
        with self.db.transaction() as db:
            row = db.execute(
                "SELECT * FROM ai_practice_sessions WHERE id=?", (session_id,)
            ).fetchone()
            if not row:
                raise HTTPException(404, detail={"code": "SESSION_NOT_FOUND", "message": "Không tìm thấy phiên."})
            if row["user_id"] != user_id:
                raise HTTPException(403, detail={"code": "ACCESS_DENIED", "message": "Bạn không có quyền."})

            questions = json.loads(row["questions_snapshot"])
            if not any(q["id"] == question_id for q in questions):
                raise HTTPException(404, detail={"code": "QUESTION_NOT_FOUND", "message": "Câu hỏi không thuộc phiên này."})

            clean_reason = reason.strip()
            if not clean_reason:
                raise HTTPException(400, detail={"code": "INVALID_REASON", "message": "Lý do báo cáo không được để trống."})

            report_id = str(uuid.uuid4())
            now = time.time()
            db.execute("""
                INSERT INTO ai_question_reports (id, user_id, session_id, question_id, reason, details, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """, (report_id, user_id, session_id, question_id, clean_reason, details.strip(), now))

        return {"reportId": report_id, "status": "RECEIVED", "message": "Cảm ơn bạn đã phản hồi chất lượng câu hỏi."}

    def explain_more(
        self, user_id: int, session_id: str, question_id: str, student_query: str
    ) -> dict[str, Any]:
        with self.db.read() as db:
            row = db.execute(
                "SELECT * FROM ai_practice_sessions WHERE id=?", (session_id,)
            ).fetchone()
            if not row:
                raise HTTPException(404, detail={"code": "SESSION_NOT_FOUND", "message": "Không tìm thấy phiên."})
            if row["user_id"] != user_id:
                raise HTTPException(403, detail={"code": "ACCESS_DENIED", "message": "Bạn không có quyền."})

            questions = json.loads(row["questions_snapshot"])
            target_q = next((q for q in questions if q["id"] == question_id), None)
            if not target_q:
                raise HTTPException(404, detail={"code": "QUESTION_NOT_FOUND", "message": "Câu hỏi không tồn tại."})

            answers = json.loads(row["answers"])
            if row['state'] != 'COMPLETED' and not answers.get(question_id, {}).get('revealed', False):
                raise HTTPException(409, detail={'code': 'ANSWER_REQUIRED', 'message': 'Hãy trả lời hoặc bỏ qua câu hỏi trước khi xem giải thích.'})
            user_ans = answers.get(question_id, {}).get("selectedOption", "(Chưa chọn)")

        clean_query = student_query.strip()
        if len(clean_query) > 500:
            raise HTTPException(400, detail={'code': 'INVALID_QUERY'})
        if not clean_query:
            raise HTTPException(400, detail={"code": "INVALID_QUERY", "message": "Nội dung câu hỏi không được để trống."})

        call_id, _ = self._claim_provider(user_id, 'EXPLAIN', str(uuid.uuid4()), hashlib.sha256(clean_query.encode()).hexdigest())
        success = False
        try:
            try:
                explanation = self.provider.explain_more(target_q, user_ans, clean_query)
            except AiProviderError as ape:
                raise HTTPException(ape.status_code, detail={"code": ape.code, "message": ape.message})
            success = True
        finally:
            self._finish_provider(call_id, 'FINISHED' if success else 'FAILED')

        return {"questionId": question_id, "explanation": explanation}

    def _build_session_dto(self, session_row: Any, reveal_all: bool) -> dict[str, Any]:
        row = dict(session_row)
        questions = json.loads(row["questions_snapshot"])
        answers = json.loads(row["answers"])

        client_questions = []
        for q in questions:
            qid = q["id"]
            ans = answers.get(qid)
            is_revealed = reveal_all or (ans is not None and ans.get("revealed", False))

            item = {
                "id": qid,
                "prompt": q["prompt"],
                "options": q["options"],
                "hint": q.get("hint", ""),
                "order": q.get("order", 1),
            }
            if is_revealed:
                item["correctOption"] = q["correctOption"]
                item["explanation"] = q["explanation"]
            client_questions.append(item)

        summary_data = json.loads(row["summary"]) if row.get("summary") else None

        return {
            "id": row["id"],
            "subjectId": row["subject_id"],
            "subjectName": SUBJECT_NAMES_VI.get(row["subject_id"], row["subject_id"]),
            "grade": row["grade"],
            "topic": row["topic"],
            "difficulty": row["difficulty"],
            "questionCount": row["question_count"],
            "currentIndex": row["current_index"],
            "state": row["state"],
            "questions": client_questions,
            "answers": answers,
            "summary": summary_data,
            "createdAt": row["created_at"],
            "updatedAt": row["updated_at"],
        }
