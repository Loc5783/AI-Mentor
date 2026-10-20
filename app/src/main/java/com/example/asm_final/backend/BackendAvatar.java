package com.example.asm_final.backend;

import java.io.*;
import java.net.HttpURLConnection;

/** Avatars are authenticated, bounded and fetched only from the configured API origin. */
public final class BackendAvatar {
  private BackendAvatar() {}

  public static byte[] load(BackendClient client, String profileId) throws Exception {
    if (!profileId.matches("[1-9][0-9]{0,18}"))
      throw new IllegalArgumentException("Invalid profile ID");
    HttpURLConnection connection =
        client.authenticatedConnection("/profiles/" + profileId + "/avatar", "image/*");
    try {
      int status = connection.getResponseCode();
      if (status != 200) throw new BackendClient.Failure("AVATAR_UNAVAILABLE", status);
      String type = connection.getContentType();
      if (type == null || !type.startsWith("image/")) throw new IOException("Invalid avatar type");
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      try (InputStream input = connection.getInputStream()) {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
          if (result.size() + count > 4 * 1024 * 1024) throw new IOException("Avatar too large");
          result.write(buffer, 0, count);
        }
      }
      return result.toByteArray();
    } finally {
      connection.disconnect();
    }
  }
}
