package com.example.mentor.chat;

import java.util.*;

/** In-memory upload payload, not a public device path. UI must copy and validate selected files. */
public final class TutorAttachment {
  public static final int MAX_BYTES = 4 * 1024 * 1024;
  private static final Set<String> ALLOWED =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  "image/jpeg", "image/png", "image/webp", "application/pdf", "text/plain")));
  public final String name, mimeType;
  private final byte[] payload;

  public TutorAttachment(String name, String mimeType, byte[] bytes) {
    if (name == null
        || name.trim().isEmpty()
        || !ALLOWED.contains(mimeType)
        || bytes == null
        || bytes.length == 0
        || bytes.length > MAX_BYTES)
      throw new IllegalArgumentException("Unsupported or oversized attachment");
    this.name = name;
    this.mimeType = mimeType;
    payload = bytes.clone();
  }

  public byte[] bytes() {
    return payload.clone();
  }

  public int size() {
    return payload.length;
  }
}
