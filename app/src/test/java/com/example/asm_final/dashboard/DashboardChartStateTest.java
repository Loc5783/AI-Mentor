package com.example.asm_final.dashboard;

import static org.junit.Assert.*;

import com.example.asm_final.utils.RecentActivity.Subject;
import java.util.*;
import org.junit.Test;

public class DashboardChartStateTest {
  private DashboardSnapshot snapshot(Set<Subject> missing, int xp) {
    EnumMap<Subject, SubjectProgress> progress = new EnumMap<>(Subject.class);
    progress.put(Subject.SCIENCE, new SubjectProgress(xp, Collections.emptyList()));
    return new DashboardSnapshot(progress, Collections.emptyList(), missing);
  }

  @Test
  public void freshLearnerGetsAnInvitationRatherThanAnEmptyChart() {
    DashboardChartState state = DashboardChartState.from(snapshot(Collections.emptySet(), 0));
    assertEquals(DashboardChartState.Phase.EMPTY, state.phase);
    assertFalse(state.showChart);
    assertFalse(state.retry());
  }

  @Test
  public void failedReadsMustNotSayTheLearnerHasNotStudied() {
    DashboardChartState state = DashboardChartState.from(snapshot(EnumSet.allOf(Subject.class), 0));
    assertEquals(DashboardChartState.Phase.ERROR, state.phase);
    assertFalse(state.showChart);
    assertTrue(state.retry());
  }

  @Test
  public void aPartialReadWithoutValuesStillNeedsRetryNotStart() {
    DashboardChartState state = DashboardChartState.from(snapshot(EnumSet.of(Subject.MATH), 0));
    assertEquals(DashboardChartState.Phase.PARTIAL, state.phase);
    assertFalse(state.showChart);
    assertTrue(state.retry());
  }

  @Test
  public void availableValuesRemainVisibleWhenAnotherSourceFails() {
    DashboardChartState state = DashboardChartState.from(snapshot(EnumSet.of(Subject.MATH), 200));
    assertEquals(DashboardChartState.Phase.PARTIAL, state.phase);
    assertTrue(state.showChart);
    assertTrue(state.retry());
  }

  @Test
  public void anUnavailableSourcesStaleValueMustNotCreateAChart() {
    DashboardChartState state =
        DashboardChartState.from(snapshot(EnumSet.of(Subject.SCIENCE), 200));
    assertFalse(state.showChart);
    assertTrue(state.retry());
  }

  @Test
  public void recordedProgressReplacesTheInvitationWithRealData() {
    DashboardChartState state = DashboardChartState.from(snapshot(Collections.emptySet(), 200));
    assertEquals(DashboardChartState.Phase.READY, state.phase);
    assertTrue(state.showChart);
    assertFalse(state.retry());
  }
}
