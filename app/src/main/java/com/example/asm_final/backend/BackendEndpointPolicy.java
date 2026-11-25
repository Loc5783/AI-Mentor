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
              && ("127.0.0.1".equals(uri.getHost()) || "10.0.2.2".equals(uri.getHost()) || "localhost".equals(uri.getHost()) || isPrivateIpv4(uri.getHost()))
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

  private static boolean isPrivateIpv4(String host) {
    if (host == null) return false;
    String[] parts = host.split("\\.");
    if (parts.length != 4) return false;
    try {
      int p0 = Integer.parseInt(parts[0]);
      int p1 = Integer.parseInt(parts[1]);
      int p2 = Integer.parseInt(parts[2]);
      int p3 = Integer.parseInt(parts[3]);
      if (p0 < 0 || p0 > 255 || p1 < 0 || p1 > 255 || p2 < 0 || p2 > 255 || p3 < 0 || p3 > 255) return false;
      return p0 == 10 || (p0 == 172 && p1 >= 16 && p1 <= 31) || (p0 == 192 && p1 == 168);
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
