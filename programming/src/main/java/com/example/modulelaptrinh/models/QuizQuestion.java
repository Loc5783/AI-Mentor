package com.example.modulelaptrinh.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizQuestion {
    public enum Type { MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK }

    private final Type type;
    private final String prompt;
    private final List<String> options;
    private final String correctAnswer;
    private final String explanation;
    private final QuizDifficulty difficulty;

    public QuizQuestion(Type type, String prompt, List<String> options,
                        String correctAnswer, String explanation) {
        this(type, prompt, options, correctAnswer, explanation,
                QuizDifficulty.inferFromType(type));
    }

    public QuizQuestion(Type type, String prompt, List<String> options,
                        String correctAnswer, String explanation,
                        QuizDifficulty difficulty) {
        this.type = type == null ? Type.MULTIPLE_CHOICE : type;
        this.prompt = prompt == null ? "" : prompt;
        this.options = options == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(options));
        this.correctAnswer = correctAnswer == null ? "" : correctAnswer;
        this.explanation = explanation == null ? "" : explanation;
        this.difficulty = difficulty == null
                ? QuizDifficulty.inferFromType(this.type)
                : difficulty;
    }

    public Type getType() { return type; }
    public String getPrompt() { return prompt; }
    public List<String> getOptions() { return options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getExplanation() { return explanation; }
    public QuizDifficulty getDifficulty() { return difficulty; }
    public int getExpReward() { return difficulty.getExpReward(); }
}