package com.example.mathmodule.databases;

import com.example.mathmodule.util.MathQuizDifficulty;

public class QuizQuestion {
    private final int id;
    private final int setId;
    private final String questionText;
    private final String optionA;
    private final String optionB;
    private final String optionC;
    private final String optionD;
    private final String correctAnswer;
    private final String explanation;
    private final MathQuizDifficulty difficulty;
    private String selectedAnswer = "";

    /** Compatibility constructor for existing bundled quiz data. */
    public QuizQuestion(int id, int setId, String questionText, String optionA,
                        String optionB, String optionC, String optionD,
                        String correctAnswer, String explanation) {
        this(id, setId, questionText, optionA, optionB, optionC, optionD,
                correctAnswer, explanation, MathQuizDifficulty.MEDIUM);
    }

    public QuizQuestion(int id, int setId, String questionText, String optionA,
                        String optionB, String optionC, String optionD,
                        String correctAnswer, String explanation,
                        MathQuizDifficulty difficulty) {
        this.id = id;
        this.setId = setId;
        this.questionText = safe(questionText);
        this.optionA = safe(optionA);
        this.optionB = safe(optionB);
        this.optionC = safe(optionC);
        this.optionD = safe(optionD);
        this.correctAnswer = normalizeAnswer(correctAnswer);
        this.explanation = safe(explanation);
        this.difficulty = difficulty == null ? MathQuizDifficulty.MEDIUM : difficulty;
    }

    public QuizQuestion(int id, int setId, String questionText, String optionA,
                        String optionB, String optionC, String optionD,
                        String correctAnswer, String explanation, String difficulty) {
        this(id, setId, questionText, optionA, optionB, optionC, optionD,
                correctAnswer, explanation, MathQuizDifficulty.fromApiValue(difficulty));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeAnswer(String value) {
        String answer = safe(value).toUpperCase();
        return answer.matches("[ABCD]") ? answer : "A";
    }

    public int getId() { return id; }
    public int getSetId() { return setId; }
    public String getQuestionText() { return questionText; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getOptionC() { return optionC; }
    public String getOptionD() { return optionD; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getExplanation() { return explanation; }
    public MathQuizDifficulty getDifficulty() { return difficulty; }
    public String getDifficultyDisplayName() { return difficulty.getDisplayName(); }
    public int getExpReward() { return difficulty.getExpReward(); }
    public String getSelectedAnswer() { return selectedAnswer; }
    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer = normalizeSelectedAnswer(selectedAnswer);
    }

    private static String normalizeSelectedAnswer(String value) {
        String answer = safe(value).toUpperCase();
        return answer.matches("[ABCD]") ? answer : "";
    }
}
