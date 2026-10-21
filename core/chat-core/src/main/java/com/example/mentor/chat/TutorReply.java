package com.example.mentor.chat;

public final class TutorReply {
  public final String text;
  public final boolean truncated;

  public TutorReply(String text, boolean truncated) {
    if (text == null || text.trim().isEmpty())
      throw new IllegalArgumentException("Empty tutor response");
    this.text = text;
    this.truncated = truncated;
  }
}
