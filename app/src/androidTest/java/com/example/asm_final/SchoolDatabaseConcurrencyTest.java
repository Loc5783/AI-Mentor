package com.example.asm_final;

import static org.junit.Assert.*;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.mentor.data.ModuleDatabaseHelper;
import java.io.File;
import java.util.concurrent.*;
import org.junit.Test;

public final class SchoolDatabaseConcurrencyTest {
  private static final class Probe extends ModuleDatabaseHelper {
    Probe(Context context) {
      super(context, "probe", null, 1);
    }

    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE probe(value INTEGER)");
    }

    public void onUpgrade(SQLiteDatabase db, int old, int next) {
      throw new AssertionError();
    }

    int count() {
      try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM probe", null)) {
        cursor.moveToFirst();
        return cursor.getInt(0);
      }
    }
  }

  @Test
  public void openingAndClosingReaderDoesNotBlockWriterOrExposeUncommittedData() throws Exception {
    Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
    File directory = new File(app.getCacheDir(), "database-concurrency-" + System.nanoTime());
    Context isolated =
        new ContextWrapper(app) {
          @Override
          public Context getApplicationContext() {
            return this;
          }

          @Override
          public File getDatabasePath(String name) {
            return new File(directory, name);
          }
        };
    ExecutorService thread = Executors.newSingleThreadExecutor();
    try (Probe writer = new Probe(isolated)) {
      SQLiteDatabase db = writer.getWritableDatabase();
      db.beginTransactionNonExclusive();
      try {
        db.execSQL("INSERT INTO probe(value) VALUES(1)");
        Future<Integer> read =
            thread.submit(
                () -> {
                  try (Probe reader = new Probe(isolated)) {
                    return reader.count();
                  }
                });
        assertEquals(0, (int) read.get(5, TimeUnit.SECONDS));
        assertTrue("Closing a reader must not close the writer", db.isOpen());
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }
      try (Probe reader = new Probe(isolated)) {
        assertEquals(1, reader.count());
      }
    } finally {
      thread.shutdownNow();
      SQLiteDatabase.deleteDatabase(isolated.getDatabasePath(ModuleDatabaseHelper.DATABASE_NAME));
      directory.delete();
    }
  }
}
