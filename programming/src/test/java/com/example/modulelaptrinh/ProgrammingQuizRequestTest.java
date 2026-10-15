package com.example.modulelaptrinh;

import com.example.modulelaptrinh.models.QuizRequest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProgrammingQuizRequestTest {
    @Test
    public void keepsProgrammingQuizConfigurationAndTrimsTopic() {
        QuizRequest request = new QuizRequest("Java", "Nâng cao", "  OOP và SOLID  ", 6);

        assertEquals("Java", request.getSubject());
        assertEquals("Nâng cao", request.getLevel());
        assertEquals("OOP và SOLID", request.getTopic());
        assertEquals(6, request.getNumberOfQuestions());
        assertEquals(0, request.getUserId());
        assertTrue(request.getRecentlyUsedPrompts().isEmpty());
    }

    @Test
    public void missingProgrammingValuesUseDefaultsAndQuestionCountIsClamped() {
        QuizRequest minimum = new QuizRequest(null, null, "  ", 1);
        QuizRequest maximum = new QuizRequest("Python", "Cơ bản", "Lists", 50);

        assertEquals("Lập trình", minimum.getSubject());
        assertEquals("Cơ bản", minimum.getLevel());
        assertEquals("Kiến thức lập trình", minimum.getTopic());
        assertEquals(3, minimum.getNumberOfQuestions());
        assertEquals(10, maximum.getNumberOfQuestions());
    }
}
