package com.brightpath.science.domain.repository;

import com.brightpath.science.domain.model.EducationLevel;
import com.brightpath.science.domain.model.ScienceSubject;

import java.util.List;

public interface QuizVariationRepository {
    List<String> getRecentPrompts(int userId, ScienceSubject subject,
                                  EducationLevel level, int limit);

    void rememberPrompts(int userId, ScienceSubject subject,
                         EducationLevel level, List<String> prompts);
}
