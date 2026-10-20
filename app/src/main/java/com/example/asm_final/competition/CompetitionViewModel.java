package com.example.asm_final.competition;

import android.app.Application;
import android.os.*;
import androidx.lifecycle.*;
import com.example.asm_final.AiMentorApplication;
import com.example.mentor.data.ActiveUser;
import java.util.concurrent.*;

/** Owns selection and asynchronous state; obsolete results cannot overwrite a newer filter. */
public final class CompetitionViewModel extends AndroidViewModel {
  public static final class State {
    public final CompetitionQuery query;
    public final CompetitionRepository.Snapshot snapshot;
    public final boolean loading;

    State(CompetitionQuery query, CompetitionRepository.Snapshot snapshot, boolean loading) {
      this.query = query;
      this.snapshot = snapshot;
      this.loading = loading;
    }
  }

  private final CompetitionRepository repository;
  private final SavedStateHandle saved;
  private final int owner;
  private final ExecutorService network = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final MutableLiveData<State> state = new MutableLiveData<>();
  private boolean running, pending;
  private volatile boolean closed;
  private CompetitionQuery query;

  public CompetitionViewModel(Application app, SavedStateHandle saved) {
    this(app, saved, ((AiMentorApplication) app).services().competition());
  }

  public CompetitionViewModel(
      Application app, SavedStateHandle saved, CompetitionRepository repository) {
    super(app);
    this.saved = saved;
    owner = ActiveUser.get(app);
    this.repository = repository;
    query =
        new CompetitionQuery(
            Boolean.TRUE.equals(saved.get("dailyXp"))
                ? CompetitionQuery.Metric.DAILY_XP
                : CompetitionQuery.Metric.STREAK,
            Boolean.TRUE.equals(saved.get("global"))
                ? CompetitionQuery.Scope.GLOBAL
                : CompetitionQuery.Scope.FRIENDS);
    state.setValue(new State(query, null, true));
  }

  public LiveData<State> state() {
    return state;
  }

  public CompetitionQuery query() {
    return query;
  }

  public void select(CompetitionQuery selected) {
    if (query.equals(selected)) return;
    query = selected;
    saved.set("dailyXp", selected.metric == CompetitionQuery.Metric.DAILY_XP);
    saved.set("global", selected.scope == CompetitionQuery.Scope.GLOBAL);
    State old = state.getValue();
    CompetitionRepository.Snapshot personal = old == null ? null : old.snapshot;
    if (personal != null)
      personal =
          new CompetitionRepository.Snapshot(
              personal.source,
              null,
              personal.dailyXp,
              personal.streak,
              personal.cached,
              personal.needsLogin);
    state.setValue(new State(query, personal, true));
    refresh();
  }

  public void refresh() {
    if (closed || ActiveUser.get(getApplication()) != owner) return;
    if (running) {
      pending = true;
      return;
    }
    running = true;
    CompetitionQuery selected = query;
    State previous = state.getValue();
    state.setValue(
        new State(
            selected,
            previous != null && selected.equals(previous.query) ? previous.snapshot : null,
            true));
    network.execute(
        () -> {
          if (closed) return;
          CompetitionRepository.Snapshot loaded = repository.load(owner, selected);
          main.post(
              () -> {
                running = false;
                if (closed || ActiveUser.get(getApplication()) != owner) return;
                if (selected.equals(query)) state.setValue(new State(selected, loaded, false));
                if (pending || !selected.equals(query)) {
                  pending = false;
                  refresh();
                }
              });
        });
  }

  @Override
  protected void onCleared() {
    closed = true;
    network.shutdownNow();
  }
}
