-- Migration: Grade 11 Curriculum Seeding
-- Generated automatically by AI Mentor tools

insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-chuyen-hoa-nang-luong',
    1,
    'LESSON',
    'Chuyển hóa năng lượng',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Chuyển hóa năng lượng theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Chuyển hóa năng lượng vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-chuyen-hoa-nang-luong-biology-11-chuyen-hoa-nang-luong-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Chuyển hóa năng lượng?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-chuyen-hoa-nang-luong-biology-11-chuyen-hoa-nang-luong-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Chuyển hóa năng lượng?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-chuyen-hoa-nang-luong-biology-11-chuyen-hoa-nang-luong-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Chuyển hóa năng lượng giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-trao-doi-nuoc-o-thuc-vat',
    2,
    'LESSON',
    'Trao đổi nước ở thực vật',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Trao đổi nước ở thực vật theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Trao đổi nước ở thực vật vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-trao-doi-nuoc-o-thuc-vat-biology-11-trao-doi-nuoc-o-thuc-vat-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Trao đổi nước ở thực vật?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-trao-doi-nuoc-o-thuc-vat-biology-11-trao-doi-nuoc-o-thuc-vat-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Trao đổi nước ở thực vật?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-trao-doi-nuoc-o-thuc-vat-biology-11-trao-doi-nuoc-o-thuc-vat-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Trao đổi nước ở thực vật giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-thoat-hoi-nuoc',
    3,
    'LESSON',
    'Thoát hơi nước',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Thoát hơi nước theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Thoát hơi nước vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-thoat-hoi-nuoc-biology-11-thoat-hoi-nuoc-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Thoát hơi nước?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-thoat-hoi-nuoc-biology-11-thoat-hoi-nuoc-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Thoát hơi nước?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-thoat-hoi-nuoc-biology-11-thoat-hoi-nuoc-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Thoát hơi nước giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-quang-hop',
    4,
    'LESSON',
    'Quang hợp',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Quang hợp theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Quang hợp vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-quang-hop-biology-11-quang-hop-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Quang hợp?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-quang-hop-biology-11-quang-hop-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Quang hợp?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-quang-hop-biology-11-quang-hop-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Quang hợp giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-ho-hap-thuc-vat',
    5,
    'FIVE_LESSON_REVIEW',
    'Hô hấp thực vật',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Hô hấp thực vật theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Hô hấp thực vật vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-ho-hap-thuc-vat-biology-11-ho-hap-thuc-vat-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Hô hấp thực vật?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-ho-hap-thuc-vat-biology-11-ho-hap-thuc-vat-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Hô hấp thực vật?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-ho-hap-thuc-vat-biology-11-ho-hap-thuc-vat-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Hô hấp thực vật giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-tieu-hoa-o-dong-vat',
    6,
    'LESSON',
    'Tiêu hóa ở động vật',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Tiêu hóa ở động vật theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Tiêu hóa ở động vật vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-tieu-hoa-o-dong-vat-biology-11-tieu-hoa-o-dong-vat-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Tiêu hóa ở động vật?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-tieu-hoa-o-dong-vat-biology-11-tieu-hoa-o-dong-vat-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Tiêu hóa ở động vật?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-tieu-hoa-o-dong-vat-biology-11-tieu-hoa-o-dong-vat-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Tiêu hóa ở động vật giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-ho-hap-o-dong-vat',
    7,
    'LESSON',
    'Hô hấp ở động vật',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Hô hấp ở động vật theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Hô hấp ở động vật vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-ho-hap-o-dong-vat-biology-11-ho-hap-o-dong-vat-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Hô hấp ở động vật?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-ho-hap-o-dong-vat-biology-11-ho-hap-o-dong-vat-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Hô hấp ở động vật?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-ho-hap-o-dong-vat-biology-11-ho-hap-o-dong-vat-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Hô hấp ở động vật giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-tuan-hoan',
    8,
    'LESSON',
    'Tuần hoàn',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Tuần hoàn theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Tuần hoàn vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-tuan-hoan-biology-11-tuan-hoan-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Tuần hoàn?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-tuan-hoan-biology-11-tuan-hoan-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Tuần hoàn?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-tuan-hoan-biology-11-tuan-hoan-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Tuần hoàn giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-mien-dich',
    9,
    'LESSON',
    'Miễn dịch',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Miễn dịch theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Miễn dịch vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-mien-dich-biology-11-mien-dich-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Miễn dịch?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-mien-dich-biology-11-mien-dich-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Miễn dịch?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-mien-dich-biology-11-mien-dich-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Miễn dịch giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-can-bang-noi-moi',
    10,
    'FIVE_LESSON_REVIEW',
    'Cân bằng nội môi',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Cân bằng nội môi theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Cân bằng nội môi vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-can-bang-noi-moi-biology-11-can-bang-noi-moi-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Cân bằng nội môi?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-can-bang-noi-moi-biology-11-can-bang-noi-moi-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Cân bằng nội môi?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-can-bang-noi-moi-biology-11-can-bang-noi-moi-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Cân bằng nội môi giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-cam-ung-o-thuc-vat',
    11,
    'LESSON',
    'Cảm ứng ở thực vật',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Cảm ứng ở thực vật theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Cảm ứng ở thực vật vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-cam-ung-o-thuc-vat-biology-11-cam-ung-o-thuc-vat-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Cảm ứng ở thực vật?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-cam-ung-o-thuc-vat-biology-11-cam-ung-o-thuc-vat-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Cảm ứng ở thực vật?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-cam-ung-o-thuc-vat-biology-11-cam-ung-o-thuc-vat-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Cảm ứng ở thực vật giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-dien-the-nghi-va-hoat-dong',
    12,
    'LESSON',
    'Điện thế nghỉ và hoạt động',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Điện thế nghỉ và hoạt động theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Điện thế nghỉ và hoạt động vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-dien-the-nghi-va-hoat-dong-biology-11-dien-the-nghi-va-hoat-dong-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Điện thế nghỉ và hoạt động?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-dien-the-nghi-va-hoat-dong-biology-11-dien-the-nghi-va-hoat-dong-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Điện thế nghỉ và hoạt động?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-dien-the-nghi-va-hoat-dong-biology-11-dien-the-nghi-va-hoat-dong-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Điện thế nghỉ và hoạt động giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-tap-tinh-dong-vat',
    13,
    'LESSON',
    'Tập tính động vật',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Tập tính động vật theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Tập tính động vật vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-tap-tinh-dong-vat-biology-11-tap-tinh-dong-vat-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Tập tính động vật?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-tap-tinh-dong-vat-biology-11-tap-tinh-dong-vat-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Tập tính động vật?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-tap-tinh-dong-vat-biology-11-tap-tinh-dong-vat-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Tập tính động vật giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-sinh-truong',
    14,
    'LESSON',
    'Sinh trưởng',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Sinh trưởng theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Sinh trưởng vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-sinh-truong-biology-11-sinh-truong-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Sinh trưởng?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-sinh-truong-biology-11-sinh-truong-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Sinh trưởng?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-sinh-truong-biology-11-sinh-truong-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Sinh trưởng giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-sinh-san',
    15,
    'FIVE_LESSON_REVIEW',
    'Sinh sản',
    'Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Sinh sản theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Sinh sản vào thực tiễn."]'::jsonb,
    '[{"id": "biology-11-sinh-san-biology-11-sinh-san-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Sinh sản?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "biology-11-sinh-san-biology-11-sinh-san-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Sinh sản?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "biology-11-sinh-san-biology-11-sinh-san-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Sinh sản giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-can-bang-hoa-hoc',
    1,
    'LESSON',
    'Cân bằng hóa học',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Cân bằng hóa học theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Cân bằng hóa học vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-can-bang-hoa-hoc-chemistry-11-can-bang-hoa-hoc-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Cân bằng hóa học?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-can-bang-hoa-hoc-chemistry-11-can-bang-hoa-hoc-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Cân bằng hóa học?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-can-bang-hoa-hoc-chemistry-11-can-bang-hoa-hoc-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Cân bằng hóa học giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-hang-so-can-bang-kc',
    2,
    'LESSON',
    'Hằng số cân bằng Kc',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Hằng số cân bằng Kc theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Hằng số cân bằng Kc vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-hang-so-can-bang-kc-chemistry-11-hang-so-can-bang-kc-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Hằng số cân bằng Kc?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-hang-so-can-bang-kc-chemistry-11-hang-so-can-bang-kc-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Hằng số cân bằng Kc?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-hang-so-can-bang-kc-chemistry-11-hang-so-can-bang-kc-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Hằng số cân bằng Kc giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-su-dien-li',
    3,
    'LESSON',
    'Sự điện li',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Sự điện li theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Sự điện li vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-su-dien-li-chemistry-11-su-dien-li-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Sự điện li?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-su-dien-li-chemistry-11-su-dien-li-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Sự điện li?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-su-dien-li-chemistry-11-su-dien-li-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Sự điện li giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-thuyet-br-nsted-lowry',
    4,
    'LESSON',
    'Thuyết Brønsted-Lowry',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Thuyết Brønsted-Lowry theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Thuyết Brønsted-Lowry vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-thuyet-br-nsted-lowry-chemistry-11-thuyet-br-nsted-lowry-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Thuyết Brønsted-Lowry?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-thuyet-br-nsted-lowry-chemistry-11-thuyet-br-nsted-lowry-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Thuyết Brønsted-Lowry?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-thuyet-br-nsted-lowry-chemistry-11-thuyet-br-nsted-lowry-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Thuyết Brønsted-Lowry giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-ph-va-chat-chi-thi',
    5,
    'FIVE_LESSON_REVIEW',
    'pH và chất chỉ thị',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài pH và chất chỉ thị theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần pH và chất chỉ thị vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-ph-va-chat-chi-thi-chemistry-11-ph-va-chat-chi-thi-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài pH và chất chỉ thị?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-ph-va-chat-chi-thi-chemistry-11-ph-va-chat-chi-thi-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần pH và chất chỉ thị?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-ph-va-chat-chi-thi-chemistry-11-ph-va-chat-chi-thi-q3", "prompt": "Tình huống thực tế áp dụng kiến thức pH và chất chỉ thị giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-nitrogen',
    6,
    'LESSON',
    'Nitrogen',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Nitrogen theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Nitrogen vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-nitrogen-chemistry-11-nitrogen-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Nitrogen?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-nitrogen-chemistry-11-nitrogen-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Nitrogen?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-nitrogen-chemistry-11-nitrogen-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Nitrogen giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-ammonia',
    7,
    'LESSON',
    'Ammonia',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Ammonia theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Ammonia vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-ammonia-chemistry-11-ammonia-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Ammonia?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-ammonia-chemistry-11-ammonia-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Ammonia?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-ammonia-chemistry-11-ammonia-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Ammonia giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-nitric-acid',
    8,
    'LESSON',
    'Nitric acid',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Nitric acid theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Nitric acid vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-nitric-acid-chemistry-11-nitric-acid-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Nitric acid?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-nitric-acid-chemistry-11-nitric-acid-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Nitric acid?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-nitric-acid-chemistry-11-nitric-acid-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Nitric acid giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-sulfur',
    9,
    'LESSON',
    'Sulfur',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Sulfur theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Sulfur vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-sulfur-chemistry-11-sulfur-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Sulfur?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-sulfur-chemistry-11-sulfur-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Sulfur?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-sulfur-chemistry-11-sulfur-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Sulfur giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-sulfuric-acid',
    10,
    'FIVE_LESSON_REVIEW',
    'Sulfuric acid',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Sulfuric acid theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Sulfuric acid vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-sulfuric-acid-chemistry-11-sulfuric-acid-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Sulfuric acid?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-sulfuric-acid-chemistry-11-sulfuric-acid-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Sulfuric acid?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-sulfuric-acid-chemistry-11-sulfuric-acid-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Sulfuric acid giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-dai-cuong-hoa-huu-co',
    11,
    'LESSON',
    'Đại cương hóa hữu cơ',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Đại cương hóa hữu cơ theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Đại cương hóa hữu cơ vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-dai-cuong-hoa-huu-co-chemistry-11-dai-cuong-hoa-huu-co-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Đại cương hóa hữu cơ?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-dai-cuong-hoa-huu-co-chemistry-11-dai-cuong-hoa-huu-co-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Đại cương hóa hữu cơ?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-dai-cuong-hoa-huu-co-chemistry-11-dai-cuong-hoa-huu-co-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Đại cương hóa hữu cơ giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-pho-hong-ngoai-ir',
    12,
    'LESSON',
    'Phổ hồng ngoại IR',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Phổ hồng ngoại IR theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Phổ hồng ngoại IR vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-pho-hong-ngoai-ir-chemistry-11-pho-hong-ngoai-ir-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Phổ hồng ngoại IR?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-pho-hong-ngoai-ir-chemistry-11-pho-hong-ngoai-ir-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Phổ hồng ngoại IR?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-pho-hong-ngoai-ir-chemistry-11-pho-hong-ngoai-ir-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Phổ hồng ngoại IR giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-dong-dang-va-dong-phan',
    13,
    'LESSON',
    'Đồng đẳng và đồng phân',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Đồng đẳng và đồng phân theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Đồng đẳng và đồng phân vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-dong-dang-va-dong-phan-chemistry-11-dong-dang-va-dong-phan-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Đồng đẳng và đồng phân?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-dong-dang-va-dong-phan-chemistry-11-dong-dang-va-dong-phan-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Đồng đẳng và đồng phân?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-dong-dang-va-dong-phan-chemistry-11-dong-dang-va-dong-phan-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Đồng đẳng và đồng phân giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-alkane',
    14,
    'LESSON',
    'Alkane',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Alkane theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Alkane vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-alkane-chemistry-11-alkane-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Alkane?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-alkane-chemistry-11-alkane-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Alkane?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-alkane-chemistry-11-alkane-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Alkane giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-alkene-va-alkyne',
    15,
    'FIVE_LESSON_REVIEW',
    'Alkene và Alkyne',
    'Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Alkene và Alkyne theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Alkene và Alkyne vào thực tiễn."]'::jsonb,
    '[{"id": "chemistry-11-alkene-va-alkyne-chemistry-11-alkene-va-alkyne-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Alkene và Alkyne?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "chemistry-11-alkene-va-alkyne-chemistry-11-alkene-va-alkyne-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Alkene và Alkyne?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "chemistry-11-alkene-va-alkyne-chemistry-11-alkene-va-alkyne-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Alkene và Alkyne giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-canh-tranh-kinh-te',
    1,
    'LESSON',
    'Cạnh tranh kinh tế',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Cạnh tranh kinh tế theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Cạnh tranh kinh tế vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-canh-tranh-kinh-te-economics_law-11-canh-tranh-kinh-te-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Cạnh tranh kinh tế?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-canh-tranh-kinh-te-economics_law-11-canh-tranh-kinh-te-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Cạnh tranh kinh tế?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-canh-tranh-kinh-te-economics_law-11-canh-tranh-kinh-te-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Cạnh tranh kinh tế giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-cung-cau',
    2,
    'LESSON',
    'Cung cầu',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Cung cầu theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Cung cầu vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-cung-cau-economics_law-11-cung-cau-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Cung cầu?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-cung-cau-economics_law-11-cung-cau-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Cung cầu?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-cung-cau-economics_law-11-cung-cau-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Cung cầu giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-lam-phat',
    3,
    'LESSON',
    'Lạm phát',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Lạm phát theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Lạm phát vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-lam-phat-economics_law-11-lam-phat-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Lạm phát?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-lam-phat-economics_law-11-lam-phat-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Lạm phát?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-lam-phat-economics_law-11-lam-phat-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Lạm phát giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-kiem-soat-lam-phat',
    4,
    'LESSON',
    'Kiểm soát lạm phát',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Kiểm soát lạm phát theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Kiểm soát lạm phát vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-kiem-soat-lam-phat-economics_law-11-kiem-soat-lam-phat-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Kiểm soát lạm phát?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-kiem-soat-lam-phat-economics_law-11-kiem-soat-lam-phat-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Kiểm soát lạm phát?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-kiem-soat-lam-phat-economics_law-11-kiem-soat-lam-phat-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Kiểm soát lạm phát giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-that-nghiep',
    5,
    'FIVE_LESSON_REVIEW',
    'Thất nghiệp',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Thất nghiệp theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Thất nghiệp vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-that-nghiep-economics_law-11-that-nghiep-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Thất nghiệp?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-that-nghiep-economics_law-11-that-nghiep-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Thất nghiệp?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-that-nghiep-economics_law-11-that-nghiep-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Thất nghiệp giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-thi-truong-lao-dong',
    6,
    'LESSON',
    'Thị trường lao động',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Thị trường lao động theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Thị trường lao động vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-thi-truong-lao-dong-economics_law-11-thi-truong-lao-dong-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Thị trường lao động?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-thi-truong-lao-dong-economics_law-11-thi-truong-lao-dong-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Thị trường lao động?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-thi-truong-lao-dong-economics_law-11-thi-truong-lao-dong-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Thị trường lao động giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-y-tuong-kinh-doanh',
    7,
    'LESSON',
    'Ý tưởng kinh doanh',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Ý tưởng kinh doanh theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Ý tưởng kinh doanh vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-y-tuong-kinh-doanh-economics_law-11-y-tuong-kinh-doanh-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Ý tưởng kinh doanh?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-y-tuong-kinh-doanh-economics_law-11-y-tuong-kinh-doanh-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Ý tưởng kinh doanh?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-y-tuong-kinh-doanh-economics_law-11-y-tuong-kinh-doanh-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Ý tưởng kinh doanh giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-nang-luc-quan-tri',
    8,
    'LESSON',
    'Năng lực quản trị',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Năng lực quản trị theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Năng lực quản trị vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-nang-luc-quan-tri-economics_law-11-nang-luc-quan-tri-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Năng lực quản trị?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-nang-luc-quan-tri-economics_law-11-nang-luc-quan-tri-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Năng lực quản trị?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-nang-luc-quan-tri-economics_law-11-nang-luc-quan-tri-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Năng lực quản trị giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-dao-duc-kinh-doanh',
    9,
    'LESSON',
    'Đạo đức kinh doanh',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Đạo đức kinh doanh theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Đạo đức kinh doanh vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-dao-duc-kinh-doanh-economics_law-11-dao-duc-kinh-doanh-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Đạo đức kinh doanh?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-dao-duc-kinh-doanh-economics_law-11-dao-duc-kinh-doanh-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Đạo đức kinh doanh?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-dao-duc-kinh-doanh-economics_law-11-dao-duc-kinh-doanh-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Đạo đức kinh doanh giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-tieu-dung-thong-minh',
    10,
    'FIVE_LESSON_REVIEW',
    'Tiêu dùng thông minh',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Tiêu dùng thông minh theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Tiêu dùng thông minh vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-tieu-dung-thong-minh-economics_law-11-tieu-dung-thong-minh-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Tiêu dùng thông minh?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-tieu-dung-thong-minh-economics_law-11-tieu-dung-thong-minh-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Tiêu dùng thông minh?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-tieu-dung-thong-minh-economics_law-11-tieu-dung-thong-minh-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Tiêu dùng thông minh giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-quyen-binh-dang-phap-luat',
    11,
    'LESSON',
    'Quyền bình đẳng pháp luật',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Quyền bình đẳng pháp luật theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Quyền bình đẳng pháp luật vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-quyen-binh-dang-phap-luat-economics_law-11-quyen-binh-dang-phap-luat-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Quyền bình đẳng pháp luật?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-quyen-binh-dang-phap-luat-economics_law-11-quyen-binh-dang-phap-luat-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Quyền bình đẳng pháp luật?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-quyen-binh-dang-phap-luat-economics_law-11-quyen-binh-dang-phap-luat-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Quyền bình đẳng pháp luật giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-binh-dang-gioi',
    12,
    'LESSON',
    'Bình đẳng giới',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Bình đẳng giới theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Bình đẳng giới vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-binh-dang-gioi-economics_law-11-binh-dang-gioi-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Bình đẳng giới?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-binh-dang-gioi-economics_law-11-binh-dang-gioi-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Bình đẳng giới?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-binh-dang-gioi-economics_law-11-binh-dang-gioi-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Bình đẳng giới giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-binh-dang-dan-toc-ton-giao',
    13,
    'LESSON',
    'Bình đẳng dân tộc tôn giáo',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Bình đẳng dân tộc tôn giáo theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Bình đẳng dân tộc tôn giáo vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-binh-dang-dan-toc-ton-giao-economics_law-11-binh-dang-dan-toc-ton-giao-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Bình đẳng dân tộc tôn giáo?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-binh-dang-dan-toc-ton-giao-economics_law-11-binh-dang-dan-toc-ton-giao-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Bình đẳng dân tộc tôn giáo?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-binh-dang-dan-toc-ton-giao-economics_law-11-binh-dang-dan-toc-ton-giao-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Bình đẳng dân tộc tôn giáo giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-quyen-bau-cu',
    14,
    'LESSON',
    'Quyền bầu cử',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Quyền bầu cử theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Quyền bầu cử vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-quyen-bau-cu-economics_law-11-quyen-bau-cu-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Quyền bầu cử?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-quyen-bau-cu-economics_law-11-quyen-bau-cu-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Quyền bầu cử?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-quyen-bau-cu-economics_law-11-quyen-bau-cu-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Quyền bầu cử giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-nghia-vu-bao-ve-to-quoc',
    15,
    'FIVE_LESSON_REVIEW',
    'Nghĩa vụ bảo vệ Tổ quốc',
    'KTPL 11: Cạnh tranh thị trường và Quyền công dân',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Nghĩa vụ bảo vệ Tổ quốc theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Nghĩa vụ bảo vệ Tổ quốc vào thực tiễn."]'::jsonb,
    '[{"id": "economics_law-11-nghia-vu-bao-ve-to-quoc-economics_law-11-nghia-vu-bao-ve-to-quoc-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Nghĩa vụ bảo vệ Tổ quốc?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "economics_law-11-nghia-vu-bao-ve-to-quoc-economics_law-11-nghia-vu-bao-ve-to-quoc-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Nghĩa vụ bảo vệ Tổ quốc?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "economics_law-11-nghia-vu-bao-ve-to-quoc-economics_law-11-nghia-vu-bao-ve-to-quoc-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Nghĩa vụ bảo vệ Tổ quốc giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-healthy-habits',
    1,
    'LESSON',
    'Healthy habits',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Healthy habits theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Healthy habits vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-healthy-habits-english-11-healthy-habits-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Healthy habits?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-healthy-habits-english-11-healthy-habits-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Healthy habits?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-healthy-habits-english-11-healthy-habits-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Healthy habits giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-generation-gap',
    2,
    'LESSON',
    'Generation gap',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Generation gap theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Generation gap vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-generation-gap-english-11-generation-gap-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Generation gap?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-generation-gap-english-11-generation-gap-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Generation gap?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-generation-gap-english-11-generation-gap-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Generation gap giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-cities-of-the-future',
    3,
    'LESSON',
    'Cities of the future',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Cities of the future theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Cities of the future vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-cities-of-the-future-english-11-cities-of-the-future-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Cities of the future?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-cities-of-the-future-english-11-cities-of-the-future-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Cities of the future?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-cities-of-the-future-english-11-cities-of-the-future-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Cities of the future giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-asean-community',
    4,
    'LESSON',
    'ASEAN community',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài ASEAN community theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần ASEAN community vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-asean-community-english-11-asean-community-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài ASEAN community?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-asean-community-english-11-asean-community-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần ASEAN community?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-asean-community-english-11-asean-community-q3", "prompt": "Tình huống thực tế áp dụng kiến thức ASEAN community giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-global-warming',
    5,
    'FIVE_LESSON_REVIEW',
    'Global warming',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Global warming theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Global warming vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-global-warming-english-11-global-warming-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Global warming?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-global-warming-english-11-global-warming-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Global warming?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-global-warming-english-11-global-warming-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Global warming giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-preserving-heritage',
    6,
    'LESSON',
    'Preserving heritage',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Preserving heritage theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Preserving heritage vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-preserving-heritage-english-11-preserving-heritage-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Preserving heritage?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-preserving-heritage-english-11-preserving-heritage-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Preserving heritage?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-preserving-heritage-english-11-preserving-heritage-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Preserving heritage giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-higher-education-options',
    7,
    'LESSON',
    'Higher education options',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Higher education options theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Higher education options vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-higher-education-options-english-11-higher-education-options-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Higher education options?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-higher-education-options-english-11-higher-education-options-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Higher education options?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-higher-education-options-english-11-higher-education-options-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Higher education options giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-becoming-independent',
    8,
    'LESSON',
    'Becoming independent',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Becoming independent theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Becoming independent vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-becoming-independent-english-11-becoming-independent-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Becoming independent?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-becoming-independent-english-11-becoming-independent-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Becoming independent?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-becoming-independent-english-11-becoming-independent-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Becoming independent giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-green-lifestyle',
    9,
    'LESSON',
    'Green lifestyle',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Green lifestyle theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Green lifestyle vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-green-lifestyle-english-11-green-lifestyle-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Green lifestyle?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-green-lifestyle-english-11-green-lifestyle-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Green lifestyle?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-green-lifestyle-english-11-green-lifestyle-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Green lifestyle giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-electronic-devices',
    10,
    'FIVE_LESSON_REVIEW',
    'Electronic devices',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Electronic devices theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Electronic devices vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-electronic-devices-english-11-electronic-devices-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Electronic devices?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-electronic-devices-english-11-electronic-devices-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Electronic devices?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-electronic-devices-english-11-electronic-devices-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Electronic devices giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-the-world-of-work',
    11,
    'LESSON',
    'The world of work',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài The world of work theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần The world of work vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-the-world-of-work-english-11-the-world-of-work-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài The world of work?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-the-world-of-work-english-11-the-world-of-work-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần The world of work?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-the-world-of-work-english-11-the-world-of-work-q3", "prompt": "Tình huống thực tế áp dụng kiến thức The world of work giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-lifelong-learning',
    12,
    'LESSON',
    'Lifelong learning',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Lifelong learning theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Lifelong learning vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-lifelong-learning-english-11-lifelong-learning-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Lifelong learning?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-lifelong-learning-english-11-lifelong-learning-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Lifelong learning?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-lifelong-learning-english-11-lifelong-learning-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Lifelong learning giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-cultural-diversity',
    13,
    'LESSON',
    'Cultural diversity',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Cultural diversity theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Cultural diversity vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-cultural-diversity-english-11-cultural-diversity-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Cultural diversity?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-cultural-diversity-english-11-cultural-diversity-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Cultural diversity?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-cultural-diversity-english-11-cultural-diversity-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Cultural diversity giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-environmental-protection',
    14,
    'LESSON',
    'Environmental protection',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Environmental protection theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Environmental protection vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-environmental-protection-english-11-environmental-protection-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Environmental protection?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-environmental-protection-english-11-environmental-protection-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Environmental protection?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-environmental-protection-english-11-environmental-protection-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Environmental protection giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-ai-in-education',
    15,
    'FIVE_LESSON_REVIEW',
    'AI in education',
    'Tiếng Anh 11: Global Success & Lifelong Learning',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài AI in education theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần AI in education vào thực tiễn."]'::jsonb,
    '[{"id": "english-11-ai-in-education-english-11-ai-in-education-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài AI in education?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "english-11-ai-in-education-english-11-ai-in-education-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần AI in education?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "english-11-ai-in-education-english-11-ai-in-education-q3", "prompt": "Tình huống thực tế áp dụng kiến thức AI in education giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-phat-trien-kinh-te-giua-cac-nuoc',
    1,
    'LESSON',
    'Phát triển kinh tế giữa các nước',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Phát triển kinh tế giữa các nước theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Phát triển kinh tế giữa các nước vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-phat-trien-kinh-te-giua-cac-nuoc-geography-11-phat-trien-kinh-te-giua-cac-nuoc-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Phát triển kinh tế giữa các nước?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-phat-trien-kinh-te-giua-cac-nuoc-geography-11-phat-trien-kinh-te-giua-cac-nuoc-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Phát triển kinh tế giữa các nước?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-phat-trien-kinh-te-giua-cac-nuoc-geography-11-phat-trien-kinh-te-giua-cac-nuoc-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Phát triển kinh tế giữa các nước giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-toan-cau-hoa',
    2,
    'LESSON',
    'Toàn cầu hóa',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Toàn cầu hóa theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Toàn cầu hóa vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-toan-cau-hoa-geography-11-toan-cau-hoa-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Toàn cầu hóa?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-toan-cau-hoa-geography-11-toan-cau-hoa-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Toàn cầu hóa?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-toan-cau-hoa-geography-11-toan-cau-hoa-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Toàn cầu hóa giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-to-chuc-lien-ket',
    3,
    'LESSON',
    'Tổ chức liên kết',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Tổ chức liên kết theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Tổ chức liên kết vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-to-chuc-lien-ket-geography-11-to-chuc-lien-ket-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Tổ chức liên kết?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-to-chuc-lien-ket-geography-11-to-chuc-lien-ket-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Tổ chức liên kết?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-to-chuc-lien-ket-geography-11-to-chuc-lien-ket-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Tổ chức liên kết giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-an-ninh-luong-thuc',
    4,
    'LESSON',
    'An ninh lương thực',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài An ninh lương thực theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần An ninh lương thực vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-an-ninh-luong-thuc-geography-11-an-ninh-luong-thuc-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài An ninh lương thực?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-an-ninh-luong-thuc-geography-11-an-ninh-luong-thuc-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần An ninh lương thực?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-an-ninh-luong-thuc-geography-11-an-ninh-luong-thuc-q3", "prompt": "Tình huống thực tế áp dụng kiến thức An ninh lương thực giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-an-ninh-nang-luong',
    5,
    'FIVE_LESSON_REVIEW',
    'An ninh năng lượng',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài An ninh năng lượng theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần An ninh năng lượng vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-an-ninh-nang-luong-geography-11-an-ninh-nang-luong-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài An ninh năng lượng?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-an-ninh-nang-luong-geography-11-an-ninh-nang-luong-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần An ninh năng lượng?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-an-ninh-nang-luong-geography-11-an-ninh-nang-luong-q3", "prompt": "Tình huống thực tế áp dụng kiến thức An ninh năng lượng giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-tu-nhien-my-latinh',
    6,
    'LESSON',
    'Tự nhiên Mỹ Latinh',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Tự nhiên Mỹ Latinh theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Tự nhiên Mỹ Latinh vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-tu-nhien-my-latinh-geography-11-tu-nhien-my-latinh-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Tự nhiên Mỹ Latinh?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-tu-nhien-my-latinh-geography-11-tu-nhien-my-latinh-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Tự nhiên Mỹ Latinh?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-tu-nhien-my-latinh-geography-11-tu-nhien-my-latinh-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Tự nhiên Mỹ Latinh giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-xa-hoi-my-latinh',
    7,
    'LESSON',
    'Xã hội Mỹ Latinh',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Xã hội Mỹ Latinh theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Xã hội Mỹ Latinh vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-xa-hoi-my-latinh-geography-11-xa-hoi-my-latinh-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Xã hội Mỹ Latinh?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-xa-hoi-my-latinh-geography-11-xa-hoi-my-latinh-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Xã hội Mỹ Latinh?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-xa-hoi-my-latinh-geography-11-xa-hoi-my-latinh-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Xã hội Mỹ Latinh giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-kinh-te-my-latinh',
    8,
    'LESSON',
    'Kinh tế Mỹ Latinh',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Kinh tế Mỹ Latinh theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Kinh tế Mỹ Latinh vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-kinh-te-my-latinh-geography-11-kinh-te-my-latinh-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Kinh tế Mỹ Latinh?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-kinh-te-my-latinh-geography-11-kinh-te-my-latinh-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Kinh tế Mỹ Latinh?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-kinh-te-my-latinh-geography-11-kinh-te-my-latinh-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Kinh tế Mỹ Latinh giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-lien-minh-chau-au-eu',
    9,
    'LESSON',
    'Liên minh châu Âu EU',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Liên minh châu Âu EU theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Liên minh châu Âu EU vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-lien-minh-chau-au-eu-geography-11-lien-minh-chau-au-eu-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Liên minh châu Âu EU?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-lien-minh-chau-au-eu-geography-11-lien-minh-chau-au-eu-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Liên minh châu Âu EU?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-lien-minh-chau-au-eu-geography-11-lien-minh-chau-au-eu-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Liên minh châu Âu EU giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-thi-truong-chung-euro',
    10,
    'FIVE_LESSON_REVIEW',
    'Thị trường chung Euro',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Thị trường chung Euro theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Thị trường chung Euro vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-thi-truong-chung-euro-geography-11-thi-truong-chung-euro-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Thị trường chung Euro?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-thi-truong-chung-euro-geography-11-thi-truong-chung-euro-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Thị trường chung Euro?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-thi-truong-chung-euro-geography-11-thi-truong-chung-euro-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Thị trường chung Euro giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-tu-nhien-dong-nam-a',
    11,
    'LESSON',
    'Tự nhiên Đông Nam Á',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Tự nhiên Đông Nam Á theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Tự nhiên Đông Nam Á vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-tu-nhien-dong-nam-a-geography-11-tu-nhien-dong-nam-a-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Tự nhiên Đông Nam Á?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-tu-nhien-dong-nam-a-geography-11-tu-nhien-dong-nam-a-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Tự nhiên Đông Nam Á?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-tu-nhien-dong-nam-a-geography-11-tu-nhien-dong-nam-a-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Tự nhiên Đông Nam Á giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-dan-cu-dong-nam-a',
    12,
    'LESSON',
    'Dân cư Đông Nam Á',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Dân cư Đông Nam Á theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Dân cư Đông Nam Á vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-dan-cu-dong-nam-a-geography-11-dan-cu-dong-nam-a-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Dân cư Đông Nam Á?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-dan-cu-dong-nam-a-geography-11-dan-cu-dong-nam-a-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Dân cư Đông Nam Á?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-dan-cu-dong-nam-a-geography-11-dan-cu-dong-nam-a-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Dân cư Đông Nam Á giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-kinh-te-dong-nam-a',
    13,
    'LESSON',
    'Kinh tế Đông Nam Á',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Kinh tế Đông Nam Á theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Kinh tế Đông Nam Á vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-kinh-te-dong-nam-a-geography-11-kinh-te-dong-nam-a-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Kinh tế Đông Nam Á?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-kinh-te-dong-nam-a-geography-11-kinh-te-dong-nam-a-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Kinh tế Đông Nam Á?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-kinh-te-dong-nam-a-geography-11-kinh-te-dong-nam-a-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Kinh tế Đông Nam Á giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-asean',
    14,
    'LESSON',
    'ASEAN',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài ASEAN theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần ASEAN vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-asean-geography-11-asean-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài ASEAN?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-asean-geography-11-asean-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần ASEAN?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-asean-geography-11-asean-q3", "prompt": "Tình huống thực tế áp dụng kiến thức ASEAN giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-vi-the-hoa-ky-nhat-ban-trung-quoc',
    15,
    'FIVE_LESSON_REVIEW',
    'Vị thế Hoa Kỳ Nhật Bản Trung Quốc',
    'Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Vị thế Hoa Kỳ Nhật Bản Trung Quốc theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Vị thế Hoa Kỳ Nhật Bản Trung Quốc vào thực tiễn."]'::jsonb,
    '[{"id": "geography-11-vi-the-hoa-ky-nhat-ban-trung-quoc-geography-11-vi-the-hoa-ky-nhat-ban-trung-quoc-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Vị thế Hoa Kỳ Nhật Bản Trung Quốc?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "geography-11-vi-the-hoa-ky-nhat-ban-trung-quoc-geography-11-vi-the-hoa-ky-nhat-ban-trung-quoc-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Vị thế Hoa Kỳ Nhật Bản Trung Quốc?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "geography-11-vi-the-hoa-ky-nhat-ban-trung-quoc-geography-11-vi-the-hoa-ky-nhat-ban-trung-quoc-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Vị thế Hoa Kỳ Nhật Bản Trung Quốc giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-cach-mang-tu-san-anh-phap',
    1,
    'LESSON',
    'Cách mạng tư sản Anh Pháp',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Cách mạng tư sản Anh Pháp theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Cách mạng tư sản Anh Pháp vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-cach-mang-tu-san-anh-phap-history-11-cach-mang-tu-san-anh-phap-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Cách mạng tư sản Anh Pháp?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-cach-mang-tu-san-anh-phap-history-11-cach-mang-tu-san-anh-phap-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Cách mạng tư sản Anh Pháp?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-cach-mang-tu-san-anh-phap-history-11-cach-mang-tu-san-anh-phap-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Cách mạng tư sản Anh Pháp giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-su-xac-lap-tbcn',
    2,
    'LESSON',
    'Sự xác lập TBCN',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Sự xác lập TBCN theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Sự xác lập TBCN vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-su-xac-lap-tbcn-history-11-su-xac-lap-tbcn-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Sự xác lập TBCN?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-su-xac-lap-tbcn-history-11-su-xac-lap-tbcn-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Sự xác lập TBCN?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-su-xac-lap-tbcn-history-11-su-xac-lap-tbcn-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Sự xác lập TBCN giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-cntb-hien-dai',
    3,
    'LESSON',
    'CNTB hiện đại',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài CNTB hiện đại theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần CNTB hiện đại vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-cntb-hien-dai-history-11-cntb-hien-dai-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài CNTB hiện đại?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-cntb-hien-dai-history-11-cntb-hien-dai-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần CNTB hiện đại?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-cntb-hien-dai-history-11-cntb-hien-dai-q3", "prompt": "Tình huống thực tế áp dụng kiến thức CNTB hiện đại giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-lien-bang-xo-viet',
    4,
    'LESSON',
    'Liên bang Xô viết',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Liên bang Xô viết theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Liên bang Xô viết vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-lien-bang-xo-viet-history-11-lien-bang-xo-viet-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Liên bang Xô viết?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-lien-bang-xo-viet-history-11-lien-bang-xo-viet-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Liên bang Xô viết?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-lien-bang-xo-viet-history-11-lien-bang-xo-viet-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Liên bang Xô viết giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-y-nghia-cach-mang-thang-muoi',
    5,
    'FIVE_LESSON_REVIEW',
    'Ý nghĩa Cách mạng tháng Mười',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Ý nghĩa Cách mạng tháng Mười theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Ý nghĩa Cách mạng tháng Mười vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-y-nghia-cach-mang-thang-muoi-history-11-y-nghia-cach-mang-thang-muoi-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Ý nghĩa Cách mạng tháng Mười?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-y-nghia-cach-mang-thang-muoi-history-11-y-nghia-cach-mang-thang-muoi-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Ý nghĩa Cách mạng tháng Mười?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-y-nghia-cach-mang-thang-muoi-history-11-y-nghia-cach-mang-thang-muoi-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Ý nghĩa Cách mạng tháng Mười giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-thuc-dan-xam-luoc-dna',
    6,
    'LESSON',
    'Thực dân xâm lược ĐNA',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Thực dân xâm lược ĐNA theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Thực dân xâm lược ĐNA vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-thuc-dan-xam-luoc-dna-history-11-thuc-dan-xam-luoc-dna-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Thực dân xâm lược ĐNA?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-thuc-dan-xam-luoc-dna-history-11-thuc-dan-xam-luoc-dna-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Thực dân xâm lược ĐNA?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-thuc-dan-xam-luoc-dna-history-11-thuc-dan-xam-luoc-dna-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Thực dân xâm lược ĐNA giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-giai-phong-dan-toc-dna',
    7,
    'LESSON',
    'Giải phóng dân tộc ĐNA',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Giải phóng dân tộc ĐNA theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Giải phóng dân tộc ĐNA vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-giai-phong-dan-toc-dna-history-11-giai-phong-dan-toc-dna-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Giải phóng dân tộc ĐNA?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-giai-phong-dan-toc-dna-history-11-giai-phong-dan-toc-dna-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Giải phóng dân tộc ĐNA?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-giai-phong-dan-toc-dna-history-11-giai-phong-dan-toc-dna-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Giải phóng dân tộc ĐNA giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-cttg-thu-nhat',
    8,
    'LESSON',
    'CTTG thứ nhất',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài CTTG thứ nhất theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần CTTG thứ nhất vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-cttg-thu-nhat-history-11-cttg-thu-nhat-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài CTTG thứ nhất?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-cttg-thu-nhat-history-11-cttg-thu-nhat-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần CTTG thứ nhất?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-cttg-thu-nhat-history-11-cttg-thu-nhat-q3", "prompt": "Tình huống thực tế áp dụng kiến thức CTTG thứ nhất giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-cttg-thu-hai',
    9,
    'LESSON',
    'CTTG thứ hai',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài CTTG thứ hai theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần CTTG thứ hai vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-cttg-thu-hai-history-11-cttg-thu-hai-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài CTTG thứ hai?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-cttg-thu-hai-history-11-cttg-thu-hai-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần CTTG thứ hai?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-cttg-thu-hai-history-11-cttg-thu-hai-q3", "prompt": "Tình huống thực tế áp dụng kiến thức CTTG thứ hai giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-trat-tu-hai-cuc-i-an-ta',
    10,
    'FIVE_LESSON_REVIEW',
    'Trật tự hai cực I-an-ta',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Trật tự hai cực I-an-ta theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Trật tự hai cực I-an-ta vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-trat-tu-hai-cuc-i-an-ta-history-11-trat-tu-hai-cuc-i-an-ta-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Trật tự hai cực I-an-ta?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-trat-tu-hai-cuc-i-an-ta-history-11-trat-tu-hai-cuc-i-an-ta-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Trật tự hai cực I-an-ta?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-trat-tu-hai-cuc-i-an-ta-history-11-trat-tu-hai-cuc-i-an-ta-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Trật tự hai cực I-an-ta giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-van-minh-dna-co-trung-dai',
    11,
    'LESSON',
    'Văn minh ĐNA cổ trung đại',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Văn minh ĐNA cổ trung đại theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Văn minh ĐNA cổ trung đại vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-van-minh-dna-co-trung-dai-history-11-van-minh-dna-co-trung-dai-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Văn minh ĐNA cổ trung đại?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-van-minh-dna-co-trung-dai-history-11-van-minh-dna-co-trung-dai-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Văn minh ĐNA cổ trung đại?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-van-minh-dna-co-trung-dai-history-11-van-minh-dna-co-trung-dai-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Văn minh ĐNA cổ trung đại giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-van-minh-van-lang',
    12,
    'LESSON',
    'Văn minh Văn Lang',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Văn minh Văn Lang theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Văn minh Văn Lang vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-van-minh-van-lang-history-11-van-minh-van-lang-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Văn minh Văn Lang?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-van-minh-van-lang-history-11-van-minh-van-lang-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Văn minh Văn Lang?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-van-minh-van-lang-history-11-van-minh-van-lang-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Văn minh Văn Lang giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-van-minh-champa-phu-nam',
    13,
    'LESSON',
    'Văn minh Champa Phù Nam',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Văn minh Champa Phù Nam theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Văn minh Champa Phù Nam vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-van-minh-champa-phu-nam-history-11-van-minh-champa-phu-nam-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Văn minh Champa Phù Nam?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-van-minh-champa-phu-nam-history-11-van-minh-champa-phu-nam-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Văn minh Champa Phù Nam?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-van-minh-champa-phu-nam-history-11-van-minh-champa-phu-nam-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Văn minh Champa Phù Nam giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-van-minh-dai-viet',
    14,
    'LESSON',
    'Văn minh Đại Việt',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Văn minh Đại Việt theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Văn minh Đại Việt vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-van-minh-dai-viet-history-11-van-minh-dai-viet-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Văn minh Đại Việt?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-van-minh-dai-viet-history-11-van-minh-dai-viet-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Văn minh Đại Việt?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-van-minh-dai-viet-history-11-van-minh-dai-viet-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Văn minh Đại Việt giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-bao-ton-di-san',
    15,
    'FIVE_LESSON_REVIEW',
    'Bảo tồn di sản',
    'Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Bảo tồn di sản theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Bảo tồn di sản vào thực tiễn."]'::jsonb,
    '[{"id": "history-11-bao-ton-di-san-history-11-bao-ton-di-san-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Bảo tồn di sản?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "history-11-bao-ton-di-san-history-11-bao-ton-di-san-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Bảo tồn di sản?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "history-11-bao-ton-di-san-history-11-bao-ton-di-san-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Bảo tồn di sản giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-cau-chuyen-va-diem-nhin',
    1,
    'LESSON',
    'Câu chuyện và điểm nhìn',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Câu chuyện và điểm nhìn theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Câu chuyện và điểm nhìn vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-cau-chuyen-va-diem-nhin-literature-11-cau-chuyen-va-diem-nhin-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Câu chuyện và điểm nhìn?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-cau-chuyen-va-diem-nhin-literature-11-cau-chuyen-va-diem-nhin-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Câu chuyện và điểm nhìn?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-cau-chuyen-va-diem-nhin-literature-11-cau-chuyen-va-diem-nhin-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Câu chuyện và điểm nhìn giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-nhan-vat-truyen-ngan',
    2,
    'LESSON',
    'Nhân vật truyện ngắn',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Nhân vật truyện ngắn theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Nhân vật truyện ngắn vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-nhan-vat-truyen-ngan-literature-11-nhan-vat-truyen-ngan-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Nhân vật truyện ngắn?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-nhan-vat-truyen-ngan-literature-11-nhan-vat-truyen-ngan-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Nhân vật truyện ngắn?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-nhan-vat-truyen-ngan-literature-11-nhan-vat-truyen-ngan-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Nhân vật truyện ngắn giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-lap-cau-truc',
    3,
    'LESSON',
    'Lặp cấu trúc',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Lặp cấu trúc theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Lặp cấu trúc vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-lap-cau-truc-literature-11-lap-cau-truc-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Lặp cấu trúc?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-lap-cau-truc-literature-11-lap-cau-truc-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Lặp cấu trúc?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-lap-cau-truc-literature-11-lap-cau-truc-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Lặp cấu trúc giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-cam-xuc-trong-tho',
    4,
    'LESSON',
    'Cảm xúc trong thơ',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Cảm xúc trong thơ theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Cảm xúc trong thơ vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-cam-xuc-trong-tho-literature-11-cam-xuc-trong-tho-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Cảm xúc trong thơ?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-cam-xuc-trong-tho-literature-11-cam-xuc-trong-tho-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Cảm xúc trong thơ?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-cam-xuc-trong-tho-literature-11-cam-xuc-trong-tho-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Cảm xúc trong thơ giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-ngon-ngu-van-hoc',
    5,
    'FIVE_LESSON_REVIEW',
    'Ngôn ngữ văn học',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Ngôn ngữ văn học theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Ngôn ngữ văn học vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-ngon-ngu-van-hoc-literature-11-ngon-ngu-van-hoc-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Ngôn ngữ văn học?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-ngon-ngu-van-hoc-literature-11-ngon-ngu-van-hoc-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Ngôn ngữ văn học?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-ngon-ngu-van-hoc-literature-11-ngon-ngu-van-hoc-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Ngôn ngữ văn học giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-tuy-but-va-tan-van',
    6,
    'LESSON',
    'Tùy bút và tản văn',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Tùy bút và tản văn theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Tùy bút và tản văn vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-tuy-but-va-tan-van-literature-11-tuy-but-va-tan-van-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Tùy bút và tản văn?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-tuy-but-va-tan-van-literature-11-tuy-but-va-tan-van-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Tùy bút và tản văn?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-tuy-but-va-tan-van-literature-11-tuy-but-va-tan-van-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Tùy bút và tản văn giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-luan-diem-va-bang-chung',
    7,
    'LESSON',
    'Luận điểm và bằng chứng',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Luận điểm và bằng chứng theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Luận điểm và bằng chứng vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-luan-diem-va-bang-chung-literature-11-luan-diem-va-bang-chung-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Luận điểm và bằng chứng?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-luan-diem-va-bang-chung-literature-11-luan-diem-va-bang-chung-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Luận điểm và bằng chứng?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-luan-diem-va-bang-chung-literature-11-luan-diem-va-bang-chung-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Luận điểm và bằng chứng giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-chem-xen-va-noi-giam',
    8,
    'LESSON',
    'Chêm xen và nói giảm',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Chêm xen và nói giảm theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Chêm xen và nói giảm vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-chem-xen-va-noi-giam-literature-11-chem-xen-va-noi-giam-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Chêm xen và nói giảm?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-chem-xen-va-noi-giam-literature-11-chem-xen-va-noi-giam-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Chêm xen và nói giảm?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-chem-xen-va-noi-giam-literature-11-chem-xen-va-noi-giam-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Chêm xen và nói giảm giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-kich-ban-van-hoc',
    9,
    'LESSON',
    'Kịch bản văn học',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Kịch bản văn học theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Kịch bản văn học vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-kich-ban-van-hoc-literature-11-kich-ban-van-hoc-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Kịch bản văn học?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-kich-ban-van-hoc-literature-11-kich-ban-van-hoc-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Kịch bản văn học?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-kich-ban-van-hoc-literature-11-kich-ban-van-hoc-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Kịch bản văn học giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-ngon-ngu-hanh-dong',
    10,
    'FIVE_LESSON_REVIEW',
    'Ngôn ngữ hành động',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Ngôn ngữ hành động theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Ngôn ngữ hành động vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-ngon-ngu-hanh-dong-literature-11-ngon-ngu-hanh-dong-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Ngôn ngữ hành động?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-ngon-ngu-hanh-dong-literature-11-ngon-ngu-hanh-dong-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Ngôn ngữ hành động?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-ngon-ngu-hanh-dong-literature-11-ngon-ngu-hanh-dong-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Ngôn ngữ hành động giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-van-ban-thong-tin',
    11,
    'LESSON',
    'Văn bản thông tin',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Văn bản thông tin theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Văn bản thông tin vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-van-ban-thong-tin-literature-11-van-ban-thong-tin-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Văn bản thông tin?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-van-ban-thong-tin-literature-11-van-ban-thong-tin-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Văn bản thông tin?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-van-ban-thong-tin-literature-11-van-ban-thong-tin-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Văn bản thông tin giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-thuyet-minh-hien-tuong',
    12,
    'LESSON',
    'Thuyết minh hiện tượng',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Thuyết minh hiện tượng theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Thuyết minh hiện tượng vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-thuyet-minh-hien-tuong-literature-11-thuyet-minh-hien-tuong-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Thuyết minh hiện tượng?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-thuyet-minh-hien-tuong-literature-11-thuyet-minh-hien-tuong-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Thuyết minh hiện tượng?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-thuyet-minh-hien-tuong-literature-11-thuyet-minh-hien-tuong-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Thuyết minh hiện tượng giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-viet-bai-so-sanh',
    13,
    'LESSON',
    'Viết bài so sánh',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Viết bài so sánh theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Viết bài so sánh vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-viet-bai-so-sanh-literature-11-viet-bai-so-sanh-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Viết bài so sánh?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-viet-bai-so-sanh-literature-11-viet-bai-so-sanh-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Viết bài so sánh?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-viet-bai-so-sanh-literature-11-viet-bai-so-sanh-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Viết bài so sánh giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-ki-nang-phan-bien',
    14,
    'LESSON',
    'Kĩ năng phản biện',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Kĩ năng phản biện theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Kĩ năng phản biện vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-ki-nang-phan-bien-literature-11-ki-nang-phan-bien-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Kĩ năng phản biện?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-ki-nang-phan-bien-literature-11-ki-nang-phan-bien-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Kĩ năng phản biện?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-ki-nang-phan-bien-literature-11-ki-nang-phan-bien-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Kĩ năng phản biện giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-doc-hieu-van-hoc-trung-dai',
    15,
    'FIVE_LESSON_REVIEW',
    'Đọc hiểu văn học trung đại',
    'Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Đọc hiểu văn học trung đại theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Đọc hiểu văn học trung đại vào thực tiễn."]'::jsonb,
    '[{"id": "literature-11-doc-hieu-van-hoc-trung-dai-literature-11-doc-hieu-van-hoc-trung-dai-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Đọc hiểu văn học trung đại?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "literature-11-doc-hieu-van-hoc-trung-dai-literature-11-doc-hieu-van-hoc-trung-dai-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Đọc hiểu văn học trung đại?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "literature-11-doc-hieu-van-hoc-trung-dai-literature-11-doc-hieu-van-hoc-trung-dai-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Đọc hiểu văn học trung đại giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-ham-so-luong-giac',
    1,
    'LESSON',
    'Hàm số lượng giác và đồ thị',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Hàm y=sinx, y=cosx có tập xác định R, tập giá trị [-1; 1]. Hàm sinx là hàm lẻ, cosx là hàm chẵn. Chu kì tuần hoàn của sinx và cosx là 2π."]'::jsonb,
    '["GTLN của y = 2sinx + 1 là 3, đạt được khi sinx = 1."]'::jsonb,
    '[{"id": "math-11-ham-so-luong-giac-q1", "difficulty": "EASY", "prompt": "Tập giá trị của hàm số y = cos x là?", "options": [{"id": "a", "text": "[-1; 1]"}, {"id": "b", "text": "(0; 1)"}, {"id": "c", "text": "R"}], "correctOptionId": "a", "explanation": "Hàm cosx luôn nhận giá trị từ -1 đến 1."}, {"id": "math-11-ham-so-luong-giac-q2", "difficulty": "MEDIUM", "prompt": "Chu kì tuần hoàn của hàm số y = tan x là?", "options": [{"id": "a", "text": "2π"}, {"id": "b", "text": "π"}, {"id": "c", "text": "π/2"}], "correctOptionId": "b", "explanation": "Hàm số y = tan x tuần hoàn với chu kì cơ sở T = π."}, {"id": "math-11-ham-so-luong-giac-q3", "difficulty": "APPLIED", "prompt": "Tìm GTLN của y = 3 - 2cos(x).", "options": [{"id": "a", "text": "1"}, {"id": "b", "text": "5"}, {"id": "c", "text": "3"}], "correctOptionId": "b", "explanation": "Ta có -1 ≤ cosx ≤ 1, do đó GTLN của -2cosx là 2. Suy ra y ≤ 3 + 2 = 5."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-phuong-trinh-luong-giac',
    2,
    'LESSON',
    'Phương trình lượng giác cơ bản',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Phương trình sinx = sina có nghiệm x = a + k2π hoặc x = π - a + k2π (k thuộc Z).", "cosx = cosa có nghiệm x = ±a + k2π."]'::jsonb,
    '["Giải PT sinx = 1/2. Vì sin(π/6) = 1/2 nên x = π/6 + k2π hoặc x = 5π/6 + k2π."]'::jsonb,
    '[{"id": "math-11-phuong-trinh-luong-giac-q1", "difficulty": "EASY", "prompt": "Nghiệm của phương trình cosx = 1 là?", "options": [{"id": "a", "text": "x = kπ"}, {"id": "b", "text": "x = k2π"}, {"id": "c", "text": "x = π/2 + kπ"}], "correctOptionId": "b", "explanation": "cos x = 1 tại các điểm x = k2π trên đường tròn lượng giác."}, {"id": "math-11-phuong-trinh-luong-giac-q2", "difficulty": "MEDIUM", "prompt": "Nghiệm của phương trình sinx = 0 là?", "options": [{"id": "a", "text": "x = kπ"}, {"id": "b", "text": "x = k2π"}, {"id": "c", "text": "Vô nghiệm"}], "correctOptionId": "a", "explanation": "sin x = 0 tại hai điểm 0 và π trên vòng tròn, tương đương x = kπ."}, {"id": "math-11-phuong-trinh-luong-giac-q3", "difficulty": "APPLIED", "prompt": "Số nghiệm của PT sinx = 1/2 trên đoạn [0; 2π] là?", "options": [{"id": "a", "text": "1"}, {"id": "b", "text": "2"}, {"id": "c", "text": "4"}], "correctOptionId": "b", "explanation": "Trên [0; 2π], sinx = 1/2 có 2 nghiệm là π/6 và 5π/6."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-day-so',
    3,
    'LESSON',
    'Dãy số',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Dãy số có thể cho bằng công thức tổng quát un = f(n) hoặc bằng hệ thức truy hồi.", "Dãy số tăng nếu un+1 > un, giảm nếu un+1 < un với mọi n."]'::jsonb,
    '["Xét dãy un = 2n + 1. Ta có un+1 - un = 2(n+1)+1 - (2n+1) = 2 > 0, dãy số tăng."]'::jsonb,
    '[{"id": "math-11-day-so-q1", "difficulty": "EASY", "prompt": "Cho dãy số un = 3n - 1. Số hạng u2 là?", "options": [{"id": "a", "text": "2"}, {"id": "b", "text": "5"}, {"id": "c", "text": "8"}], "correctOptionId": "b", "explanation": "Thay n = 2 vào công thức: u2 = 3(2) - 1 = 5."}, {"id": "math-11-day-so-q2", "difficulty": "MEDIUM", "prompt": "Dãy số un = 1/n là dãy số?", "options": [{"id": "a", "text": "Tăng"}, {"id": "b", "text": "Giảm"}, {"id": "c", "text": "Không đổi"}], "correctOptionId": "b", "explanation": "un+1 - un = 1/(n+1) - 1/n < 0 với mọi n nguyên dương, nên dãy số giảm."}, {"id": "math-11-day-so-q3", "difficulty": "APPLIED", "prompt": "Dãy số truy hồi u1 = 2, un+1 = 3un. Tìm u3?", "options": [{"id": "a", "text": "18"}, {"id": "b", "text": "6"}, {"id": "c", "text": "12"}], "correctOptionId": "a", "explanation": "u2 = 3(u1) = 6. u3 = 3(u2) = 18."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-cap-so-cong',
    4,
    'LESSON',
    'Cấp số cộng',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Cấp số cộng (CSC) là dãy số thỏa mãn un+1 = un + d (d là công sai).", "Số hạng tổng quát: un = u1 + (n-1)d.", "Tổng n số hạng đầu: Sn = n(u1 + un)/2."]'::jsonb,
    '["CSC có u1=2, d=3 thì u4 = 2 + (4-1)3 = 11."]'::jsonb,
    '[{"id": "math-11-cap-so-cong-q1", "difficulty": "EASY", "prompt": "Cho CSC có u1 = 1, d = 2. Tìm u3?", "options": [{"id": "a", "text": "3"}, {"id": "b", "text": "5"}, {"id": "c", "text": "7"}], "correctOptionId": "b", "explanation": "u3 = u1 + 2d = 1 + 2(2) = 5."}, {"id": "math-11-cap-so-cong-q2", "difficulty": "MEDIUM", "prompt": "CSC có u2 = 5, u4 = 11. Tìm d?", "options": [{"id": "a", "text": "2"}, {"id": "b", "text": "3"}, {"id": "c", "text": "4"}], "correctOptionId": "b", "explanation": "Ta có u4 - u2 = 2d <=> 11 - 5 = 2d => d = 3."}, {"id": "math-11-cap-so-cong-q3", "difficulty": "APPLIED", "prompt": "Tính tổng 10 số hạng đầu của CSC có u1 = 2, d = 3.", "options": [{"id": "a", "text": "155"}, {"id": "b", "text": "165"}, {"id": "c", "text": "290"}], "correctOptionId": "a", "explanation": "S10 = 10[2u1 + 9d]/2 = 5[4 + 27] = 5*31 = 155."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-cap-so-nhan',
    5,
    'FIVE_LESSON_REVIEW',
    'Cấp số nhân',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Cấp số nhân (CSN) là dãy thỏa mãn un+1 = un * q (q là công bội).", "Số hạng tổng quát: un = u1 * q^(n-1)."]'::jsonb,
    '["CSN có u1=2, q=3 thì u3 = 2 * 3^2 = 18."]'::jsonb,
    '[{"id": "math-11-cap-so-nhan-q1", "difficulty": "EASY", "prompt": "Cho CSN có u1 = 3, q = 2. Tìm u2?", "options": [{"id": "a", "text": "5"}, {"id": "b", "text": "6"}, {"id": "c", "text": "9"}], "correctOptionId": "b", "explanation": "u2 = u1 * q = 3 * 2 = 6."}, {"id": "math-11-cap-so-nhan-q2", "difficulty": "MEDIUM", "prompt": "CSN có u1 = -1, q = -2. Số hạng u4 là?", "options": [{"id": "a", "text": "8"}, {"id": "b", "text": "-8"}, {"id": "c", "text": "16"}], "correctOptionId": "a", "explanation": "u4 = u1 * q^3 = (-1) * (-2)^3 = (-1) * (-8) = 8."}, {"id": "math-11-cap-so-nhan-q3", "difficulty": "APPLIED", "prompt": "Tính tổng 5 số hạng đầu của CSN có u1 = 1, q = 2.", "options": [{"id": "a", "text": "15"}, {"id": "b", "text": "31"}, {"id": "c", "text": "63"}], "correctOptionId": "b", "explanation": "S5 = u1(1-q^5)/(1-q) = 1(1-32)/(1-2) = 31."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-gioi-han-day-so',
    6,
    'LESSON',
    'Giới hạn của dãy số',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Giới hạn của dãy số đánh giá xu hướng của un khi n tiến tới vô cực.", "lim(1/n) = 0. lim(q^n) = 0 nếu |q| < 1."]'::jsonb,
    '["lim (2n+1)/(n+2) = lim (2 + 1/n)/(1 + 2/n) = 2/1 = 2."]'::jsonb,
    '[{"id": "math-11-gioi-han-day-so-q1", "difficulty": "EASY", "prompt": "Giá trị của lim (1/n) bằng?", "options": [{"id": "a", "text": "0"}, {"id": "b", "text": "1"}, {"id": "c", "text": "+∞"}], "correctOptionId": "a", "explanation": "Khi n tiến tới vô cực, 1 chia cho số vô cùng lớn sẽ tiến tới 0."}, {"id": "math-11-gioi-han-day-so-q2", "difficulty": "MEDIUM", "prompt": "Tính lim (3n^2 - 1)/(n^2 + 2).", "options": [{"id": "a", "text": "0"}, {"id": "b", "text": "3"}, {"id": "c", "text": "1"}], "correctOptionId": "b", "explanation": "Chia cả tử và mẫu cho n^2, ta được lim (3 - 1/n^2)/(1 + 2/n^2) = 3."}, {"id": "math-11-gioi-han-day-so-q3", "difficulty": "APPLIED", "prompt": "Tính tổng CSN lùi vô hạn: 1 + 1/2 + 1/4 + ...", "options": [{"id": "a", "text": "2"}, {"id": "b", "text": "1.5"}, {"id": "c", "text": "Vô cực"}], "correctOptionId": "a", "explanation": "S = u1/(1-q) = 1 / (1 - 1/2) = 2."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-gioi-han-ham-so',
    7,
    'LESSON',
    'Giới hạn của hàm số và hàm liên tục',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Giới hạn hàm số xét sự hội tụ của f(x) khi x dần tới x0.", "Hàm f(x) liên tục tại x0 nếu lim(x->x0) f(x) = f(x0)."]'::jsonb,
    '["lim(x->2) (x^2 - 4)/(x - 2) = lim (x+2) = 4."]'::jsonb,
    '[{"id": "math-11-gioi-han-ham-so-q1", "difficulty": "EASY", "prompt": "Tính lim(x->1) (2x + 3).", "options": [{"id": "a", "text": "5"}, {"id": "b", "text": "2"}, {"id": "c", "text": "0"}], "correctOptionId": "a", "explanation": "Thay trực tiếp x = 1 vào đa thức, kết quả là 5."}, {"id": "math-11-gioi-han-ham-so-q2", "difficulty": "MEDIUM", "prompt": "Tính lim(x->2) (x^2 - 4)/(x - 2).", "options": [{"id": "a", "text": "0"}, {"id": "b", "text": "4"}, {"id": "c", "text": "Không tồn tại"}], "correctOptionId": "b", "explanation": "Phân tích tử số: (x-2)(x+2). Rút gọn (x-2) còn x+2. Khi x->2 thì giới hạn bằng 4."}, {"id": "math-11-gioi-han-ham-so-q3", "difficulty": "APPLIED", "prompt": "Hàm số f(x) = (x^2-1)/(x-1) với x≠1 và f(1)=m. Tìm m để hàm liên tục tại x=1.", "options": [{"id": "a", "text": "1"}, {"id": "b", "text": "2"}, {"id": "c", "text": "0"}], "correctOptionId": "b", "explanation": "lim(x->1) f(x) = lim (x+1) = 2. Để liên tục thì f(1) = m = 2."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-duong-thang-mat-phang',
    8,
    'LESSON',
    'Đường thẳng và mặt phẳng trong không gian',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Hình học không gian nghiên cứu các đối tượng 3 chiều.", "Ba điểm không thẳng hàng xác định duy nhất một mặt phẳng."]'::jsonb,
    '["Giao tuyến của 2 mặt phẳng phân biệt (có điểm chung) luôn là một đường thẳng."]'::jsonb,
    '[{"id": "math-11-duong-thang-mat-phang-q1", "difficulty": "EASY", "prompt": "Có bao nhiêu mặt phẳng đi qua 3 điểm không thẳng hàng?", "options": [{"id": "a", "text": "1"}, {"id": "b", "text": "2"}, {"id": "c", "text": "Vô số"}], "correctOptionId": "a", "explanation": "Ba điểm không thẳng hàng là điều kiện xác định duy nhất 1 mặt phẳng."}, {"id": "math-11-duong-thang-mat-phang-q2", "difficulty": "MEDIUM", "prompt": "Hai mặt phẳng phân biệt có điểm chung thì giao nhau theo một?", "options": [{"id": "a", "text": "Điểm"}, {"id": "b", "text": "Đường thẳng"}, {"id": "c", "text": "Mặt phẳng"}], "correctOptionId": "b", "explanation": "Giao tuyến của hai mặt phẳng phân biệt (nếu có) luôn là một đường thẳng."}, {"id": "math-11-duong-thang-mat-phang-q3", "difficulty": "APPLIED", "prompt": "Tứ diện ABCD có bao nhiêu mặt?", "options": [{"id": "a", "text": "3"}, {"id": "b", "text": "4"}, {"id": "c", "text": "6"}], "correctOptionId": "b", "explanation": "Tứ diện (hình chóp tam giác) có 4 mặt đều là các tam giác."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-hai-duong-thang',
    9,
    'LESSON',
    'Hai đường thẳng song song và chéo nhau',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Trong không gian, hai đường thẳng có 4 vị trí: cắt nhau, song song, trùng nhau, và chéo nhau.", "Chéo nhau là không cùng nằm trên bất kỳ mặt phẳng nào."]'::jsonb,
    '["Hai cạnh đối diện của một tứ diện luôn là hai đường thẳng chéo nhau."]'::jsonb,
    '[{"id": "math-11-hai-duong-thang-q1", "difficulty": "EASY", "prompt": "Hai đường thẳng không có điểm chung và cùng nằm trong một mặt phẳng thì?", "options": [{"id": "a", "text": "Chéo nhau"}, {"id": "b", "text": "Song song"}, {"id": "c", "text": "Cắt nhau"}], "correctOptionId": "b", "explanation": "Đây là định nghĩa chuẩn của hai đường thẳng song song."}, {"id": "math-11-hai-duong-thang-q2", "difficulty": "MEDIUM", "prompt": "Hai đường thẳng chéo nhau khi chúng?", "options": [{"id": "a", "text": "Không có điểm chung"}, {"id": "b", "text": "Không đồng phẳng"}, {"id": "c", "text": "Cắt nhau"}], "correctOptionId": "b", "explanation": "Không đồng phẳng (không cùng nằm trên một mặt phẳng) là tính chất cốt lõi của hai đường chéo nhau."}, {"id": "math-11-hai-duong-thang-q3", "difficulty": "APPLIED", "prompt": "Trong tứ diện ABCD, hai cạnh AB và CD ở vị trí?", "options": [{"id": "a", "text": "Song song"}, {"id": "b", "text": "Cắt nhau"}, {"id": "c", "text": "Chéo nhau"}], "correctOptionId": "c", "explanation": "AB và CD không cùng nằm trong bất kỳ mặt phẳng nào của tứ diện, nên chúng chéo nhau."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-duong-thang-mat-phang-song-song',
    10,
    'FIVE_LESSON_REVIEW',
    'Đường thẳng và mặt phẳng song song',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Đường thẳng d song song với (P) nếu d không nằm trong (P) và d song song với một đường d'' nằm trong (P)."]'::jsonb,
    '["Nếu d // (P), mọi mặt phẳng chứa d mà cắt (P) thì giao tuyến sẽ song song với d."]'::jsonb,
    '[{"id": "math-11-duong-thang-mat-phang-song-song-q1", "difficulty": "EASY", "prompt": "Đường thẳng d song song với (P) thì d và (P) có bao nhiêu điểm chung?", "options": [{"id": "a", "text": "0"}, {"id": "b", "text": "1"}, {"id": "c", "text": "Vô số"}], "correctOptionId": "a", "explanation": "Song song nghĩa là không có bất kỳ điểm chung nào."}, {"id": "math-11-duong-thang-mat-phang-song-song-q2", "difficulty": "MEDIUM", "prompt": "Nếu d // d'' và d'' nằm trong (P) thì d song song hoặc ... với (P).", "options": [{"id": "a", "text": "Cắt"}, {"id": "b", "text": "Trùng"}, {"id": "c", "text": "Nằm trong"}], "correctOptionId": "c", "explanation": "Nếu d không nằm trong (P) thì d // (P). Nếu d đã thuộc (P) thì d nằm trong (P)."}, {"id": "math-11-duong-thang-mat-phang-song-song-q3", "difficulty": "APPLIED", "prompt": "Cho hình chóp S.ABCD đáy hình bình hành. AB song song với mặt phẳng nào?", "options": [{"id": "a", "text": "(SCD)"}, {"id": "b", "text": "(SBC)"}, {"id": "c", "text": "(SAD)"}], "correctOptionId": "a", "explanation": "AB // CD và CD nằm trong mp(SCD), nên AB // (SCD)."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-hai-mat-phang-song-song',
    11,
    'LESSON',
    'Hai mặt phẳng song song',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Hai mặt phẳng (P) và (Q) song song nếu chúng không có điểm chung.", "Nếu (P) chứa hai đường cắt nhau cùng song song với (Q) thì (P) // (Q)."]'::jsonb,
    '["Mặt trần nhà và mặt sàn nhà là hình ảnh của hai mặt phẳng song song."]'::jsonb,
    '[{"id": "math-11-hai-mat-phang-song-song-q1", "difficulty": "EASY", "prompt": "Hai mặt phẳng song song có điểm chung không?", "options": [{"id": "a", "text": "Có"}, {"id": "b", "text": "Không"}, {"id": "c", "text": "Tùy trường hợp"}], "correctOptionId": "b", "explanation": "Theo định nghĩa, hai mặt phẳng song song tuyệt đối không có điểm chung."}, {"id": "math-11-hai-mat-phang-song-song-q2", "difficulty": "MEDIUM", "prompt": "Định lí Talet trong không gian áp dụng cho?", "options": [{"id": "a", "text": "3 MP song song"}, {"id": "b", "text": "2 MP song song"}, {"id": "c", "text": "3 MP cắt nhau"}], "correctOptionId": "a", "explanation": "Định lí Talet không gian phát biểu: 3 mặt phẳng song song chắn trên hai cát tuyến bất kì các đoạn thẳng tương ứng tỉ lệ."}, {"id": "math-11-hai-mat-phang-song-song-q3", "difficulty": "APPLIED", "prompt": "Cho lăng trụ ABC.A''B''C''. Mặt (ABC) song song với mặt nào?", "options": [{"id": "a", "text": "(A''B''C'')"}, {"id": "b", "text": "(ABB''A'')"}, {"id": "c", "text": "(BCC''B'')"}], "correctOptionId": "a", "explanation": "Hai đáy của hình lăng trụ luôn nằm trên hai mặt phẳng song song với nhau."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-dao-ham-dinh-nghia',
    12,
    'LESSON',
    'Định nghĩa và ý nghĩa đạo hàm',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Đạo hàm của f(x) tại x0 là giới hạn của tỉ số biến thiên (f(x)-f(x0))/(x-x0) khi x tiến về x0.", "Ý nghĩa hình học: f''(x0) là hệ số góc của tiếp tuyến."]'::jsonb,
    '["Tiếp tuyến của y = f(x) tại điểm có hoành độ x0 có phương trình y = f''(x0)(x - x0) + f(x0)."]'::jsonb,
    '[{"id": "math-11-dao-ham-dinh-nghia-q1", "difficulty": "EASY", "prompt": "Ý nghĩa hình học của f''(x0) là?", "options": [{"id": "a", "text": "Hệ số góc tiếp tuyến"}, {"id": "b", "text": "Vận tốc"}, {"id": "c", "text": "Tọa độ đỉnh"}], "correctOptionId": "a", "explanation": "Đạo hàm tại một điểm bằng hệ số góc của tiếp tuyến với đồ thị tại điểm đó."}, {"id": "math-11-dao-ham-dinh-nghia-q2", "difficulty": "MEDIUM", "prompt": "Vận tốc tức thời v(t) của chuyển động s(t) bằng?", "options": [{"id": "a", "text": "s''(t)"}, {"id": "b", "text": "s(t)/t"}, {"id": "c", "text": "s''''(t)"}], "correctOptionId": "a", "explanation": "Ý nghĩa vật lý: Vận tốc là đạo hàm bậc nhất của quãng đường theo thời gian."}, {"id": "math-11-dao-ham-dinh-nghia-q3", "difficulty": "APPLIED", "prompt": "Nếu f''(x0) = 0 thì tiếp tuyến tại điểm đó có đặc điểm gì?", "options": [{"id": "a", "text": "Song song trục tung"}, {"id": "b", "text": "Song song/trùng trục hoành"}, {"id": "c", "text": "Độ dốc bằng 1"}], "correctOptionId": "b", "explanation": "Hệ số góc bằng 0 tức là tiếp tuyến là đường thẳng y = hằng số, song song với trục hoành Ox."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-quy-tac-dao-ham',
    13,
    'LESSON',
    'Các quy tắc tính đạo hàm',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["(u+v)'' = u'' + v''", "(uv)'' = u''v + uv''", "(u/v)'' = (u''v - uv'') / v^2", "(x^n)'' = n.x^(n-1)"]'::jsonb,
    '["y = x^3 => y'' = 3x^2.", "y = sin x => y'' = cos x."]'::jsonb,
    '[{"id": "math-11-quy-tac-dao-ham-q1", "difficulty": "EASY", "prompt": "Đạo hàm của hàm số y = x^4 là?", "options": [{"id": "a", "text": "4x^3"}, {"id": "b", "text": "3x^4"}, {"id": "c", "text": "x^3"}], "correctOptionId": "a", "explanation": "Áp dụng công thức (x^n)'' = n.x^(n-1)."}, {"id": "math-11-quy-tac-dao-ham-q2", "difficulty": "MEDIUM", "prompt": "Đạo hàm của y = sin(2x) là?", "options": [{"id": "a", "text": "cos(2x)"}, {"id": "b", "text": "2cos(2x)"}, {"id": "c", "text": "-2cos(2x)"}], "correctOptionId": "b", "explanation": "Áp dụng đạo hàm hàm hợp: (sin u)'' = u''.cos u => 2cos(2x)."}, {"id": "math-11-quy-tac-dao-ham-q3", "difficulty": "APPLIED", "prompt": "Đạo hàm của y = 1/x là?", "options": [{"id": "a", "text": "1/x^2"}, {"id": "b", "text": "-1/x^2"}, {"id": "c", "text": "ln(x)"}], "correctOptionId": "b", "explanation": "(1/x)'' = (x^-1)'' = -1.x^-2 = -1/x^2."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-bien-co-hop-giao',
    14,
    'LESSON',
    'Biến cố hợp và biến cố giao',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Biến cố hợp (A U B) xảy ra khi A hoặc B xảy ra.", "Biến cố giao (AB) xảy ra khi cả A và B cùng xảy ra."]'::jsonb,
    '["Tung xúc xắc: A=''chẵn'', B=''chia hết cho 3''. A giao B là biến cố tung được mặt 6 chấm."]'::jsonb,
    '[{"id": "math-11-bien-co-hop-giao-q1", "difficulty": "EASY", "prompt": "Ký hiệu của biến cố giao giữa A và B là?", "options": [{"id": "a", "text": "A U B"}, {"id": "b", "text": "AB"}, {"id": "c", "text": "A \\ B"}], "correctOptionId": "b", "explanation": "Biến cố giao thường viết tắt là AB hoặc A ∩ B."}, {"id": "math-11-bien-co-hop-giao-q2", "difficulty": "MEDIUM", "prompt": "Hai biến cố xung khắc thì phần giao của chúng bằng?", "options": [{"id": "a", "text": "Biến cố chắc chắn"}, {"id": "b", "text": "Tập rỗng"}, {"id": "c", "text": "Tùy ý"}], "correctOptionId": "b", "explanation": "Xung khắc tức là không thể cùng xảy ra, nên phần giao là tập rỗng (biến cố không thể)."}, {"id": "math-11-bien-co-hop-giao-q3", "difficulty": "APPLIED", "prompt": "A=''chẵn'', B=''nhỏ hơn 4''. Xúc xắc 6 mặt. Tập hợp A hợp B là?", "options": [{"id": "a", "text": "{2}"}, {"id": "b", "text": "{1, 2, 3, 4, 6}"}, {"id": "c", "text": "{2, 4, 6}"}], "correctOptionId": "b", "explanation": "A={2,4,6}, B={1,2,3}. A hợp B là kết hợp tất cả: {1,2,3,4,6}."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-xac-suat-co-dieu-kien',
    15,
    'FIVE_LESSON_REVIEW',
    'Xác suất có điều kiện và Quy tắc nhân',
    'Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian',
    'Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.',
    '["Xác suất có điều kiện của A biết B đã xảy ra: P(A|B) = P(AB)/P(B).", "Quy tắc nhân: P(AB) = P(A) * P(B) nếu độc lập."]'::jsonb,
    '["Tung 2 đồng xu độc lập. XS mặt ngửa đồng xu 2 biết đồng xu 1 ngửa vẫn là 1/2 vì chúng độc lập."]'::jsonb,
    '[{"id": "math-11-xac-suat-co-dieu-kien-q1", "difficulty": "EASY", "prompt": "Nếu A và B độc lập thì P(AB) bằng?", "options": [{"id": "a", "text": "P(A) + P(B)"}, {"id": "b", "text": "P(A) * P(B)"}, {"id": "c", "text": "P(A) / P(B)"}], "correctOptionId": "b", "explanation": "Theo quy tắc nhân xác suất cho 2 biến cố độc lập."}, {"id": "math-11-xac-suat-co-dieu-kien-q2", "difficulty": "MEDIUM", "prompt": "Công thức P(A|B) là?", "options": [{"id": "a", "text": "P(A)/P(B)"}, {"id": "b", "text": "P(AB)/P(B)"}, {"id": "c", "text": "P(AB)/P(A)"}], "correctOptionId": "b", "explanation": "Xác suất của A với điều kiện B bằng xác suất đồng thời chia cho xác suất của B."}, {"id": "math-11-xac-suat-co-dieu-kien-q3", "difficulty": "APPLIED", "prompt": "Có 3 bi đỏ, 2 bi xanh. Rút 2 bi không hoàn lại. XS rút 2 bi đỏ là?", "options": [{"id": "a", "text": "3/10"}, {"id": "b", "text": "9/25"}, {"id": "c", "text": "1/2"}], "correctOptionId": "a", "explanation": "Lần 1 đỏ: 3/5. Lần 2 đỏ: 2/4. P = (3/5)*(2/4) = 6/20 = 3/10."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-dao-dong-dieu-hoa',
    1,
    'LESSON',
    'Dao động điều hòa',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Dao động điều hòa theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Dao động điều hòa vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-dao-dong-dieu-hoa-physics-11-dao-dong-dieu-hoa-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Dao động điều hòa?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-dao-dong-dieu-hoa-physics-11-dao-dong-dieu-hoa-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Dao động điều hòa?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-dao-dong-dieu-hoa-physics-11-dao-dong-dieu-hoa-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Dao động điều hòa giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-cac-dai-luong-li-do-van-toc-gia-toc',
    2,
    'LESSON',
    'Các đại lượng li độ - vận tốc - gia tốc',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Các đại lượng li độ - vận tốc - gia tốc theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Các đại lượng li độ - vận tốc - gia tốc vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-cac-dai-luong-li-do-van-toc-gia-toc-physics-11-cac-dai-luong-li-do-van-toc-gia-toc-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Các đại lượng li độ - vận tốc - gia tốc?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-cac-dai-luong-li-do-van-toc-gia-toc-physics-11-cac-dai-luong-li-do-van-toc-gia-toc-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Các đại lượng li độ - vận tốc - gia tốc?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-cac-dai-luong-li-do-van-toc-gia-toc-physics-11-cac-dai-luong-li-do-van-toc-gia-toc-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Các đại lượng li độ - vận tốc - gia tốc giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-nang-luong-trong-dao-dong',
    3,
    'LESSON',
    'Năng lượng trong dao động',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Năng lượng trong dao động theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Năng lượng trong dao động vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-nang-luong-trong-dao-dong-physics-11-nang-luong-trong-dao-dong-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Năng lượng trong dao động?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-nang-luong-trong-dao-dong-physics-11-nang-luong-trong-dao-dong-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Năng lượng trong dao động?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-nang-luong-trong-dao-dong-physics-11-nang-luong-trong-dao-dong-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Năng lượng trong dao động giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-dao-dong-tat-dan-va-cuong-buc',
    4,
    'LESSON',
    'Dao động tắt dần và cưỡng bức',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Dao động tắt dần và cưỡng bức theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Dao động tắt dần và cưỡng bức vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-dao-dong-tat-dan-va-cuong-buc-physics-11-dao-dong-tat-dan-va-cuong-buc-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Dao động tắt dần và cưỡng bức?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-dao-dong-tat-dan-va-cuong-buc-physics-11-dao-dong-tat-dan-va-cuong-buc-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Dao động tắt dần và cưỡng bức?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-dao-dong-tat-dan-va-cuong-buc-physics-11-dao-dong-tat-dan-va-cuong-buc-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Dao động tắt dần và cưỡng bức giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-hien-tuong-cong-huong',
    5,
    'FIVE_LESSON_REVIEW',
    'Hiện tượng cộng hưởng',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Hiện tượng cộng hưởng theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Hiện tượng cộng hưởng vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-hien-tuong-cong-huong-physics-11-hien-tuong-cong-huong-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Hiện tượng cộng hưởng?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-hien-tuong-cong-huong-physics-11-hien-tuong-cong-huong-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Hiện tượng cộng hưởng?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-hien-tuong-cong-huong-physics-11-hien-tuong-cong-huong-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Hiện tượng cộng hưởng giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-song-co-va-su-truyen-song',
    6,
    'LESSON',
    'Sóng cơ và sự truyền sóng',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Sóng cơ và sự truyền sóng theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Sóng cơ và sự truyền sóng vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-song-co-va-su-truyen-song-physics-11-song-co-va-su-truyen-song-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Sóng cơ và sự truyền sóng?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-song-co-va-su-truyen-song-physics-11-song-co-va-su-truyen-song-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Sóng cơ và sự truyền sóng?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-song-co-va-su-truyen-song-physics-11-song-co-va-su-truyen-song-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Sóng cơ và sự truyền sóng giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-cac-dac-trung-cua-song',
    7,
    'LESSON',
    'Các đặc trưng của sóng',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Các đặc trưng của sóng theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Các đặc trưng của sóng vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-cac-dac-trung-cua-song-physics-11-cac-dac-trung-cua-song-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Các đặc trưng của sóng?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-cac-dac-trung-cua-song-physics-11-cac-dac-trung-cua-song-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Các đặc trưng của sóng?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-cac-dac-trung-cua-song-physics-11-cac-dac-trung-cua-song-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Các đặc trưng của sóng giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-giao-thoa-song-co',
    8,
    'LESSON',
    'Giao thoa sóng cơ',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Giao thoa sóng cơ theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Giao thoa sóng cơ vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-giao-thoa-song-co-physics-11-giao-thoa-song-co-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Giao thoa sóng cơ?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-giao-thoa-song-co-physics-11-giao-thoa-song-co-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Giao thoa sóng cơ?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-giao-thoa-song-co-physics-11-giao-thoa-song-co-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Giao thoa sóng cơ giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-song-dung',
    9,
    'LESSON',
    'Sóng dừng',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Sóng dừng theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Sóng dừng vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-song-dung-physics-11-song-dung-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Sóng dừng?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-song-dung-physics-11-song-dung-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Sóng dừng?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-song-dung-physics-11-song-dung-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Sóng dừng giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-song-dien-tu',
    10,
    'FIVE_LESSON_REVIEW',
    'Sóng điện từ',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Sóng điện từ theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Sóng điện từ vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-song-dien-tu-physics-11-song-dien-tu-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Sóng điện từ?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-song-dien-tu-physics-11-song-dien-tu-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Sóng điện từ?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-song-dien-tu-physics-11-song-dien-tu-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Sóng điện từ giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-dien-truong-va-cuong-do-dien-truong',
    11,
    'LESSON',
    'Điện trường và cường độ điện trường',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Điện trường và cường độ điện trường theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Điện trường và cường độ điện trường vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-dien-truong-va-cuong-do-dien-truong-physics-11-dien-truong-va-cuong-do-dien-truong-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Điện trường và cường độ điện trường?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-dien-truong-va-cuong-do-dien-truong-physics-11-dien-truong-va-cuong-do-dien-truong-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Điện trường và cường độ điện trường?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-dien-truong-va-cuong-do-dien-truong-physics-11-dien-truong-va-cuong-do-dien-truong-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Điện trường và cường độ điện trường giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-the-nang-va-dien-the',
    12,
    'LESSON',
    'Thế năng và điện thế',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Thế năng và điện thế theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Thế năng và điện thế vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-the-nang-va-dien-the-physics-11-the-nang-va-dien-the-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Thế năng và điện thế?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-the-nang-va-dien-the-physics-11-the-nang-va-dien-the-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Thế năng và điện thế?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-the-nang-va-dien-the-physics-11-the-nang-va-dien-the-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Thế năng và điện thế giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-tu-dien-va-dien-dung',
    13,
    'LESSON',
    'Tụ điện và điện dung',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Tụ điện và điện dung theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Tụ điện và điện dung vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-tu-dien-va-dien-dung-physics-11-tu-dien-va-dien-dung-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Tụ điện và điện dung?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-tu-dien-va-dien-dung-physics-11-tu-dien-va-dien-dung-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Tụ điện và điện dung?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-tu-dien-va-dien-dung-physics-11-tu-dien-va-dien-dung-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Tụ điện và điện dung giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-cuong-do-dong-dien',
    14,
    'LESSON',
    'Cường độ dòng điện',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Cường độ dòng điện theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Cường độ dòng điện vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-cuong-do-dong-dien-physics-11-cuong-do-dong-dien-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Cường độ dòng điện?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-cuong-do-dong-dien-physics-11-cuong-do-dong-dien-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Cường độ dòng điện?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-cuong-do-dong-dien-physics-11-cuong-do-dong-dien-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Cường độ dòng điện giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-nang-luong-dien',
    15,
    'FIVE_LESSON_REVIEW',
    'Năng lượng điện',
    'Vật lí 11: Dao động, Sóng và Điện từ trường',
    'Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.',
    '["Nội dung trọng tâm bài Năng lượng điện theo khung CT GDPT 2018."]'::jsonb,
    '["Ví dụ ứng dụng kiến thức phần Năng lượng điện vào thực tiễn."]'::jsonb,
    '[{"id": "physics-11-nang-luong-dien-physics-11-nang-luong-dien-q1", "prompt": "Câu hỏi nhận biết khái niệm cơ bản của bài Năng lượng điện?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}], "correctOptionId": "a", "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."}, {"id": "physics-11-nang-luong-dien-physics-11-nang-luong-dien-q2", "prompt": "Câu hỏi thông hiểu hoặc tính toán cơ bản phần Năng lượng điện?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}], "correctOptionId": "b", "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."}, {"id": "physics-11-nang-luong-dien-physics-11-nang-luong-dien-q3", "prompt": "Tình huống thực tế áp dụng kiến thức Năng lượng điện giải quyết vấn đề?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}], "correctOptionId": "c", "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."}]'::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
