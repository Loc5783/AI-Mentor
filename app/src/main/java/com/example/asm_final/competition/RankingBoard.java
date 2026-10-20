package com.example.asm_final.competition;

import java.util.*;

/** Validated competition ranks: tied scores share rank and consume positions (1,1,3). */
public final class RankingBoard {
  public static final class Entry {
    public final int rank;
    public final long value;
    public final PublicLearner learner;

    public Entry(int rank, long value, PublicLearner learner) {
      this.rank = rank;
      this.value = value;
      this.learner = learner;
    }
  }

  public final CompetitionQuery query;
  public final List<Entry> entries;
  public final String learningDay;

  public RankingBoard(CompetitionQuery query, String day, List<Entry> entries) {
    if (query == null || entries.size() > 100) throw new IllegalArgumentException("Invalid board");
    Set<String> ids = new HashSet<>();
    long previous = Long.MAX_VALUE;
    int rank = 0;
    for (int i = 0; i < entries.size(); i++) {
      Entry entry = entries.get(i);
      if (entry == null
          || entry.learner == null
          || !ids.add(entry.learner.id)
          || !entry.learner.learningDay.equals(day)
          || entry.value < 0
          || entry.value > previous) throw new IllegalArgumentException("Invalid ranking order");
      if (i == 0 || entry.value != previous) rank = i + 1;
      long expected =
          query.metric == CompetitionQuery.Metric.STREAK
              ? entry.learner.streakDays
              : entry.learner.dailyXp;
      if (entry.rank != rank || entry.value != expected)
        throw new IllegalArgumentException("Invalid ranking value");
      previous = entry.value;
    }
    this.query = query;
    learningDay = day;
    this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
  }
}
