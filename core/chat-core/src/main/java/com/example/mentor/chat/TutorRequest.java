package com.example.mentor.chat;

import com.example.mentor.learning.*;
import java.util.*;

/** Includes ordered successful history and the latest user turn, never another account's data. */
public final class TutorRequest {
  public final LearningContext context;
  public final List<TutorTurn> turns;
  public final LessonHandoff handoff;

  public TutorRequest(LearningContext context, List<TutorTurn> turns, LessonHandoff handoff) {
    if (context == null || turns == null || turns.isEmpty() || turns.size() > 41)
      throw new IllegalArgumentException("Invalid conversation");
    List<TutorTurn> copy = new ArrayList<>(turns);
    long bytes = 0, characters = 0;
    for (int i = 0; i < copy.size(); i++) {
      TutorTurn turn = copy.get(i);
      if (turn == null
          || turn.userId != context.userId
          || turn.role != (i % 2 == 0 ? TutorTurn.Role.USER : TutorTurn.Role.ASSISTANT))
        throw new IllegalArgumentException("Conversation ownership or order mismatch");
      characters += turn.text.length();
      for (TutorAttachment attachment : turn.attachments) bytes += attachment.size();
    }
    if (copy.get(copy.size() - 1).role != TutorTurn.Role.USER
        || bytes > 8L * 1024 * 1024
        || characters > 120000)
      throw new IllegalArgumentException("Conversation is incomplete or exceeds request budget");
    this.context = context;
    this.turns = Collections.unmodifiableList(copy);
    this.handoff = handoff;
  }
}
