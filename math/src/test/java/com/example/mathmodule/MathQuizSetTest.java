package com.example.mathmodule;

import com.example.mathmodule.databases.QuizSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathQuizSetTest {
    @Test
    public void keepsMathQuizMetadataAndTrimsEducationLevel() {
        QuizSet set = new QuizSet(4, "Đại số", "Phương trình", "Khó", "  High School  ");

        assertEquals(4, set.getId());
        assertEquals("Đại số", set.getTitle());
        assertEquals("Phương trình", set.getCategory());
        assertEquals("Khó", set.getDifficulty());
        assertEquals("High School", set.getEducationLevel());
    }

    @Test
    public void missingValuesUseSafeMathDefaults() {
        QuizSet set = new QuizSet(1, null, null, null, "  ");

        assertEquals("Bộ đề Toán", set.getTitle());
        assertEquals("Khác", set.getCategory());
        assertEquals("Trung bình", set.getDifficulty());
        assertEquals("Middle School", set.getEducationLevel());
    }
}
