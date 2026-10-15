package com.example.modulelaptrinh;

import com.example.modulelaptrinh.models.QuizQuestion;
import com.example.modulelaptrinh.models.QuizRequest;
import com.example.modulelaptrinh.models.QuizResult;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ProgrammingQuizResultTest {
    @Test
    public void calculatesProgrammingQuizAccuracyAndClampsValues() {
        QuizResult result = new QuizResult(
                new QuizRequest("Java", "Cơ bản", "Biến", 3),
                Arrays.asList(question("Q1"), question("Q2"), question("Q3")),
                8,
                -10);

        assertEquals(3, result.getTotalQuestions());
        assertEquals(3, result.getCorrectAnswers());
        assertEquals(0, result.getExpEarned());
        assertEquals(100.0, result.getAccuracyPercent(), 0.001);
    }

    @Test
    public void emptyProgrammingQuizHasZeroAccuracy() {
        QuizResult result = new QuizResult(
                new QuizRequest("Python", "Cơ bản", "List", 3),
                Collections.emptyList(),
                2,
                15);

        assertEquals(0, result.getTotalQuestions());
        assertEquals(0, result.getCorrectAnswers());
        assertEquals(0.0, result.getAccuracyPercent(), 0.001);
    }

    private QuizQuestion question(String prompt) {
        return new QuizQuestion(
                QuizQuestion.Type.TRUE_FALSE,
                prompt,
                Arrays.asList("True", "False"),
                "True",
                "Explanation");
    }
}
