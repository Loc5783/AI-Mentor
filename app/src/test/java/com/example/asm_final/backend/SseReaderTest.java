package com.example.asm_final.backend;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import org.junit.Test;

public class SseReaderTest {
  private List<String> parse(String input) throws Exception {
    List<String> values = new ArrayList<>();
    SseReader.read(
        new StringReader(input), (id, event, data) -> values.add(id + "/" + event + "/" + data));
    return values;
  }

  @Test
  public void framesCrLfAndMultilineDataIgnoringHeartbeats() throws Exception {
    assertEquals(
        Collections.singletonList("12/change/first\nsecond"),
        parse(": heartbeat\r\n\r\nid: 12\r\nevent: change\r\ndata: first\r\ndata:second\r\n\r\n"));
  }

  @Test
  public void eofDoesNotAcknowledgeAnIncompleteEvent() throws Exception {
    assertTrue(parse("id: 7\ndata: incomplete\n").isEmpty());
  }

  @Test
  public void eventNameResetsAndIdsPersistPerSseSpec() throws Exception {
    assertEquals(
        Arrays.asList("3/change/a", "3//b"), parse("id:3\nevent:change\ndata:a\n\ndata:b\n\n"));
  }

  @Test
  public void supportsCrOnlyAndIgnoresNullContainingId() throws Exception {
    assertEquals(Arrays.asList("4//a", "4//b"), parse("id:4\rdata:a\r\rid:5\0bad\rdata:b\r\r"));
  }

  @Test
  public void boundsIndividualLinesBeforeAllocatingAnUnboundedString() throws Exception {
    try {
      parse("data:" + "x".repeat(16384));
      fail();
    } catch (IOException expected) {
    }
  }

  @Test
  public void boundsTotalMultilineEvent() throws Exception {
    try {
      parse(("data:" + "x".repeat(12000) + "\n").repeat(6));
      fail();
    } catch (IOException expected) {
    }
  }
}
