package com.example.mathmodule.databases;

public class QuizSet {
    private final int id;
    private final String title;
    private final String category;
    private final String difficulty;
    private final String educationLevel;

    public QuizSet(int id, String title, String category, String difficulty) {
        this(id, title, category, difficulty, "Middle School");
    }

    public QuizSet(int id, String title, String category, String difficulty,
                   String educationLevel) {
        this.id = id;
        this.title = title == null ? "Bộ đề Toán" : title;
        this.category = category == null ? "Khác" : category;
        this.difficulty = difficulty == null ? "Trung bình" : difficulty;
        this.educationLevel = educationLevel == null || educationLevel.trim().isEmpty()
                ? "Middle School" : educationLevel.trim();
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDifficulty() { return difficulty; }
    public String getEducationLevel() { return educationLevel; }
}
