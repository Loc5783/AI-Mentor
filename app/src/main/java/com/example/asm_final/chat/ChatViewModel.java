package com.example.asm_final.chat;

import android.app.Application;
import android.net.Uri;
import android.os.*;
import androidx.lifecycle.*;
import com.example.asm_final.*;
import com.example.asm_final.R;
import com.example.mentor.chat.*;
import com.example.mentor.learning.LearningContext;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Activity-scoped state. SQLite is serialized independently from the cancellable AI request. */
public final class ChatViewModel extends AndroidViewModel implements AutoCloseable {
  private final AppServices services;
  private final ChatStore store;
  private final TutorGateway gateway;
  private final LocalChatFiles files;
  private final ExecutorService database = Executors.newSingleThreadExecutor();
  private final ExecutorService network = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
  private volatile boolean closed;
  private int owner, version;
  private long selectionVersion, draftVersion;
  private String threadId, jobToken, busyThreadId, busyMessageId;
  private Future<?> call;
  private Runnable pendingSave;
  private PendingSave unsavedReply;
  public ChatRecords.Snapshot snapshot;
  public List<ChatRecords.Thread> threads = Collections.emptyList();
  public List<ChatRecords.Folder> folders = Collections.emptyList();
  public String draft = "";
  public int grade, status;
  public boolean loading, importing, requesting;

  public ChatViewModel(Application app) {
    this(
        app,
        ((AiMentorApplication) app).services().chats(),
        ((AiMentorApplication) app).services().tutor());
  }

  public ChatViewModel(Application app, ChatStore store, TutorGateway gateway) {
    super(app);
    this.services = ((AiMentorApplication) app).services();
    this.store = store;
    this.gateway = gateway;
    this.files = new LocalChatFiles(app);
  }

  public LiveData<Integer> changes() {
    return changes;
  }

  public int owner() {
    return owner;
  }

  public String threadId() {
    return threadId;
  }

  public boolean busyHere() {
    return requesting && Objects.equals(threadId, busyThreadId);
  }

  public boolean hasUnsavedReply(String id) {
    return unsavedReply != null && unsavedReply.message.equals(id);
  }

  private void emit() {
    if (!closed) changes.setValue(++version);
  }

  private <T> void work(Callable<T> task, Consumer<T> success, Consumer<Exception> failure) {
    if (closed) return;
    database.execute(
        () -> {
          try {
            T value = task.call();
            main.post(
                () -> {
                  if (!closed) success.accept(value);
                });
          } catch (Exception error) {
            main.post(
                () -> {
                  if (!closed) failure.accept(error);
                });
          }
        });
  }

  private void fail(Exception error) {
    loading = false;
    importing = false;
    status = error(error);
    emit();
  }

  public static int error(Exception error) {
    if (error instanceof LocalChatFiles.FileFailure)
      return ((LocalChatFiles.FileFailure) error).tooLarge
          ? R.string.chat_file_large
          : R.string.chat_file_invalid;
    if (error instanceof TutorFailure) {
      switch (((TutorFailure) error).code) {
        case NOT_CONFIGURED:
        case INVALID_CONFIGURATION:
          return R.string.chat_service_unavailable;
        case NETWORK:
          return R.string.chat_offline;
        case RATE_LIMITED:
          return R.string.chat_rate_limit;
        case UNAUTHORIZED:
          return R.string.chat_service_unavailable;
        case CANCELLED:
          return R.string.chat_cancelled;
        case EMPTY_RESPONSE:
          return R.string.chat_empty_response;
        case INVALID_REQUEST:
          return R.string.chat_context_full;
        default:
          return R.string.chat_provider_error;
      }
    }
    return error instanceof IllegalArgumentException
        ? R.string.chat_context_full
        : R.string.chat_storage_error;
  }

  private static final class Loaded {
    final ChatRecords.Snapshot snapshot;
    final List<ChatRecords.Thread> threads;
    final List<ChatRecords.Folder> folders;
    final int grade;

    Loaded(ChatRecords.Snapshot s, List<ChatRecords.Thread> t, List<ChatRecords.Folder> f, int g) {
      snapshot = s;
      threads = t;
      folders = f;
      grade = g;
    }
  }

