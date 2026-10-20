package com.example.asm_final.backend;

import java.net.URI;

/** Endpoint policy is shared by account, event-stream and lesson transports. */
public final class BackendEndpointPolicy {
  private BackendEndpointPolicy() {}

  public static boolean allowed(String value, boolean debug) {
    try {
      URI uri = URI.create(value);
      boolean local =
          debug
              && "http".equals(uri.getScheme())
              && ("127.0.0.1".equals(uri.getHost()) || "10.0.2.2".equals(uri.getHost()))
              && uri.getPort() >= 1
              && uri.getPort() <= 65535;
      return ("https".equals(uri.getScheme()) || local)
          && uri.getHost() != null
          && uri.getUserInfo() == null
          && uri.getQuery() == null
          && uri.getFragment() == null
          && (uri.getPort() == -1 || uri.getPort() >= 1 && uri.getPort() <= 65535);
    } catch (RuntimeException invalid) {
      return false;
    }
  }
}
