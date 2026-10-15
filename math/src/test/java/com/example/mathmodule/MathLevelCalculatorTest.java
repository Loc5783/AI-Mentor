package com.example.mathmodule;

import com.example.mathmodule.util.MathLevelCalculator;
import com.example.mathmodule.R;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MathLevelCalculatorTest {
    @Test
    public void calculatesMathLevelAtExpBoundaries() {
        assertEquals(1, MathLevelCalculator.getLevel(-50));
        assertEquals(1, MathLevelCalculator.getLevel(119));
        assertEquals(2, MathLevelCalculator.getLevel(120));
        assertEquals(3, MathLevelCalculator.getLevel(265));
    }

    @Test
    public void calculatesExpInsideAndRemainingForCurrentLevel() {
        assertEquals(25, MathLevelCalculator.getInsideLevel(145));
        assertEquals(120, MathLevelCalculator.getRemaining(145));
        assertEquals(145, MathLevelCalculator.getExpToNextLevel(2));
    }

    @Test
    public void capsLevelAndUsesMathSpecificTitles() {
        int maxExp = MathLevelCalculator.getTotalExpForLevel(MathLevelCalculator.MAX_LEVEL);
        assertEquals(MathLevelCalculator.MAX_LEVEL, MathLevelCalculator.getLevel(maxExp + 10_000));
        assertEquals(0, MathLevelCalculator.getRemaining(maxExp));
        assertEquals(R.string.math_level_6, MathLevelCalculator.getTitleResId(20));
    }
}
