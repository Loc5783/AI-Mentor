package com.example.asm_final;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.asm_final.accounts.FreshSchoolInstall;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;

/** Isolated filesystem context: the reset test never clears the test runner's own app data. */
public final class FreshSchoolInstallTest {
  @Test
  public void resetsOldDataOnceAndPreservesNewProgressOnNextLaunch() throws Exception {
    Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
    File root = new File(base.getCacheDir(), "reset-fixture-" + System.nanoTime());
    String prefix = "qa-reset-" + System.nanoTime() + "-";
    Context isolated =
        new ContextWrapper(base) {
          @Override
          public ApplicationInfo getApplicationInfo() {
            ApplicationInfo info = new ApplicationInfo(super.getApplicationInfo());
            info.dataDir = root.getAbsolutePath();
            return info;
          }

          @Override
          public File getFilesDir() {
            return new File(root, "files");
          }

          @Override
          public File getCacheDir() {
            return new File(root, "cache");
          }

          @Override
          public File getDatabasePath(String name) {
            return new File(root, "databases/" + name);
          }

          @Override
          public String[] databaseList() {
            String[] names = new File(root, "databases").list();
            return names == null ? new String[0] : names;
          }

          @Override
          public boolean deleteDatabase(String name) {
            File file = getDatabasePath(name);
            boolean deleted = file.delete();
            new File(file + "-wal").delete();
            return deleted;
          }

          @Override
          public SharedPreferences getSharedPreferences(String name, int mode) {
            return base.getSharedPreferences(prefix + name, mode);
          }
        };
    try {
      touch(isolated.getDatabasePath("AImentor.db"));
      touch(isolated.getDatabasePath("AImentor.db-wal"));
      touch(new File(root, "shared_prefs/auth_session.xml"));
      touch(new File(isolated.getFilesDir(), "old-chat/photo.jpg"));
      touch(new File(isolated.getCacheDir(), "old-cache"));
      isolated.getSharedPreferences("auth_session", 0).edit().putInt("user_id", 42).commit();
      FreshSchoolInstall.prepare(isolated);
      assertEquals(0, isolated.databaseList().length);
      assertEquals(0, isolated.getFilesDir().list().length);
      assertEquals(0, isolated.getCacheDir().list().length);
      assertFalse(isolated.getSharedPreferences("auth_session", 0).contains("user_id"));
      File progress = isolated.getDatabasePath("AIMentorSchool.db");
      touch(progress);
      isolated.getSharedPreferences("auth_session", 0).edit().putInt("user_id", 7).commit();
      FreshSchoolInstall.prepare(isolated);
      assertTrue(progress.exists());
      assertEquals(7, isolated.getSharedPreferences("auth_session", 0).getInt("user_id", 0));
    } finally {
      isolated.getSharedPreferences("auth_session", 0).edit().clear().commit();
      isolated.getSharedPreferences("mentor_school_install", 0).edit().clear().commit();
      delete(root);
    }
  }

  private static void touch(File file) throws Exception {
    assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(1);
    }
  }

  private static void delete(File file) {
    File[] children = file.listFiles();
    if (children != null) for (File child : children) delete(child);
    file.delete();
  }
}
