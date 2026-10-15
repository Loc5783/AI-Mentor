package com.brightpath.science.domain.service;

import com.brightpath.science.domain.model.ScienceAnswer;
import com.brightpath.science.domain.model.ScienceQuestion;

public interface ScienceAnswerService {
    void answer(ScienceQuestion question, ResultCallback<ScienceAnswer> callback);
}
