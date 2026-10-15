package com.example.historymodule;

import com.example.historymodule.ai.HistoryQuizParser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HistoryQuizParserTest {
    @Test
    public void extractsHistoryQuizArrayFromMarkdownResponse() {
        String response = "```json\n[{\"question\":\"Chiến thắng Điện Biên Phủ diễn ra năm nào?\"}]\n```";

        assertEquals("[{\"question\":\"Chiến thắng Điện Biên Phủ diễn ra năm nào?\"}]",
                HistoryQuizParser.cleanJson(response));
    }

    @Test
    public void malformedHistoryQuizResponseReturnsEmptyArray() {
        assertEquals("[]", HistoryQuizParser.cleanJson(null));
        assertEquals("[]", HistoryQuizParser.cleanJson("Không có JSON"));
        assertEquals("[]", HistoryQuizParser.cleanJson("[{\"question\":\"thiếu đóng\"}"));
    }
}
