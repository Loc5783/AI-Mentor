package com.example.asm_final.competition;

import android.app.Application;
import android.os.*;
import androidx.lifecycle.*;
import com.example.asm_final.AiMentorApplication;
import com.example.asm_final.accounts.RemoteAccountLinks;
import com.example.asm_final.backend.BackendClient;
import com.example.asm_final.supabase.SupabaseHttp;
import com.example.mentor.data.ActiveUser;
import java.util.concurrent.*;
import java.util.*;

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

  public static final class FriendsState {
    public final List<FriendshipRepository.Item> items;
    public final boolean loading, failed;
    public final String message;

    FriendsState(List<FriendshipRepository.Item> items, boolean loading, boolean failed, String message) {
      this.items = Collections.unmodifiableList(new ArrayList<>(items));
      this.loading = loading;
      this.failed = failed;
      this.message = message;
    }
  }

  private final CompetitionRepository repository;
  private final FriendshipRepository friendships;
  private final SavedStateHandle saved;
  private final int owner;
  private volatile String profileId = "";
  private final ExecutorService network = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final MutableLiveData<State> state = new MutableLiveData<>();
  private final MutableLiveData<FriendsState> friendsState = new MutableLiveData<>(new FriendsState(Collections.emptyList(), true, false, ""));
  private boolean running, pending;
  private volatile boolean closed;
  private CompetitionQuery query;

  public CompetitionViewModel(Application app, SavedStateHandle saved) {
    this(app, saved, ((AiMentorApplication) app).services().competition());
  }

  public CompetitionViewModel(
      Application app, SavedStateHandle saved, CompetitionRepository repository) {
    this(app, saved, repository, ((AiMentorApplication) app).services().friendships());
  }

  public CompetitionViewModel(
      Application app, SavedStateHandle saved, CompetitionRepository repository, FriendshipRepository friendships) {
    super(app);
    this.saved = saved;
    owner = ActiveUser.get(app);
    this.repository = repository;
    this.friendships = friendships;
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

  public LiveData<FriendsState> friendsState() { return friendsState; }

  public boolean isCurrentLearner(String id) { return id != null && !profileId.isEmpty() && profileId.equals(id); }

  private void resolveProfileId() {
    if (!profileId.isEmpty()) return;
    try (RemoteAccountLinks links = new RemoteAccountLinks(getApplication())) {
      String endpoint = SupabaseHttp.enabled(getApplication())
          ? com.example.asm_final.BuildConfig.SUPABASE_URL : BackendClient.endpoint(getApplication());
      profileId = links.remoteId(owner, endpoint);
    } catch (Exception ignored) {
      // Ranking remains useful even if a local profile has no remote link yet.
    }
  }

  public void refreshFriends() {
    if (closed || ActiveUser.get(getApplication()) != owner) return;
    FriendsState old = friendsState.getValue();
    friendsState.setValue(new FriendsState(old == null ? Collections.emptyList() : old.items, true, false, ""));
    network.execute(() -> {
      try {
        resolveProfileId();
        List<FriendshipRepository.Item> loaded = friendships.list(owner);
        main.post(() -> {
          if (!closed && ActiveUser.get(getApplication()) == owner)
            friendsState.setValue(new FriendsState(loaded, false, false, ""));
        });
      } catch (Exception failure) {
        main.post(() -> {
          if (!closed && ActiveUser.get(getApplication()) == owner) {
            FriendsState previous = friendsState.getValue();
            friendsState.setValue(new FriendsState(previous == null ? Collections.emptyList() : previous.items, false, true, "LOAD_FAILED"));
          }
        });
      }
    });
  }

  public void sendFriendRequest(String profileId) {
    runFriendOperation("REQUEST_SENT", () -> friendships.request(owner, profileId, UUID.randomUUID().toString()));
  }

  public void decideFriendship(FriendshipRepository.Item item, String decision) {
    runFriendOperation("ACCEPT".equals(decision) ? "FRIEND_ACCEPTED" : "REQUEST_DECLINED",
        () -> friendships.decide(owner, item.id, decision, item.revision));
  }

  public void removeFriend(FriendshipRepository.Item item) {
    runFriendOperation("FRIEND_REMOVED", () -> { friendships.remove(owner, item.id, item.revision); return null; });
  }

  private interface FriendMutation { FriendshipRepository.Item run() throws Exception; }

  private void runFriendOperation(String successCode, FriendMutation mutation) {
    if (closed || ActiveUser.get(getApplication()) != owner) return;
    FriendsState old = friendsState.getValue();
    friendsState.setValue(new FriendsState(old == null ? Collections.emptyList() : old.items, true, false, ""));
    network.execute(() -> {
      try {
        mutation.run();
        List<FriendshipRepository.Item> loaded = friendships.list(owner);
        main.post(() -> {
          if (!closed && ActiveUser.get(getApplication()) == owner)
            friendsState.setValue(new FriendsState(loaded, false, false, successCode));
        });
      } catch (Exception failure) {
        main.post(() -> {
          if (!closed && ActiveUser.get(getApplication()) == owner) {
            FriendsState previous = friendsState.getValue();
            friendsState.setValue(new FriendsState(previous == null ? Collections.emptyList() : previous.items, false, true, "ACTION_FAILED"));
          }
        });
      }
    });
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
          resolveProfileId();
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
