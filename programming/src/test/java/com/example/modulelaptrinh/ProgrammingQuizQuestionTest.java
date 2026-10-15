package com.example.modulelaptrinh;

import com.example.modulelaptrinh.models.QuizDifficulty;
import com.example.modulelaptrinh.models.QuizQuestion;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ProgrammingQuizQuestionTest {
    @Test
    public void infersProgrammingDifficultyAndRewardFromQuestionType() {
        QuizQuestion question = new QuizQuestion(
                QuizQuestion.Type.FILL_BLANK,
                "Điền từ khóa để khai báo lớp trong Java",
                Arrays.asList(),
                "class",
                "Java dùng từ khóa class.");

        assertEquals(QuizDifficulty.HARD, question.getDifficulty());
        assertEquals(20, question.getExpReward());
    }

    @Test
    public void protectsOptionsAndUsesSafeDefaults() {
        List<String> options = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
        QuizQuestion question = new QuizQuestion(
                null, null, options, null, null, null);
        options.set(0, "Changed");

        assertEquals(QuizQuestion.Type.MULTIPLE_CHOICE, question.getType());
        assertEquals("", question.getPrompt());
        assertEquals("A", question.getOptions().get(0));
        assertEquals("", question.getCorrectAnswer());
        assertEquals(QuizDifficulty.MEDIUM, question.getDifficulty());
    }
}
