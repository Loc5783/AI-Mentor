package com.example.historymodule;

import com.example.historymodule.model.HistoryQuizQuestion;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class HistoryQuizQuestionTest {
    @Test
    public void normalizesHistoryDifficultyAndRewards() {
        HistoryQuizQuestion easy = question("easy", 1);
        HistoryQuizQuestion hard = question("HARD", 2);
        HistoryQuizQuestion unknown = question("expert", 0);

        assertEquals("EASY", easy.getDifficulty());
        assertEquals("Dễ", easy.getDifficultyDisplayName());
        assertEquals(5, easy.getExpReward());
        assertEquals(20, hard.getExpReward());
        assertEquals("MEDIUM", unknown.getDifficulty());
        assertEquals(10, unknown.getExpReward());
    }

    @Test
    public void clampsCorrectIndexAndProtectsHistoryOptions() {
        List<String> source = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
        HistoryQuizQuestion question = new HistoryQuizQuestion(
                "MEDIUM", "  Mốc lịch sử?  ", source, 9, "  Giải thích  ");
        source.set(0, "Đã thay đổi");

        assertEquals("Mốc lịch sử?", question.getQuestion());
        assertEquals(3, question.getCorrectIndex());
        assertEquals("A", question.getOptions().get(0));
        assertEquals("Giải thích", question.getExplanation());
    }

    private HistoryQuizQuestion question(String difficulty, int correctIndex) {
        return new HistoryQuizQuestion(
                difficulty,
                "Sự kiện nào xảy ra trước?",
                Arrays.asList("A", "B", "C", "D"),
                correctIndex,
                "Dựa trên dòng thời gian.");
    }
}
