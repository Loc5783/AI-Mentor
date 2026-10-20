package com.example.asm_final.dashboard;

import com.example.asm_final.utils.RecentActivity.Subject;

/** Missing sources are different from a learner having no recorded activity. */
public final class DashboardChartState {
  public enum Phase {
    LOADING,
    EMPTY,
    READY,
    PARTIAL,
    ERROR
  }

  public final Phase phase;
  public final boolean showChart;

  private DashboardChartState(Phase phase, boolean showChart) {
    this.phase = phase;
    this.showChart = showChart;
  }

  public static DashboardChartState loading() {
    return new DashboardChartState(Phase.LOADING, false);
  }

  public static DashboardChartState recorded(boolean hasValues, boolean stale) {
    return new DashboardChartState(
        stale ? Phase.PARTIAL : hasValues ? Phase.READY : Phase.EMPTY, hasValues);
  }

  public static DashboardChartState unavailable() {
    return new DashboardChartState(Phase.ERROR, false);
  }

  public static DashboardChartState from(DashboardSnapshot snapshot) {
    boolean hasValues = false;
    int available = 0;
    for (Subject subject : Subject.values()) {
      if (snapshot.unavailable.contains(subject)) continue;
      available++;
      hasValues |= snapshot.progress(subject).experience > 0;
    }
    Phase phase =
        available == 0
            ? Phase.ERROR
            : !snapshot.unavailable.isEmpty()
                ? Phase.PARTIAL
                : hasValues ? Phase.READY : Phase.EMPTY;
    return new DashboardChartState(phase, hasValues);
  }

  public boolean retry() {
    return phase == Phase.PARTIAL || phase == Phase.ERROR;
  }
}
