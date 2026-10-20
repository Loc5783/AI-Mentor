package com.example.asm_final.competition;

import android.app.Application;
import android.graphics.*;
import android.os.*;
import androidx.lifecycle.*;
import com.example.asm_final.AiMentorApplication;
import com.example.mentor.data.ActiveUser;
import java.util.concurrent.*;

/** Keeps profile loading and safe image decoding off the UI thread. */
public final class PublicProfileViewModel extends AndroidViewModel {
  public static final class State {
    public final PublicLearner learner;
    public final Bitmap avatar;
    public final boolean loading, failed, avatarFailed;

    State(
        PublicLearner learner,
        Bitmap avatar,
        boolean loading,
        boolean failed,
        boolean avatarFailed) {
      this.learner = learner;
      this.avatar = avatar;
      this.loading = loading;
      this.failed = failed;
      this.avatarFailed = avatarFailed;
    }
  }

  private final PublicProfileRepository repository;
  private final int owner;
  public final String profileId;
  private final ExecutorService network = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final MutableLiveData<State> state =
      new MutableLiveData<>(new State(null, null, true, false, false));
  private volatile int generation;
  private volatile boolean closed;

  public PublicProfileViewModel(Application app, SavedStateHandle saved) {
    super(app);
    owner = ActiveUser.get(app);
    profileId = saved.get("PROFILE_ID");
    repository = ((AiMentorApplication) app).services().publicProfiles();
  }

  public LiveData<State> state() {
    return state;
  }

  public void refresh() {
    if (closed || ActiveUser.get(getApplication()) != owner) return;
    int version = ++generation;
    State old = state.getValue();
    state.setValue(
        new State(
            old == null ? null : old.learner, old == null ? null : old.avatar, true, false, false));
    network.execute(
        () -> {
          if (closed || version != generation) return;
          PublicLearner learner = null;
          Bitmap image = null;
          boolean imageFailed = false;
          try {
            learner = repository.load(owner, profileId);
            State metadata = new State(learner, null, false, false, false);
            main.post(
                () -> {
                  if (!closed && version == generation && ActiveUser.get(getApplication()) == owner)
                    state.setValue(metadata);
                });
            if (learner.hasAvatar) {
              try {
                byte[] bytes = repository.avatar(owner, profileId);
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0)
                  throw new IllegalArgumentException("Invalid image");
                BitmapFactory.Options sampled = new BitmapFactory.Options();
                sampled.inSampleSize = 1;
                while (Math.max(bounds.outWidth, bounds.outHeight) / (long) sampled.inSampleSize
                    > 512) sampled.inSampleSize *= 2;
                image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, sampled);
                imageFailed = image == null;
              } catch (Exception unavailable) {
                imageFailed = true;
              }
            }
          } catch (Exception unavailable) {
            /* Render a retryable error without stale private fields. */
          }
          State result = new State(learner, image, false, learner == null, imageFailed);
          main.post(
              () -> {
                if (!closed && version == generation && ActiveUser.get(getApplication()) == owner)
                  state.setValue(result);
              });
        });
  }

  @Override
  protected void onCleared() {
    closed = true;
    generation++;
    network.shutdownNow();
  }
}
