package com.example.asm_final.competition;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.asm_final.AiMentorApplication;
import com.example.asm_final.accounts.RemoteAccountLinks;
import com.example.asm_final.backend.BackendClient;
import com.example.asm_final.supabase.SupabaseHttp;
import com.example.mentor.data.ActiveUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
  private final int owner;
  private volatile String profileId = "";
  private final ExecutorService network = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final MutableLiveData<FriendsState> friendsState =
      new MutableLiveData<>(new FriendsState(Collections.emptyList(), true, false, ""));
  private final MutableLiveData<SearchState> searchState =
      new MutableLiveData<>(new SearchState(Collections.emptyList(), false, false, false));
  private int searchGeneration;
  private volatile boolean closed;

  public FriendsViewModel(Application app) {
    this(app, ((AiMentorApplication) app).services().friendships());
  }

  public FriendsViewModel(Application app, FriendshipRepository friendships) {
    super(app);
    owner = ActiveUser.get(app);
    this.friendships = friendships;
  }

  private static String friendFailureCode(Exception failure, String fallback) {
    if (failure instanceof java.io.IOException) return fallback.replace("_FAILED", "_OFFLINE");
    if (failure instanceof BackendClient.Failure
        && ((BackendClient.Failure) failure).status == 401)
      return fallback.replace("_FAILED", "_LOGIN_REQUIRED");
    return fallback;
  }

  public LiveData<FriendsState> friendsState() { return friendsState; }

  public LiveData<SearchState> searchState() { return searchState; }

  public void searchLearners(String text) {
    if (closed || ActiveUser.get(getApplication()) != owner) return;
    String query = text.trim();
    if (query.length() < 2 || query.length() > 40) return;
    int version = ++searchGeneration;
    searchState.setValue(new SearchState(Collections.emptyList(), true, false, true));
    network.execute(() -> {
      try {
        List<SearchItem> found = new SupabaseLearnerSearch(getApplication()).search(owner, query);
        main.post(() -> {
          if (!closed && version == searchGeneration && ActiveUser.get(getApplication()) == owner)
            searchState.setValue(new SearchState(found, false, false, true));
        });
      } catch (Exception failure) {
        main.post(() -> {
          if (!closed && version == searchGeneration && ActiveUser.get(getApplication()) == owner)
            searchState.setValue(new SearchState(Collections.emptyList(), false, true, true));
        });
      }
    });
  }

  public boolean isCurrentLearner(String id) { return id != null && !profileId.isEmpty() && profileId.equals(id); }

  private void resolveProfileId() {
    if (!profileId.isEmpty()) return;
    try (RemoteAccountLinks links = new RemoteAccountLinks(getApplication())) {
      String endpoint = SupabaseHttp.enabled(getApplication())
          ? com.example.asm_final.BuildConfig.SUPABASE_URL : BackendClient.endpoint(getApplication());
      profileId = links.remoteId(owner, endpoint);
    } catch (Exception ignored) {
      // Keep the friends page available while the account link is unavailable.
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
            friendsState.setValue(new FriendsState(previous == null ? Collections.emptyList() : previous.items,
                false, true, friendFailureCode(failure, "LOAD_FAILED")));
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
      } catch (Exception failure) {
        main.post(() -> {
          if (!closed && ActiveUser.get(getApplication()) == owner) {
            FriendsState previous = friendsState.getValue();
            friendsState.setValue(new FriendsState(previous == null ? Collections.emptyList() : previous.items,
                false, true, friendFailureCode(failure, "ACTION_FAILED")));
          }
        });
        return;
      }

      // A mutation can succeed even if the follow-up list refresh fails.
      // Keep the confirmed outcome instead of showing a false failure and inviting a duplicate retry.
      FriendsState before = friendsState.getValue();
      List<FriendshipRepository.Item> loaded = before == null ? Collections.emptyList() : before.items;
      try { loaded = friendships.list(owner); } catch (Exception ignored) { }
      List<FriendshipRepository.Item> result = loaded;
      main.post(() -> {
        if (!closed && ActiveUser.get(getApplication()) == owner)
          friendsState.setValue(new FriendsState(result, false, false, successCode));
      });
    });
  }

  @Override
  protected void onCleared() {
    closed = true;
    network.shutdownNow();
  }
}
