package com.example.asm_final.competition;

import static org.junit.Assert.*;

import android.app.Application;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;

public class FriendsViewModelTest {
  @Rule public InstantTaskExecutorRule liveData = new InstantTaskExecutorRule();

  private static final class Queue implements Executor {
    final List<Runnable> tasks = new ArrayList<>();
    @Override public void execute(Runnable task) { tasks.add(task); }
    void next() { tasks.remove(0).run(); }
    void all() { while (!tasks.isEmpty()) next(); }
  }

  private static final class Directory implements LearnerDirectory {
    String me = "self";
    @Override public String currentProfileId(int owner) { return me; }
    @Override public List<FriendsViewModel.SearchItem> search(int owner, String query) {
      return Arrays.asList(new FriendsViewModel.SearchItem("self", "Self"),
          new FriendsViewModel.SearchItem(query, query));
    }
  }

  private static final class Friends implements FriendshipRepository {
    boolean failList, failRequest;
    int requests, lists;
    List<Item> items = new ArrayList<>();
    @Override public List<Item> list(int owner) throws Exception {
      lists++;
      if (failList) throw new IOException("offline");
      return items;
    }
    @Override public Item request(int owner, String id, String key) throws Exception {
      requests++;
      if (failRequest) throw new IOException("offline");
      Item item = new Item("friendship", id, "PENDING", "OUTGOING", "Friend", 0, 0, 1);
      items = Arrays.asList(item);
      return item;
    }
    @Override public Item decide(int owner, String id, String decision, int revision) { throw new AssertionError(); }
    @Override public void remove(int owner, String id, int revision) { throw new AssertionError(); }
  }

  private static FriendsViewModel model(Friends friends, Directory directory, AtomicInteger user,
      Queue worker, Queue main) {
    return new FriendsViewModel(new Application(), friends, directory, user::get, worker, main);
  }

  @Test public void searchExcludesCurrentAccountAndRejectsUnknownIdentity() {
    Friends friends = new Friends(); Directory directory = new Directory();
    Queue worker = new Queue(), main = new Queue(); AtomicInteger user = new AtomicInteger(1);
    FriendsViewModel model = model(friends, directory, user, worker, main);
    model.searchLearners("other"); worker.all(); main.all();
    assertEquals(1, model.searchState().getValue().items.size());
    assertEquals("other", model.searchState().getValue().items.get(0).id);
    directory.me = "";
    model.searchLearners("self"); worker.all(); main.all();
    assertTrue(model.searchState().getValue().failed);
    assertTrue(model.searchState().getValue().items.isEmpty());
  }

  @Test public void confirmedRequestSurvivesFailedListAndSubsequentRefresh() {
    Friends friends = new Friends(); friends.failList = true;
    Queue worker = new Queue(), main = new Queue();
    FriendsViewModel model = model(friends, new Directory(), new AtomicInteger(1), worker, main);
    model.sendFriendRequest("other");
    model.refreshFriends(); // Realtime invalidation during the mutation is coalesced.
    worker.all(); main.all(); worker.all(); main.all();
    assertEquals(1, friends.requests);
    assertFalse(model.friendsState().getValue().failed);
    assertEquals("REQUEST_SENT", model.friendsState().getValue().message);
    friends.failList = false;
    model.refreshFriends(); worker.all(); main.all();
    assertEquals("REQUEST_SENT", model.friendsState().getValue().message);
    assertEquals(1, model.friendsState().getValue().items.size());
  }

  @Test public void olderRefreshCannotEraseMutationConfirmation() {
    Friends friends = new Friends(); Queue worker = new Queue(), main = new Queue();
    FriendsViewModel model = model(friends, new Directory(), new AtomicInteger(1), worker, main);
    model.refreshFriends(); worker.next(); // Its callback is waiting on the main thread.
    model.sendFriendRequest("other");
    main.next(); worker.all(); main.all();
    assertEquals("REQUEST_SENT", model.friendsState().getValue().message);
    assertEquals(1, model.friendsState().getValue().items.size());
  }

  @Test public void queuedRefreshCannotEraseMutationFailure() {
    Friends friends = new Friends(); friends.failRequest = true;
    Queue worker = new Queue(), main = new Queue();
    FriendsViewModel model = model(friends, new Directory(), new AtomicInteger(1), worker, main);
    model.sendFriendRequest("other"); model.refreshFriends();
    worker.all(); main.all();
    assertTrue(model.friendsState().getValue().failed);
    assertEquals("ACTION_OFFLINE", model.friendsState().getValue().message);
    assertTrue(worker.tasks.isEmpty());
  }

  @Test public void lateSearchAndFriendResultsNeverCrossAccounts() {
    Friends friends = new Friends(); Queue worker = new Queue(), main = new Queue();
    AtomicInteger user = new AtomicInteger(1);
    FriendsViewModel model = model(friends, new Directory(), user, worker, main);
    model.searchLearners("first"); model.searchLearners("second");
    worker.all(); main.all();
    assertEquals("second", model.searchState().getValue().items.get(0).id);
    model.refreshFriends(); worker.all();
    user.set(2); main.all();
    assertTrue(model.friendsState().getValue().items.isEmpty());
    model.searchLearners("third");
    assertTrue(worker.tasks.isEmpty());
  }

  @Test public void clearedModelIgnoresQueuedCallbacks() {
    Friends friends = new Friends(); Queue worker = new Queue(), main = new Queue();
    FriendsViewModel model = model(friends, new Directory(), new AtomicInteger(1), worker, main);
    model.searchLearners("other"); worker.all();
    model.onCleared(); main.all();
    assertTrue(model.searchState().getValue().loading);
    assertTrue(model.searchState().getValue().items.isEmpty());
  }
}