  private Loaded read(String id) {
    com.example.asm_final.onboarding.LearnerProfile profile = services.learners().load(owner);
    return new Loaded(
        store.snapshot(owner, id),
        store.threads(owner, ""),
        store.folders(owner),
        profile == null ? 0 : profile.grade);
  }

  private void accept(Loaded data, long draftAtStart) {
    snapshot = data.snapshot;
    threads = data.threads;
    folders = data.folders;
    grade = data.grade;
    if (draftVersion == draftAtStart) draft = snapshot.thread.draft;
    loading = false;
    emit();
  }

  public void bind(int userId) {
    if (owner != 0) {
      if (owner != userId) throw new IllegalStateException("Chat model cannot switch accounts");
      return;
    }
    if (userId <= 0) throw new IllegalArgumentException("Missing chat account");
    owner = userId;
    loading = true;
    emit();
    work(
        () -> {
          store.recoverInterrupted(owner);
          String saved =
              getApplication()
                  .getSharedPreferences("general_chat_state", 0)
                  .getString("active_" + owner, "");
          ChatRecords.Thread current = null;
          if (!saved.isEmpty())
            try {
              current = store.thread(owner, saved);
            } catch (IllegalArgumentException deleted) {
            }
          if (current == null) current = store.create(owner, null);
          return read(current.id);
        },
        loaded -> {
          threadId = loaded.snapshot.thread.id;
          remember();
          accept(loaded, draftVersion);
        },
        this::fail);
  }

  private void remember() {
    getApplication()
        .getSharedPreferences("general_chat_state", 0)
        .edit()
        .putString("active_" + owner, threadId)
        .apply();
  }

  public void refresh() {
    if (threadId == null || closed) return;
    String target = threadId;
    long selected = ++selectionVersion, draftAtStart = draftVersion;
    work(
        () -> read(target),
        data -> {
          if (selected == selectionVersion && target.equals(threadId)) accept(data, draftAtStart);
        },
        this::fail);
  }

  public void setDraft(String text) {
    if (closed || loading || Objects.equals(draft, text)) return;
    draft = text;
    draftVersion++;
    if (pendingSave != null) main.removeCallbacks(pendingSave);
    final String target = threadId, content = text;
    pendingSave = () -> save(target, content);
    main.postDelayed(pendingSave, 300);
  }

  private void save(String target, String text) {
    if (target == null || closed) return;
    work(
        () -> {
          store.saveDraft(owner, target, text);
          return true;
        },
        ignored -> {},
        this::fail);
  }

  public void flush() {
    if (pendingSave != null) main.removeCallbacks(pendingSave);
    pendingSave = null;
    if (requesting && busyMessageId == null && Objects.equals(threadId, busyThreadId)) return;
    save(threadId, draft);
  }

  public void select(String id) {
    if (importing || loading || id.equals(threadId)) return;
    flush();
    threadId = id;
    draft = "";
    draftVersion++;
    snapshot = null;
    status = 0;
    loading = true;
    remember();
    emit();
    refresh();
  }

  public void newChat() {
    if (loading || importing) return;
    if (snapshot != null
        && snapshot.messages.isEmpty()
        && draft.isEmpty()
        && snapshot.draftFiles.isEmpty()) return;
    flush();
    loading = true;
    emit();
    work(
        () -> store.create(owner, null),
        thread -> {
          threadId = thread.id;
          draft = "";
          draftVersion++;
          snapshot = null;
          status = 0;
          remember();
          refresh();
        },
        this::fail);
  }

  public void setGrade(int chosen) {
    work(
        () -> {
          services.learners().setGrade(owner, chosen);
          return chosen;
        },
        value -> {
          grade = value;
          status = 0;
          emit();
        },
        this::fail);
  }

  public void createFolder(String title) {
    work(
        () -> store.createFolder(owner, title),
        ignored -> {
          status = 0;
          refresh();
        },
        this::fail);
  }

  public void renameFolder(String id, String title) {
    work(
        () -> {
          store.renameFolder(owner, id, title);
          return true;
        },
        ignored -> refresh(),
        this::fail);
  }

  public void renameChat(String id, String title) {
    work(
        () -> {
          store.renameThread(owner, id, title);
          return true;
        },
        ignored -> refresh(),
        this::fail);
  }

