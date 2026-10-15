package com.brightpath.science.domain.service;

import com.brightpath.science.domain.model.QuizRequest;
import com.brightpath.science.domain.model.ScienceQuestion;

public interface SciencePromptFactory {
    String buildAnswerPrompt(ScienceQuestion question);
    String buildQuizPrompt(QuizRequest request);
}
