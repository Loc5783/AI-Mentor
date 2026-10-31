package com.example.asm_final.dashboard;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.asm_final.accounts.RemoteAccountLinks;
import com.example.asm_final.backend.*;
import com.example.mentor.data.ActiveUser;

/** Coalesces server invalidations; screens retain responsibility for rendering typed summaries. */
public final class LiveOverviewUpdates implements OverviewUpdates {
  public interface SourceFactory {
    EventStream create(int owner);
  }

  public interface EventPolicy {
    boolean affects(String type, String id, String owner);
  }

  private final Context context;
  private final SourceFactory factory;
  private final EventPolicy policy;
  private final Handler main = new Handler(Looper.getMainLooper());

  public LiveOverviewUpdates(Context context, SourceFactory factory) {
    this(context, factory, OverviewEventPolicy::affectsSummary);
  }

  public LiveOverviewUpdates(Context context, SourceFactory factory, EventPolicy policy) {
    this.context = context.getApplicationContext();
    this.factory = factory;
    this.policy = policy;
  }

  @Override
  public Subscription observe(int owner, Runnable refresh) {
    Watch watch = new Watch(owner, refresh);
    watch.worker.start();
    return watch;
  }

  private final class Watch implements Subscription, EventStream.Listener {
    final int owner;
    final Runnable refresh;
    final Thread worker;
    volatile boolean closed;
    volatile EventStream source;
    String remoteId;
    boolean queued;
    final Runnable deliver;

    Watch(int owner, Runnable refresh) {
      this.owner = owner;
      this.refresh = refresh;
      deliver =
          () -> {
            synchronized (this) {
              queued = false;
            }
            if (!closed && ActiveUser.get(context) == owner) this.refresh.run();
          };
      worker = new Thread(this::run, "mentor-overview-events");
    }

    void run() {
      try {
        String endpoint = com.example.asm_final.supabase.SupabaseHttp.enabled(context)
            ? com.example.asm_final.BuildConfig.SUPABASE_URL
            : BackendClient.endpoint(context);
        if (endpoint.isEmpty() || owner <= 0) return;
        try (RemoteAccountLinks links = new RemoteAccountLinks(context)) {
          remoteId = links.remoteId(owner, endpoint);
        }
        if (remoteId.isEmpty() || closed || ActiveUser.get(context) != owner) return;
        EventStream opened = factory.create(owner);
        source = opened;
        int delay = 1000;
        while (!closed && ActiveUser.get(context) == owner) {
          long started = android.os.SystemClock.elapsedRealtime();
          try {
            opened.run(this);
          } catch (BackendClient.Failure failure) {
            if (failure.status == 401 || failure.status == 403) return;
          } catch (Exception offline) {
            // An invalidation is read-only: reconnect safely, without retrying a mutation.
          }
          if (closed) break;
          if (android.os.SystemClock.elapsedRealtime() - started > 30000) delay = 1000;
          Thread.sleep(delay);
          delay = Math.min(30000, delay * 2);
        }
      } catch (InterruptedException stopped) {
        Thread.currentThread().interrupt();
      } catch (RuntimeException unavailable) {
        // Local/cache mode still works when a session or storage cannot be opened.
      } finally {
        EventStream current = source;
        if (current != null) current.close();
      }
    }

    @Override
    public void ready() {
      invalidate();
    }

    @Override
    public void changed(String type, String id) {
      if (policy.affects(type, id, remoteId)) invalidate();
    }

    synchronized void invalidate() {
      if (closed || queued) return;
      queued = true;
      main.postDelayed(deliver, 500);
    }

    @Override
    public synchronized void close() {
      closed = true;
      queued = false;
      main.removeCallbacks(deliver);
      EventStream current = source;
      if (current != null) current.close();
      worker.interrupt();
    }
  }
}