  public void moveChat(String id, String folderId) {
    work(
        () -> {
          store.moveThread(owner, id, folderId);
          return true;
        },
        ignored -> refresh(),
        this::fail);
  }

  public void deleteChat(String id) {
    if (unsavedReply != null && unsavedReply.thread.equals(id)) unsavedReply = null;
    if (Objects.equals(id, busyThreadId) && requesting) cancel();
    flush();
    boolean current = id.equals(threadId);
    loading = true;
    emit();
    work(
        () -> {
          store.deleteThread(owner, id);
          return current ? store.create(owner, null) : null;
        },
        created -> {
          if (current) {
            threadId = created.id;
            draft = "";
            draftVersion++;
            snapshot = null;
            remember();
          }
          refresh();
        },
        this::fail);
  }

  public void deleteFolder(String id, boolean deleteChats) {
    if (deleteChats && unsavedReply != null)
      for (ChatRecords.Thread t : threads)
        if (t.id.equals(unsavedReply.thread) && Objects.equals(t.folderId, id)) {
          unsavedReply = null;
          break;
        }
    if (deleteChats && requesting)
      for (ChatRecords.Thread t : threads)
        if (t.id.equals(busyThreadId) && Objects.equals(t.folderId, id)) cancel();
    boolean affectsCurrent =
        deleteChats && snapshot != null && Objects.equals(snapshot.thread.folderId, id);
    flush();
    loading = true;
    emit();
    work(
        () -> {
          store.deleteFolder(owner, id, deleteChats);
          return affectsCurrent ? store.create(owner, null) : null;
        },
        created -> {
          if (created != null) {
            threadId = created.id;
            draft = "";
            draftVersion++;
            snapshot = null;
            remember();
          }
          refresh();
        },
        this::fail);
  }

  public void removeFile(String id) {
    if (threadId == null || importing || loading) return;
    String target = threadId;
    work(
        () -> {
          store.removeDraftFile(owner, target, id);
          return true;
        },
        ignored -> refresh(),
        this::fail);
  }

  public void importUris(List<Uri> uris) {
    if (uris == null || uris.isEmpty() || snapshot == null || importing || loading) return;
    if (snapshot.draftFiles.size() + uris.size() > 4) {
      status = R.string.chat_file_count;
      emit();
      return;
    }
    final String target = threadId;
    importing = true;
    status = 0;
    emit();
    work(
        () -> {
          int rejected = 0;
          for (Uri uri : uris) {
            ChatRecords.FileItem file = null;
            try {
              file = files.importUri(owner, uri);
              store.addDraftFile(owner, target, file);
            } catch (RuntimeException error) {
              if (file != null) files.delete(owner, file);
              rejected++;
            }
          }
          return rejected;
        },
        rejected -> {
          importing = false;
          if (rejected > 0) status = R.string.chat_some_files_invalid;
          refresh();
        },
        this::fail);
  }

  public void importCamera(File source, String capturedThread) {
    if (snapshot == null || !Objects.equals(capturedThread, threadId) || importing || loading) {
      source.delete();
      return;
    }
    if (snapshot.draftFiles.size() >= 4) {
      source.delete();
      status = R.string.chat_file_count;
      emit();
      return;
    }
    final String target = threadId;
    importing = true;
    status = 0;
    emit();
    work(
        () -> {
          ChatRecords.FileItem file = null;
          try {
            file = files.importCamera(owner, source);
            store.addDraftFile(owner, target, file);
            return true;
          } catch (RuntimeException error) {
            if (file != null) files.delete(owner, file);
            throw error;
          } finally {
            source.delete();
          }
        },
        ignored -> {
          importing = false;
          refresh();
        },
        this::fail);
  }

  public void send() {
    start(null);
  }

  public void retry(String messageId) {
    start(messageId);
  }

