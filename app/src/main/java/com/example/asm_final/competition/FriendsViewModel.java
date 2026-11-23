package com.example.asm_final.competition;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.asm_final.AiMentorApplication;
import com.example.asm_final.backend.BackendClient;
import com.example.mentor.data.ActiveUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntSupplier;

/** Owns account-scoped friendship and learner-search state. */
public final class FriendsViewModel extends AndroidViewModel {
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

  public static final class SearchItem {
    public final String id, nickname;

    public SearchItem(String id, String nickname) {
      this.id = id;
      this.nickname = nickname;
    }
  }

  public static final class SearchState {
    public final List<SearchItem> items;
    public final boolean loading, failed, searched;

    SearchState(List<SearchItem> items, boolean loading, boolean failed, boolean searched) {
      this.items = Collections.unmodifiableList(new ArrayList<>(items));
      this.loading = loading;
      this.failed = failed;
      this.searched = searched;
    }
  }

  private final FriendshipRepository friendships;
  private final LearnerDirectory directory;
  private final IntSupplier activeUser;
  private final Executor network;
  private final Executor main;
  private final int owner;
  private final MutableLiveData<FriendsState> friendsState =
      new MutableLiveData<>(new FriendsState(Collections.emptyList(), true, false, ""));
  private final MutableLiveData<SearchState> searchState =
      new MutableLiveData<>(new SearchState(Collections.emptyList(), false, false, false));
  private int searchGeneration, friendsGeneration;
  private boolean refreshing, pendingRefresh, mutating;
  private volatile boolean closed;

  public FriendsViewModel(Application app) {
    this(app, ((AiMentorApplication) app).services().friendships(),
        ((AiMentorApplication) app).services().learnerDirectory(),
        () -> ActiveUser.get(app), Executors.newSingleThreadExecutor(),
        new Handler(Looper.getMainLooper())::post);
  }

  FriendsViewModel(Application app, FriendshipRepository friendships, LearnerDirectory directory,
      IntSupplier activeUser, Executor network, Executor main) {
    super(app);
    this.friendships = friendships;
    this.directory = directory;
    this.activeUser = activeUser;
    this.network = network;
    this.main = main;
    owner = activeUser.getAsInt();
  }

  private static String friendFailureCode(Exception failure, String fallback) {
    if (failure instanceof java.io.IOException) return fallback.replace("_FAILED", "_OFFLINE");
    if (failure instanceof BackendClient.Failure
        && ((BackendClient.Failure) failure).status == 401)
      return fallback.replace("_FAILED", "_LOGIN_REQUIRED");
    return fallback;
  }

  private boolean current() { return !closed && activeUser.getAsInt() == owner; }

  private static boolean confirmation(String message) {
    return "REQUEST_SENT".equals(message) || "FRIEND_ACCEPTED".equals(message)
        || "REQUEST_DECLINED".equals(message) || "FRIEND_REMOVED".equals(message);
  }

  public LiveData<FriendsState> friendsState() { return friendsState; }

  public LiveData<SearchState> searchState() { return searchState; }

  public void searchLearners(String text) {
    if (!current()) return;
    String query = text == null ? "" : text.trim();
    int version = ++searchGeneration;
    if (query.length() < 2 || query.length() > 40) {
      searchState.setValue(new SearchState(Collections.emptyList(), false, false, false));
      return;
    }
    searchState.setValue(new SearchState(Collections.emptyList(), true, false, true));
    network.execute(() -> {
      try {
        String me = directory.currentProfileId(owner);
        if (me == null || me.isBlank()) throw new IllegalStateException("Account link unavailable");
        List<SearchItem> found = new ArrayList<>();
        for (SearchItem item : directory.search(owner, query))
          if (!me.equals(item.id)) found.add(item);
        main.execute(() -> {
          if (current() && version == searchGeneration)
            searchState.setValue(new SearchState(found, false, false, true));
        });
      } catch (Exception failure) {
        main.execute(() -> {
          if (current() && version == searchGeneration)
            searchState.setValue(new SearchState(Collections.emptyList(), false, true, true));
        });
      }
    });
  }

  public void refreshFriends() {
    if (!current()) return;
    if (mutating || refreshing) {
      pendingRefresh = true;
      return;
    }
    refreshing = true;
    int version = ++friendsGeneration;
    FriendsState old = friendsState.getValue();
    friendsState.setValue(new FriendsState(old == null ? Collections.emptyList() : old.items,
        true, false, old != null && confirmation(old.message) ? old.message : ""));
    network.execute(() -> {
      try {
        List<FriendshipRepository.Item> loaded = friendships.list(owner);
        main.execute(() -> {
          refreshing = false;
          if (current() && version == friendsGeneration && !mutating) {
            FriendsState previous = friendsState.getValue();
            String message = previous != null && confirmation(previous.message) ? previous.message : "";
            friendsState.setValue(new FriendsState(loaded, false, false, message));
          }
          refreshPending();
        });
      } catch (Exception failure) {
        main.execute(() -> {
          refreshing = false;
          if (current() && version == friendsGeneration && !mutating) {
            FriendsState previous = friendsState.getValue();
            friendsState.setValue(new FriendsState(previous == null ? Collections.emptyList() : previous.items,
                false, true, friendFailureCode(failure, "LOAD_FAILED")));
          }
          refreshPending();
        });
      }
    });
  }

  private void refreshPending() {
    if (current() && pendingRefresh && !mutating && !refreshing) {
      pendingRefresh = false;
      refreshFriends();
    }
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
    if (!current() || mutating) return;
    mutating = true;
    ++friendsGeneration;
    FriendsState old = friendsState.getValue();
    List<FriendshipRepository.Item> fallbackItems = old == null ? Collections.emptyList() : old.items;
    friendsState.setValue(new FriendsState(fallbackItems, true, false, ""));
    network.execute(() -> {
      try {
        mutation.run();
      } catch (Exception failure) {
        main.execute(() -> {
          mutating = false;
          if (current())
            friendsState.setValue(new FriendsState(fallbackItems, false, true,
                friendFailureCode(failure, "ACTION_FAILED")));
          // A queued invalidation must not erase the action error before it is seen.
          pendingRefresh = false;
        });
        return;
      }
      // A confirmed mutation stays successful even if the follow-up list fails.
      List<FriendshipRepository.Item> loaded = fallbackItems;
      boolean listLoaded = false;
      try {
        loaded = friendships.list(owner);
        listLoaded = true;
      } catch (Exception ignored) { }
      List<FriendshipRepository.Item> result = loaded;
      boolean refreshed = listLoaded;
      main.execute(() -> {
        mutating = false;
        if (current()) friendsState.setValue(new FriendsState(result, false, false, successCode));
        if (refreshed) refreshPending();
        else pendingRefresh = false;
      });
    });
  }

  @Override
  protected void onCleared() {
    closed = true;
    if (network instanceof ExecutorService) ((ExecutorService) network).shutdownNow();
  }
}
