package com.brightpath.science.domain.service;

import com.brightpath.science.domain.model.ScienceSubject;

/** Subject-specific teaching strategy for one Science AI tutor. */
public interface ScienceTutorPromptStrategy {
    ScienceSubject getSubject();
    String getTutorName();
    String buildSubjectInstructions();
}