  private void start(String retryId) {
    if (loading || importing || requesting || snapshot == null) return;
    if (unsavedReply != null) {
      if (unsavedReply.message.equals(retryId)) {
        requesting = true;
        status = 0;
        emit();
        persistReply(unsavedReply);
      } else {
        status = R.string.chat_storage_error;
        emit();
      }
      return;
    }
    if (grade < 10 || grade > 12) {
      status = R.string.chat_grade_required;
      emit();
      return;
    }
    if (!gateway.configured()) {
      flush();
      status = R.string.chat_service_unavailable;
      emit();
      return;
    }
    if (retryId == null && draft.trim().isEmpty() && snapshot.draftFiles.isEmpty()) return;
    flush();
    status = 0;
    requesting = true;
    loading = true;
    String target = threadId, text = draft, token = UUID.randomUUID().toString();
    busyThreadId = target;
    busyMessageId = null;
    jobToken = token;
    emit();
    work(
        () -> {
          String message = retryId;
          if (message == null) message = store.stageMessage(owner, target, text);
          else store.markPending(owner, target, message);
          try {
            com.example.asm_final.onboarding.LearnerProfile profile =
                services.learners().load(owner);
            TutorRequest request =
                store.request(
                    owner,
                    target,
                    message,
                    new LearningContext(
                        owner,
                        profile == null ? 0 : profile.grade,
                        com.example.asm_final.utils.LocaleHelper.getLanguage(getApplication())));
            return new Prepared(message, request);
          } catch (Exception error) {
            store.finish(owner, target, message, null, "INVALID_REQUEST");
            throw error;
          }
        },
        prepared -> {
          if (!token.equals(jobToken)) {
            work(
                () -> store.finish(owner, target, prepared.id, null, "CANCELLED"),
                ignored -> refresh(),
                this::fail);
            return;
          }
          busyMessageId = prepared.id;
          if (retryId == null) {
            draft = "";
            draftVersion++;
          }
          loading = false;
          refresh();
          call =
              network.submit(
                  () -> {
                    TutorReply reply = null;
                    Exception failure = null;
                    try {
                      reply = gateway.reply(prepared.request);
                    } catch (Exception error) {
                      failure = error;
                    }
                    TutorReply answer = reply;
                    Exception resultError = failure;
                    main.post(
                        () -> {
                          if (closed || !token.equals(jobToken)) return;
                          unsavedReply = new PendingSave(target, prepared.id, answer, resultError);
                          persistReply(unsavedReply);
                        });
                  });
        },
        error -> {
          if (token.equals(jobToken)) {
            requesting = false;
            busyThreadId = null;
            busyMessageId = null;
            jobToken = null;
            loading = false;
            status = error(error);
            refresh();
          }
        });
  }

  private static final class Prepared {
    final String id;
    final TutorRequest request;

    Prepared(String id, TutorRequest request) {
      this.id = id;
      this.request = request;
    }
  }

  private static final class PendingSave {
    final String thread, message;
    final TutorReply reply;
    final Exception error;

    PendingSave(String thread, String message, TutorReply reply, Exception error) {
      this.thread = thread;
      this.message = message;
      this.reply = reply;
      this.error = error;
    }
  }

  private void persistReply(PendingSave result) {
    work(
        () ->
            store.finish(
                owner,
                result.thread,
                result.message,
                result.reply,
                result.error instanceof TutorFailure
                    ? ((TutorFailure) result.error).code.name()
                    : result.error == null ? "" : "PROVIDER"),
        ignored -> {
          if (unsavedReply == result) unsavedReply = null;
          requesting = false;
          busyMessageId = null;
          busyThreadId = null;
          jobToken = null;
          if (result.error != null) status = error(result.error);
          refresh();
        },
        error -> {
          requesting = false;
          loading = false;
          busyMessageId = null;
          busyThreadId = null;
          jobToken = null;
          status = R.string.chat_storage_error;
          refresh();
        });
  }

  public void cancel() {
    if (!requesting) return;
    String target = busyThreadId, message = busyMessageId;
    jobToken = null;
    requesting = false;
    busyThreadId = null;
    busyMessageId = null;
    loading = false;
    if (call != null) call.cancel(true);
    if (message != null)
      work(
          () -> store.finish(owner, target, message, null, "CANCELLED"),
          ignored -> refresh(),
          this::fail);
    status = R.string.chat_cancelled;
    emit();
  }

  @Override
  public void close() {
    if (closed) return;
    flush();
    cancel();
    closed = true;
    if (pendingSave != null) main.removeCallbacks(pendingSave);
    database.shutdown();
    network.shutdownNow();
  }

  @Override
  protected void onCleared() {
    close();
  }
}
