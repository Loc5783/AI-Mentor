package com.example.modulelaptrinh.utils;

import android.util.Log;

import com.example.modulelaptrinh.models.QuizDifficulty;
import com.example.modulelaptrinh.models.QuizQuestion;
import com.example.modulelaptrinh.models.QuizRequest;
import com.example.modulelaptrinh.models.QuizResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class QuizGenerator {
    private static final String TAG = "QuizGenerator";

    private static final String QUIZ_PROMPT_TEMPLATE = String.join("\n",
            "Bạn là chuyên gia giáo dục lập trình. Hãy tạo chính xác %d câu hỏi trắc nghiệm",
            "về chủ đề \"%s\" ở mức \"%s\".",
            "",
            "YÊU CẦU:",
            "1. Mỗi câu có đúng 4 lựa chọn.",
            "2. correct_answer phải chỉ là một chữ A, B, C hoặc D.",
            "3. Có giải thích ngắn gọn, chính xác.",
            "4. Câu hỏi đa dạng: lý thuyết, đọc code, tìm lỗi, dự đoán kết quả và khái niệm.",
            "5. Mỗi câu phải có difficulty là EASY, MEDIUM hoặc HARD.",
            "6. Không tạo câu phụ thuộc vào hình ảnh hoặc tài liệu bên ngoài.",
            "7. Code trong chuỗi JSON phải escape đúng ký tự xuống dòng và dấu ngoặc kép.",
            "",
            "Chỉ trả về JSON hợp lệ, không markdown, không lời dẫn:",
            "{",
            "  \"questions\": [",
            "    {",
            "      \"prompt\": \"Câu hỏi\",",
            "      \"options\": [\"A. ...\", \"B. ...\", \"C. ...\", \"D. ...\"],",
            "      \"correct_answer\": \"A\",",
            "      \"explanation\": \"Giải thích\",",
            "      \"difficulty\": \"EASY\"",
            "    }",
            "  ]",
            "}",
            ""
    );

    private final QuizGeminiApiClient geminiClient;

    public QuizGenerator() {
        this.geminiClient = new QuizGeminiApiClient();
    }

    public QuizResult generateQuiz(QuizRequest request, int userId) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin tạo Quiz.");
        }

        String prompt = String.format(
                Locale.ROOT,
                QUIZ_PROMPT_TEMPLATE,
                request.getNumberOfQuestions(),
                request.getTopic(),
                request.getLevel()
        );

        // FIX: Sử dụng empty lists để tránh ambiguous method
        QuizGeminiApiClient.Result result = geminiClient.generateText(
                prompt,
                Collections.emptyList(),
                Collections.emptyList(),
                0.55,
                8192
        );

        if (result == null || result.text == null || result.text.trim().isEmpty()) {
            throw new Exception("AI không trả về dữ liệu Quiz.");
        }

        List<QuizQuestion> questions = parseQuizResponse(result.text);
        if (questions.size() != request.getNumberOfQuestions()) {
            Log.w(TAG, "AI returned " + questions.size() + " questions instead of "
                    + request.getNumberOfQuestions());
        }
        if (questions.isEmpty()) {
            throw new Exception("Không có câu hỏi hợp lệ trong phản hồi AI.");
        }

        return new QuizResult(request, questions, 0, 0);
    }

    private List<QuizQuestion> parseQuizResponse(String response) throws Exception {
        List<QuizQuestion> questions = new ArrayList<>();

        try {
            JSONObject root = new JSONObject(extractJson(response));
            JSONArray questionsArray = root.optJSONArray("questions");
            if (questionsArray == null) {
                throw new Exception("Thiếu mảng questions.");
            }

            for (int i = 0; i < questionsArray.length(); i++) {
                JSONObject item = questionsArray.optJSONObject(i);
                if (item == null) {
                    continue;
                }

                String prompt = item.optString("prompt", "").trim();
                JSONArray optionsArray = item.optJSONArray("options");
                if (prompt.isEmpty() || optionsArray == null || optionsArray.length() != 4) {
                    Log.w(TAG, "Skipping invalid question at index " + i);
                    continue;
                }

                List<String> options = new ArrayList<>();
                for (int j = 0; j < 4; j++) {
                    String option = optionsArray.optString(j, "").trim();
                    if (option.isEmpty()) {
                        options.clear();
                        break;
                    }
                    options.add(option);
                }
                if (options.size() != 4) {
                    continue;
                }

                String correctAnswer = normalizeCorrectAnswer(
                        item.optString("correct_answer", ""),
                        options
                );
                if (correctAnswer.isEmpty()) {
                    Log.w(TAG, "Skipping question with invalid answer at index " + i);
                    continue;
                }

                String explanation = item.optString(
                        "explanation",
                        "Chưa có giải thích."
                ).trim();
                QuizDifficulty difficulty = QuizDifficulty.fromApiValue(
                        item.optString("difficulty", "MEDIUM")
                );

                questions.add(new QuizQuestion(
                        QuizQuestion.Type.MULTIPLE_CHOICE,
                        prompt,
                        options,
                        correctAnswer,
                        explanation,
                        difficulty
                ));
            }
        } catch (Exception error) {
            Log.e(TAG, "Không thể parse Quiz AI", error);
            throw new Exception(
                    "Dữ liệu Quiz từ AI không đúng định dạng: " + error.getMessage(),
                    error
            );
        }

        return questions;
    }

    private String normalizeCorrectAnswer(String rawAnswer, List<String> options) {
        if (rawAnswer == null) {
            return "";
        }

        String normalized = rawAnswer.trim().toUpperCase(Locale.ROOT);
        if (!normalized.isEmpty()) {
            char first = normalized.charAt(0);
            if (first >= 'A' && first <= 'D') {
                return String.valueOf(first);
            }
        }

        try {
            int number = Integer.parseInt(normalized);
            if (number >= 1 && number <= 4) {
                return String.valueOf((char) ('A' + number - 1));
            }
            if (number >= 0 && number <= 3) {
                return String.valueOf((char) ('A' + number));
            }
        } catch (NumberFormatException ignored) {
        }

        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            if (option.equalsIgnoreCase(rawAnswer.trim())) {
                return String.valueOf((char) ('A' + i));
            }
        }
        return "";
    }

    private String extractJson(String response) throws Exception {
        if (response == null) {
            throw new Exception("Phản hồi rỗng.");
        }

        String cleaned = response.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new Exception("Không tìm thấy JSON.");
        }
        return cleaned.substring(start, end + 1);
    }
}