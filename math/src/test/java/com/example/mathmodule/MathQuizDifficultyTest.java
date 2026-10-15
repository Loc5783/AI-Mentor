package com.example.mathmodule;

import com.example.mathmodule.util.MathQuizDifficulty;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathQuizDifficultyTest {
    @Test
    public void mapsApiDifficultyValuesToMathDifficulty() {
        assertEquals(MathQuizDifficulty.EASY,
                MathQuizDifficulty.fromApiValue("easy"));
        assertEquals(MathQuizDifficulty.HARD,
                MathQuizDifficulty.fromApiValue("Khó"));
        assertEquals(MathQuizDifficulty.MEDIUM,
                MathQuizDifficulty.fromApiValue("unknown"));
        assertEquals(MathQuizDifficulty.MEDIUM,
                MathQuizDifficulty.fromApiValue(null));
    }

    @Test
    public void rewardsIncreaseWithMathQuestionDifficulty() {
        assertEquals(5, MathQuizDifficulty.EASY.getExpReward());
        assertEquals(10, MathQuizDifficulty.MEDIUM.getExpReward());
        assertEquals(20, MathQuizDifficulty.HARD.getExpReward());
    }
}
