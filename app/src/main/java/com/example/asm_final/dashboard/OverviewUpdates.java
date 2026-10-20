package com.example.asm_final.dashboard;

/** UI dependency: observe invalidations without knowing SSE, credentials or reconnect policy. */
public interface OverviewUpdates {
  interface Subscription extends AutoCloseable {
    @Override
    void close();
  }

  Subscription observe(int owner, Runnable refresh);
}
