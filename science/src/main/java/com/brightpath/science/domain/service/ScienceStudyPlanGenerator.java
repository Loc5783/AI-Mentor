package com.brightpath.science.domain.service;

import com.brightpath.science.domain.model.StudyPlan;
import com.brightpath.science.domain.model.StudyPlanRequest;

public interface ScienceStudyPlanGenerator {
    void generate(StudyPlanRequest request, ResultCallback<StudyPlan> callback);
}
