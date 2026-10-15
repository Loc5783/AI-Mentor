package com.example.modulelaptrinh;

import com.example.modulelaptrinh.utils.ProgrammingLevelCalculator;
import com.example.modulelaptrinh.R;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProgrammingLevelCalculatorTest {
    @Test
    public void calculatesProgrammingLevelAtExpBoundaries() {
        assertEquals(1, ProgrammingLevelCalculator.getLevel(-20));
        assertEquals(1, ProgrammingLevelCalculator.getLevel(99));
        assertEquals(2, ProgrammingLevelCalculator.getLevel(100));
        assertEquals(3, ProgrammingLevelCalculator.getLevel(220));
    }

    @Test
    public void calculatesProgrammingProgressInsideLevel() {
        assertEquals(50, ProgrammingLevelCalculator.getExpInsideCurrentLevel(150));
        assertEquals(42, ProgrammingLevelCalculator.getProgressPercent(150));
        assertEquals(120, ProgrammingLevelCalculator.getExpToNextLevel(2));
    }

    @Test
    public void capsAtMaximumAndUsesProgrammingSpecificTitles() {
        int maxExp = ProgrammingLevelCalculator.getTotalExpForLevel(
                ProgrammingLevelCalculator.MAX_LEVEL);
        assertEquals(ProgrammingLevelCalculator.MAX_LEVEL,
                ProgrammingLevelCalculator.getLevel(maxExp + 50_000));
        assertEquals(100, ProgrammingLevelCalculator.getProgressPercent(maxExp));
        assertEquals(R.string.prog_level_5, ProgrammingLevelCalculator.getTitleResId(20));
    }
}
