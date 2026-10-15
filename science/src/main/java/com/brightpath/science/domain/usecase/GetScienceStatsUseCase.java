package com.brightpath.science.domain.usecase;

import com.brightpath.science.domain.model.ScienceStats;
import com.brightpath.science.domain.repository.QuizAttemptRepository;
import com.brightpath.science.domain.repository.ScienceBookmarkRepository;
import com.brightpath.science.domain.repository.ScienceHistoryRepository;

public final class GetScienceStatsUseCase {
    private final ScienceHistoryRepository historyRepository;
    private final ScienceBookmarkRepository bookmarkRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public GetScienceStatsUseCase(ScienceHistoryRepository historyRepository,
                                  ScienceBookmarkRepository bookmarkRepository,
                                  QuizAttemptRepository quizAttemptRepository) {
        this.historyRepository = historyRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    public ScienceStats execute(int userId) {
        return new ScienceStats(
                historyRepository.countByUser(userId),
                bookmarkRepository.countByUser(userId),
                historyRepository.mostFrequentSubject(userId),
                quizAttemptRepository.countAttempts(userId),
                quizAttemptRepository.calculateAccuracy(userId),
                historyRepository.totalReviewSeconds(userId),
                quizAttemptRepository.getTotalExp(userId)
        );
    }
}
