package com.example.mathmodule;

import com.example.mathmodule.ai.MathQuizParser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathQuizParserTest {
    @Test
    public void extractsJsonArrayFromGeminiMarkdownResponse() {
        String response = "Lời dẫn\n```json\n[{\"question\":\"2 + 2 = ?\"}]\n```\nKết thúc";
        assertEquals("[{\"question\":\"2 + 2 = ?\"}]",
                MathQuizParser.cleanJson(response));
    }

    @Test
    public void invalidOrMissingResponseReturnsEmptyArray() {
        assertEquals("[]", MathQuizParser.cleanJson(null));
        assertEquals("[]", MathQuizParser.cleanJson("Không có dữ liệu JSON"));
        assertEquals("[]", MathQuizParser.cleanJson("[thiếu dấu đóng"));
    }
}
