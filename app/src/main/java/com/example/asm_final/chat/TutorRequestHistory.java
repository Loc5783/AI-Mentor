package com.example.asm_final.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Keeps the latest completed turns plus the current question for bounded AI context. */
public final class TutorRequestHistory {
  private TutorRequestHistory() {}

  public static List<ChatRecords.Message> select(
      List<ChatRecords.Message> messages, String messageId) {
    List<ChatRecords.Message> recent = new ArrayList<>(20);
    for (int i = 0; i < messages.size(); i++) {
      ChatRecords.Message message = messages.get(i);
      if (!message.role.equals("USER")) continue;
      if (message.id.equals(messageId)) {
        recent.add(message);
        return Collections.unmodifiableList(
            new ArrayList<>(recent.subList(Math.max(0, recent.size() - 21), recent.size())));
      }
      if (message.status.equals("SENT")
          && i + 1 < messages.size()
          && messages.get(i + 1).role.equals("ASSISTANT")) {
        recent.add(message);
        recent.add(messages.get(i + 1));
        if (recent.size() > 20) {
          recent.remove(0);
          recent.remove(0);
        }
      }
    }
    throw new IllegalArgumentException("Missing owned question");
  }
}
