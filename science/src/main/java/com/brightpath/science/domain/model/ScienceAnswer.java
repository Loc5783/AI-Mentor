package com.brightpath.science.domain.model;

public final class ScienceAnswer {
    private final String content;
    private final String detectedSubject;
    private final String difficulty;
    private final boolean fromCache;
    private final long historyId;
    private final int promptTokens;
    private final int responseTokens;
    private final int totalTokens;

    public ScienceAnswer(String content, String detectedSubject, String difficulty,
                         boolean fromCache, long historyId) {
        this(content, detectedSubject, difficulty, fromCache, historyId, 0, 0, 0);
    }

    public ScienceAnswer(String content, String detectedSubject, String difficulty,
                         boolean fromCache, long historyId,
                         int promptTokens, int responseTokens, int totalTokens) {
        this.content = content == null ? "" : content;
        this.detectedSubject = detectedSubject == null ? "" : detectedSubject;
        this.difficulty = difficulty == null ? "" : difficulty;
        this.fromCache = fromCache;
        this.historyId = historyId;
        this.promptTokens = promptTokens;
        this.responseTokens = responseTokens;
        this.totalTokens = totalTokens;
    }

    public String getContent() { return content; }
    public String getDetectedSubject() { return detectedSubject; }
    public String getDifficulty() { return difficulty; }
    public boolean isFromCache() { return fromCache; }
    public long getHistoryId() { return historyId; }
    public int getPromptTokens() { return promptTokens; }
    public int getResponseTokens() { return responseTokens; }
    public int getTotalTokens() { return totalTokens; }

    public ScienceAnswer withPersistence(long newHistoryId, boolean cached) {
        return new ScienceAnswer(content, detectedSubject, difficulty, cached, newHistoryId,
                promptTokens, responseTokens, totalTokens);
    }
}
