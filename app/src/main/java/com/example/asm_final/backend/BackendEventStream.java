package com.example.asm_final.backend;

import android.content.Context;
import com.example.mentor.data.ActiveUser;
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** One account-scoped SSE transport; reconnect policy belongs to its subscriber. */
public final class BackendEventStream implements EventStream {
  private final Context context;
  private final int owner;
  private final BackendClient client;
  private volatile boolean closed;
  private volatile HttpURLConnection connection;
  private String cursor;

  public BackendEventStream(Context context, int owner) {
    this.context = context.getApplicationContext();
    this.owner = owner;
    client = new BackendClient(context, owner);
  }

  private void check() throws IOException {
    if (closed || ActiveUser.get(context) != owner) throw new IOException("Stream closed");
  }

  @Override
  public void run(Listener listener) throws Exception {
    check();
    if (cursor == null) {
      cursor = client.request("GET", "/sync/cursor", null, null, null).getString("cursor");
      if (!cursor.matches("[0-9]{1,19}")) throw new IOException("Invalid cursor");
    }
    HttpURLConnection opened = client.events(cursor);
    connection = opened;
    try {
      check();
      int status = opened.getResponseCode();
      if (status != 200) throw new BackendClient.Failure("EVENT_STREAM_REJECTED", status);
      String type = opened.getContentType();
      if (type == null || !type.startsWith("text/event-stream"))
        throw new IOException("Invalid stream");
      check();
      // Refresh after capturing the cursor: changes between the initial read and subscription
      // cannot be lost, including reconnects after a previously failed overview request.
      listener.ready();
      try (Reader reader = new InputStreamReader(opened.getInputStream(), StandardCharsets.UTF_8)) {
        SseReader.read(
            reader,
            (id, event, data) -> {
              check();
              if (!id.matches("[0-9]{1,19}")) throw new IOException("Invalid event ID");
              long sequence = Long.parseLong(id);
              if (sequence <= Long.parseLong(cursor)) return;
              if (event.equals("change")) {
                JSONObject value = new JSONObject(data);
                if (!id.equals(value.getString("eventId")))
                  throw new IOException("Mismatched event ID");
                listener.changed(value.getString("resourceType"), value.getString("resourceId"));
              }
              cursor = id;
            });
      }
    } finally {
      opened.disconnect();
      if (connection == opened) connection = null;
    }
  }

  @Override
  public void close() {
    closed = true;
    HttpURLConnection current = connection;
    if (current != null) current.disconnect();
  }
}
