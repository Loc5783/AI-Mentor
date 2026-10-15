package com.brightpath.science.domain.policy;

import com.brightpath.science.domain.model.QuizRequest;

public interface AdaptiveQuizPolicy {
    QuizRequest adapt(int userId, QuizRequest baseRequest);
}
