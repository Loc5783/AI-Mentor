package com.example.asm_final.competition;

/** Supported ranking axes, independent of UI labels and HTTP. */
public final class CompetitionQuery {
  public enum Metric {
    STREAK,
    DAILY_XP
  }

  public enum Scope {
    FRIENDS,
    GLOBAL
  }

  public final Metric metric;
  public final Scope scope;

  public CompetitionQuery(Metric metric, Scope scope) {
    if (metric == null || scope == null) throw new IllegalArgumentException("Missing ranking axis");
    this.metric = metric;
    this.scope = scope;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof CompetitionQuery
        && ((CompetitionQuery) other).metric == metric
        && ((CompetitionQuery) other).scope == scope;
  }

  @Override
  public int hashCode() {
    return metric.hashCode() * 31 + scope.hashCode();
  }
}
