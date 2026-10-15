package com.example.mathmodule;

import com.example.mathmodule.databases.QuizQuestion;
import com.example.mathmodule.util.MathQuizDifficulty;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathQuizQuestionTest {
    @Test
    public void normalizesAnswerAndKeepsMathDifficultyReward() {
        QuizQuestion question = new QuizQuestion(
                1, 7, "  Tính 3 x 4  ", " 10 ", " 12 ", " 14 ", " 16 ",
                " b ", " 3 nhân 4 bằng 12 ", "HARD");

        assertEquals("Tính 3 x 4", question.getQuestionText());
        assertEquals("B", question.getCorrectAnswer());
        assertEquals(MathQuizDifficulty.HARD, question.getDifficulty());
        assertEquals(20, question.getExpReward());
    }

    @Test
    public void invalidCorrectAndSelectedAnswersAreHandledSafely() {
        QuizQuestion question = new QuizQuestion(
                2, 8, null, null, null, null, null,
                "Z", null, (MathQuizDifficulty) null);

        assertEquals("", question.getQuestionText());
        assertEquals("A", question.getCorrectAnswer());
        assertEquals(MathQuizDifficulty.MEDIUM, question.getDifficulty());

        question.setSelectedAnswer(" d ");
        assertEquals("D", question.getSelectedAnswer());
        question.setSelectedAnswer("X");
        assertEquals("", question.getSelectedAnswer());
    }
}
