package com.example.mentor.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

/** Fresh shared database with versioned schemas; never imports old subject databases. */
public abstract class ModuleDatabaseHelper implements AutoCloseable {
  public static final String DATABASE_NAME = "AIMentorSchool.db";
  private static final Object SCHEMA_LOCK = new Object();
  private final Context context;
  private final String schema;
  private final int version;
  private SQLiteDatabase database;
  private SchoolDatabaseConnections.Lease lease;

  protected ModuleDatabaseHelper(
      Context context, String schema, SQLiteDatabase.CursorFactory ignored, int version) {
    this.context = context.getApplicationContext();
    this.schema = schema;
    this.version = version;
  }

  public final synchronized SQLiteDatabase getWritableDatabase() {
    if (database != null && database.isOpen()) return database;
    synchronized (SCHEMA_LOCK) {
      SchoolDatabaseConnections.Lease acquired = SchoolDatabaseConnections.acquire(context);
      SQLiteDatabase db = acquired.database;
      try {
        Integer known = acquired.schemas.get(schema);
        if (known != null && known == version) {
          database = db;
          lease = acquired;
          return db;
        }
        db.beginTransaction();
        try {
          db.execSQL(
              "CREATE TABLE IF NOT EXISTS mentor_schema_versions(module TEXT PRIMARY KEY,version"
                  + " INTEGER NOT NULL)");
          int current = 0;
          try (Cursor c =
              db.rawQuery(
                  "SELECT version FROM mentor_schema_versions WHERE module=?",
                  new String[] {schema})) {
            if (c.moveToFirst()) current = c.getInt(0);
          }
          if (current > version) onDowngrade(db, current, version);
          else if (current < version) {
            if (current == 0) onCreate(db);
            else onUpgrade(db, current, version);
            db.execSQL(
                "INSERT OR REPLACE INTO mentor_schema_versions(module,version) VALUES(?,?)",
                new Object[] {schema, version});
          }
          db.setTransactionSuccessful();
        } finally {
          db.endTransaction();
        }
        acquired.schemas.put(schema, version);
        onOpen(db);
        lease = acquired;
        database = db;
        return db;
      } catch (RuntimeException failure) {
        SchoolDatabaseConnections.release(acquired);
        throw failure;
      }
    }
  }

  public final SQLiteDatabase getReadableDatabase() {
    return getWritableDatabase();
  }

  public final String getDatabaseName() {
    return DATABASE_NAME;
  }

  public abstract void onCreate(SQLiteDatabase db);

  public abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

  public void onOpen(SQLiteDatabase db) {}

  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    throw new SQLiteException("Unsupported schema downgrade");
  }

  @Override
  public synchronized void close() {
    synchronized (SCHEMA_LOCK) {
      if (lease != null) {
        SchoolDatabaseConnections.release(lease);
        lease = null;
      }
      database = null;
    }
  }
}
