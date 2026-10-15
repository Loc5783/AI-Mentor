package com.example.mathmodule;

import com.example.mathmodule.util.MathEducationLevel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MathEducationLevelTest {
    @Test
    public void resolvesDisplayNamesIgnoringCaseAndWhitespace() {
        assertEquals(MathEducationLevel.HIGH_SCHOOL,
                MathEducationLevel.fromDisplayName("  high school "));
        assertEquals(MathEducationLevel.UNIVERSITY,
                MathEducationLevel.fromDisplayName("University"));
    }

    @Test
    public void unknownOrMissingLevelFallsBackToMiddleSchool() {
        assertEquals(MathEducationLevel.MIDDLE_SCHOOL,
                MathEducationLevel.fromDisplayName(null));
        assertEquals(MathEducationLevel.MIDDLE_SCHOOL,
                MathEducationLevel.fromDisplayName("Primary School"));
    }

    @Test
    public void eachLevelProvidesMathLearningContextForAiPrompt() {
        assertTrue(MathEducationLevel.MIDDLE_SCHOOL.getPromptDescription().contains("lớp 6"));
        assertTrue(MathEducationLevel.HIGH_SCHOOL.getPromptDescription().contains("lớp 10"));
        assertTrue(MathEducationLevel.UNIVERSITY.getPromptDescription().contains("đại học"));
    }
}
