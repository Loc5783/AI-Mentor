package com.example.modulelaptrinh.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizRequest {
    private final String subject;
    private final String level;
    private final String topic;
    private final int numberOfQuestions;
    private final int userId;
    private final long variationSeed;
    private final List<String> recentlyUsedPrompts;

    public QuizRequest(String subject, String level, String topic, int numberOfQuestions) {
        this(subject, level, topic, numberOfQuestions, 0, System.nanoTime(), Collections.emptyList());
    }

    private QuizRequest(String subject, String level, String topic, int numberOfQuestions,
                        int userId, long variationSeed, List<String> recentlyUsedPrompts) {
        this.subject = subject == null ? "Lập trình" : subject;
        this.level = level == null ? "Cơ bản" : level;
        this.topic = topic == null || topic.trim().isEmpty() ? "Kiến thức lập trình" : topic.trim();
        this.numberOfQuestions = Math.max(3, Math.min(10, numberOfQuestions));
        this.userId = Math.max(0, userId);
        this.variationSeed = variationSeed;
        this.recentlyUsedPrompts = recentlyUsedPrompts == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(recentlyUsedPrompts));
    }

    public String getSubject() { return subject; }
    public String getLevel() { return level; }
    public String getTopic() { return topic; }
    public int getNumberOfQuestions() { return numberOfQuestions; }
    public int getUserId() { return userId; }
    public long getVariationSeed() { return variationSeed; }
    public List<String> getRecentlyUsedPrompts() { return recentlyUsedPrompts; }
}