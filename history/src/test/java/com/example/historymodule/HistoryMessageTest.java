package com.example.historymodule;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HistoryMessageTest {
    @Test
    public void pendingMessageRepresentsHistoryAiAnalysisState() {
        HistoryMessage message = HistoryMessage.pending();

        assertEquals(HistoryMessage.SENT_BY_BOT, message.getSentBy());
        assertEquals("AI đang phân tích...", message.getMessage());
        assertTrue(message.isPending());
        assertFalse(message.hasImageAttached());
    }

    @Test
    public void nullFieldsAreSafeAndStateCanBeUpdated() {
        HistoryMessage message = new HistoryMessage(null, null, false, true);

        assertEquals("", message.getMessage());
        assertEquals(HistoryMessage.SENT_BY_BOT, message.getSentBy());
        assertTrue(message.hasImageAttached());

        message.setMessage("Phân tích sự kiện lịch sử");
        message.setPending(true);
        assertEquals("Phân tích sự kiện lịch sử", message.getMessage());
        assertTrue(message.isPending());
    }
}
