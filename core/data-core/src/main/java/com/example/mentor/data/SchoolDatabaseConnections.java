package com.example.mentor.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** One connection pool per school database; short-lived repositories borrow a reference. */
final class SchoolDatabaseConnections {
  static final class Lease {
    final SQLiteDatabase database;
    final String path;
    final Map<String, Integer> schemas = new HashMap<>();
    int references;

    Lease(String path, SQLiteDatabase database) {
      this.path = path;
      this.database = database;
    }
  }

  private static final Map<String, Lease> OPEN = new HashMap<>();

  static synchronized Lease acquire(Context context) {
    File file = context.getDatabasePath(ModuleDatabaseHelper.DATABASE_NAME);
    String path = file.getAbsolutePath();
    Lease lease = OPEN.get(path);
    if (lease == null) {
      File parent = file.getParentFile();
      if (!parent.isDirectory() && !parent.mkdirs())
        throw new SQLiteException("Cannot create database directory");
      SQLiteDatabase db =
          SQLiteDatabase.openDatabase(
              path,
              null,
              SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING);
      try {
        db.setForeignKeyConstraintsEnabled(true);
        lease = new Lease(path, db);
        OPEN.put(path, lease);
      } catch (RuntimeException failure) {
        db.close();
        throw failure;
      }
    }
    lease.references++;
    return lease;
  }

  static synchronized void release(Lease lease) {
    if (--lease.references == 0) {
      OPEN.remove(lease.path);
      lease.database.close();
    }
  }

  private SchoolDatabaseConnections() {}
}
