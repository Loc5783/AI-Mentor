package com.example.asm_final.backend;

/** A cancelable source of server invalidations. It never supplies UI state or applies mutations. */
public interface EventStream extends AutoCloseable {
  interface Listener {
    void ready();

    void changed(String type, String resourceId);
  }

  void run(Listener listener) throws Exception;

  @Override
  void close();
}
