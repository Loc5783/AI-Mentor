package com.example.historymodule;

import com.example.historymodule.model.HistoryLevelCalculator;
import com.example.historymodule.R;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HistoryLevelCalculatorTest {
    @Test
    public void calculatesHistoryLevelAtExpBoundaries() {
        assertEquals(1, HistoryLevelCalculator.getLevel(-1));
        assertEquals(1, HistoryLevelCalculator.getLevel(119));
        assertEquals(2, HistoryLevelCalculator.getLevel(120));
        assertEquals(3, HistoryLevelCalculator.getLevel(265));
    }

    @Test
    public void calculatesHistoryProgressInsideCurrentLevel() {
        assertEquals(25, HistoryLevelCalculator.getExpInsideCurrentLevel(145));
        assertEquals(17, HistoryLevelCalculator.getProgressPercent(145));
        assertEquals(145, HistoryLevelCalculator.getExpToNextLevel(2));
    }

    @Test
    public void capsAtMaximumAndUsesHistorySpecificTitles() {
        int maxExp = HistoryLevelCalculator.getTotalExpForLevel(HistoryLevelCalculator.MAX_LEVEL);
        assertEquals(HistoryLevelCalculator.MAX_LEVEL,
                HistoryLevelCalculator.getLevel(maxExp + 99_999));
        assertEquals(100, HistoryLevelCalculator.getProgressPercent(maxExp));
        assertEquals(R.string.history_level_6, HistoryLevelCalculator.getTitleResId(20));
    }
}
