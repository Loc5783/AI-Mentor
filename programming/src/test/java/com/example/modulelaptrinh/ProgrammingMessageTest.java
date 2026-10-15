package com.example.modulelaptrinh;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProgrammingMessageTest {
    @Test
    public void startsWithoutImagesAndCanAttachProgrammingScreenshots() {
        ProgrammingMessage message = new ProgrammingMessage("Giải thích lỗi Java", true);

        assertTrue(message.isUser());
        assertFalse(message.hasImages());
        assertEquals(0, message.getImageCount());
        assertNull(message.getFirstImage());

        message.addImage("base64-code-screenshot");
        assertTrue(message.hasImages());
        assertEquals(1, message.getImageCount());
        assertEquals("base64-code-screenshot", message.getFirstImage());
    }

    @Test
    public void nullImageListsAreHandledSafely() {
        ProgrammingMessage message = new ProgrammingMessage("Phân tích code", false, null);
        assertFalse(message.hasImages());

        message.setImageBase64List(Arrays.asList("img-1", "img-2"));
        assertEquals(2, message.getImageCount());
        assertEquals("img-1", message.getFirstImage());

        message.setImageBase64List(null);
        assertFalse(message.hasImages());
    }
}
