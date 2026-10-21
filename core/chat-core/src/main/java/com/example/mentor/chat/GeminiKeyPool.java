package com.example.mentor.chat;

import java.util.*;
import java.util.function.LongSupplier;

/**
 * Same configurable primary/backup/cooldown scheme as legacy chats, isolated per general-chat pool.
 */
public final class GeminiKeyPool {
  private final String[] keys;
  private final long[] disabledUntil;
  private final LongSupplier clock;
  private int current;

  public GeminiKeyPool(String... values) {
    this(System::currentTimeMillis, values);
  }

  GeminiKeyPool(LongSupplier clock, String... values) {
    this.clock = clock;
    LinkedHashSet<String> unique = new LinkedHashSet<>();
    for (String value : values)
      if (value != null && !value.trim().isEmpty()) unique.add(value.trim());
    keys = unique.toArray(new String[0]);
    disabledUntil = new long[keys.length];
  }

  public boolean configured() {
    return keys.length > 0;
  }

  synchronized int next() {
    long now = clock.getAsLong();
    for (int offset = 0; offset < keys.length; offset++) {
      int index = (current + offset) % keys.length;
      if (disabledUntil[index] <= now) {
        current = index;
        return index;
      }
    }
    return -1;
  }

  synchronized String key(int index) {
    return keys[index];
  }

  synchronized void reject(int index, int status) {
    if (status != 401 && status != 403 && status != 429)
      throw new IllegalArgumentException("Only rejected or rate-limited keys rotate");
    disabledUntil[index] = status == 429 ? clock.getAsLong() + 60000L : Long.MAX_VALUE;
    current = (index + 1) % keys.length;
  }

  @Override
  public String toString() {
    return "GeminiKeyPool[redacted]";
  }
}
