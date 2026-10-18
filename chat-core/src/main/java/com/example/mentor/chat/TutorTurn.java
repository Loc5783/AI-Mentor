package com.example.mentor.chat;

import java.util.*;

public final class TutorTurn {
  public enum Role {
    USER,
    ASSISTANT
  }

  public final int userId;
  public final Role role;
  public final String text;
  public final List<TutorAttachment> attachments;

  public TutorTurn(int userId, Role role, String text, List<TutorAttachment> attachments) {
    if (userId <= 0 || role == null || text == null || text.length() > 30000 || attachments == null)
      throw new IllegalArgumentException("Invalid chat turn");
    ArrayList<TutorAttachment> copy = new ArrayList<>(attachments);
    if (copy.size() > 4
        || copy.contains(null)
        || role == Role.ASSISTANT && !copy.isEmpty()
        || text.trim().isEmpty() && copy.isEmpty())
      throw new IllegalArgumentException("Invalid chat content");
    this.userId = userId;
    this.role = role;
    this.text = text;
    this.attachments = Collections.unmodifiableList(copy);
  }
}
