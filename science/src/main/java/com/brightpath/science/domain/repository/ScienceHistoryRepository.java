package com.brightpath.science.domain.repository;

import com.brightpath.science.domain.model.ScienceHistory;
import com.brightpath.science.domain.model.ScienceQuestion;
import com.brightpath.science.domain.model.ScienceSubject;

import java.util.List;

public interface ScienceHistoryRepository {
    long save(ScienceQuestion question, String answer);
    ScienceHistory findCached(int userId, ScienceSubject subject, String educationLevel, String normalizedQuestion);
    ScienceHistory findById(int userId, long historyId);
    List<ScienceHistory> findConversation(int userId, String conversationId);
    List<ScienceHistory> search(int userId, ScienceSubject subject, String keyword);
    void addReviewDuration(long historyId, int seconds);
    int countByUser(int userId);
    int totalReviewSeconds(int userId);
    ScienceSubject mostFrequentSubject(int userId);
}
