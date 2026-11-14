begin;

-- Kết nối tri thức Toán 10 teaches inequalities in two variables. Replace
-- the earlier one-variable prerequisite with a question from that unit.
update public.mentor_questions
set prompt = 'Điểm nào thỏa mãn hệ bất phương trình x ≥ 0, y ≥ 0, x + y ≤ 3?',
    option_a = '(1; 1)',
    option_b = '(2; 2)',
    option_c = '(-1; 1)',
    option_d = '(1; -1)',
    correct_option = 'A',
    explanation = '(1; 1) có hai tọa độ không âm và tổng bằng 2 ≤ 3; mỗi điểm còn lại vi phạm ít nhất một bất phương trình.',
    chapter = 'Hệ bất phương trình và miền nghiệm',
    topic = 'Hệ bất phương trình và miền nghiệm',
    updated_at = now()
where id = 'g10-found-math-01' and grade = 10 and subject_id = 'math';

commit;
