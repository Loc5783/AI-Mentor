package com.example.asm_final.competition;

public interface CompetitionRepository {
  enum Source {
    LOCAL,
    LIVE,
    UNAVAILABLE
  }

  final class Snapshot {
    public final Source source;
    public final RankingBoard board;
    public final Long dailyXp;
    public final Integer streak;
    public final boolean cached, needsLogin;

    public Snapshot(
        Source source,
        RankingBoard board,
        Long xp,
        Integer streak,
        boolean cached,
        boolean needsLogin) {
      this.source = source;
      this.board = board;
      dailyXp = xp;
      this.streak = streak;
      this.cached = cached;
      this.needsLogin = needsLogin;
    }
  }

  Snapshot load(int owner, CompetitionQuery query);
}
