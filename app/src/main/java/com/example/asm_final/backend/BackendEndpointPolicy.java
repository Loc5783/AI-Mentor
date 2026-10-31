package com.example.asm_final.backend;

import java.net.URI;

/** Endpoint policy is shared by account, event-stream and lesson transports. */
public final class BackendEndpointPolicy {
  private BackendEndpointPolicy() {}

  private static boolean isLocalOrLanHost(String host) {
    if (host == null) return false;
    if ("127.0.0.1".equals(host) || "localhost".equals(host) || "10.0.2.2".equals(host)) {
      return true;
    }
    return host.matches("^192\\.168\\.\\d{1,3}\\.\\d{1,3}$")
        || host.matches("^10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
        || host.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\.\\d{1,3}\\.\\d{1,3}$");
  }

  public static boolean allowed(String value, boolean debug) {
    try {
      if (value == null || value.trim().isEmpty()) return false;
      URI uri = URI.create(value);
      String host = uri.getHost();
      boolean local =
          debug
              && "http".equals(uri.getScheme())
              && isLocalOrLanHost(host)
              && uri.getPort() >= 1
              && uri.getPort() <= 65535;
      return ("https".equals(uri.getScheme()) || local)
          && host != null
          && uri.getUserInfo() == null
          && uri.getQuery() == null
          && uri.getFragment() == null
          && (uri.getPort() == -1 || (uri.getPort() >= 1 && uri.getPort() <= 65535));
    } catch (RuntimeException invalid) {
      return false;
    }
  }
}
