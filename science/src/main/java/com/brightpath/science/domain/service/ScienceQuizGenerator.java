package com.brightpath.science.domain.service;

import com.brightpath.science.domain.model.QuizQuestion;
import com.brightpath.science.domain.model.QuizRequest;

import java.util.List;

public interface ScienceQuizGenerator {
    void generateQuiz(QuizRequest request, ResultCallback<List<QuizQuestion>> callback);
}
