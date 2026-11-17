export function essayPrompt({grade, subject, question, rubric, answer, maxPoints}) {
  return `Chấm câu tự luận lớp ${grade}, môn ${subject}. Chỉ dựa trên câu hỏi và thang điểm dưới đây. Chấp nhận cách giải tương đương đúng; không thưởng điểm cho nội dung ngoài yêu cầu. Nếu thang điểm thiếu chi tiết, cho điểm thận trọng và nêu lý do. Trả JSON duy nhất {"points": số nguyên từ 0 đến ${maxPoints}, "feedback": "nhận xét ngắn bằng tiếng Việt"}. Điểm là đơn vị nhỏ nhất của hệ thống, tối đa ${maxPoints}.\nCâu hỏi: ${question}\nThang điểm/đáp án: ${rubric}\nBài làm học sinh: ${answer}`;
}

export function parseEssayGrade(raw, maxPoints) {
  let grade;
  try { grade = JSON.parse(raw); } catch { throw new Error('INVALID_AI_GRADE'); }
  if (!Number.isInteger(grade?.points) || grade.points < 0 || grade.points > maxPoints ||
      typeof grade.feedback !== 'string' || !grade.feedback.trim() || grade.feedback.length > 1000) {
    throw new Error('INVALID_AI_GRADE');
  }
  return {points: grade.points, feedback: grade.feedback.trim()};
}
