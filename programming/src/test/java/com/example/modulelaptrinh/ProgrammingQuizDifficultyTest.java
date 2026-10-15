package com.example.modulelaptrinh;

import com.example.modulelaptrinh.models.QuizDifficulty;
import com.example.modulelaptrinh.models.QuizQuestion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProgrammingQuizDifficultyTest {
    @Test
    public void mapsProgrammingDifficultyFromApiValues() {
        assertEquals(QuizDifficulty.EASY, QuizDifficulty.fromApiValue("EASY"));
        assertEquals(QuizDifficulty.EASY, QuizDifficulty.fromApiValue("Dễ"));
        assertEquals(QuizDifficulty.HARD, QuizDifficulty.fromApiValue("Khó"));
        assertEquals(QuizDifficulty.MEDIUM, QuizDifficulty.fromApiValue("unknown"));
        assertEquals(QuizDifficulty.MEDIUM, QuizDifficulty.fromApiValue(null));
    }

    @Test
    public void infersDifficultyFromProgrammingQuestionType() {
        assertEquals(QuizDifficulty.EASY,
                QuizDifficulty.inferFromType(QuizQuestion.Type.TRUE_FALSE));
        assertEquals(QuizDifficulty.HARD,
                QuizDifficulty.inferFromType(QuizQuestion.Type.FILL_BLANK));
        assertEquals(QuizDifficulty.MEDIUM,
                QuizDifficulty.inferFromType(QuizQuestion.Type.MULTIPLE_CHOICE));
        assertEquals(QuizDifficulty.MEDIUM,
                QuizDifficulty.inferFromType(null));
    }
}
