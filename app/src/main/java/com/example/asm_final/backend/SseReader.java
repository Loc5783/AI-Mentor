package com.example.asm_final.backend;

import java.io.*;

/** Bounded SSE framing independent of Android, authentication and event payloads. */
public final class SseReader {
  public interface Receiver {
    void receive(String id, String event, String data) throws Exception;
  }

  public static void read(Reader input, Receiver receiver) throws Exception {
    StringBuilder line = new StringBuilder(), data = new StringBuilder();
    String id = "", event = "";
    boolean afterCr = false;
    int next;
    while ((next = input.read()) != -1) {
      char c = (char) next;
      if (afterCr && c == '\n') {
        afterCr = false;
        continue;
      }
      afterCr = c == '\r';
      if (c != '\r' && c != '\n') {
        if (line.length() >= 16384) throw new IOException("SSE line too large");
        line.append(c);
        continue;
      }
      String value = line.toString();
      line.setLength(0);
      if (value.isEmpty()) {
        if (data.length() > 0) receiver.receive(id, event, data.substring(0, data.length() - 1));
        data.setLength(0);
        event = "";
        continue;
      }
      if (value.startsWith(":")) continue;
      int colon = value.indexOf(':');
      String field = colon < 0 ? value : value.substring(0, colon);
      String content = colon < 0 ? "" : value.substring(colon + 1);
      if (content.startsWith(" ")) content = content.substring(1);
      if (field.equals("id") && content.indexOf('\0') < 0) id = content;
      else if (field.equals("event")) event = content;
      else if (field.equals("data")) {
        if (data.length() + content.length() > 65536) throw new IOException("SSE event too large");
        data.append(content).append('\n');
      }
    }
    // An incomplete event at EOF is deliberately discarded; reconnect resumes its previous ID.
  }
}
