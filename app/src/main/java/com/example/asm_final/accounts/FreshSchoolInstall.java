package com.example.asm_final.accounts;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Explicit product reset, before services open data. The marker preserves subsequent new progress.
 */
public final class FreshSchoolInstall {
  private static final String MARKER = "mentor_school_install";

  private FreshSchoolInstall() {}

  public static void prepare(Context context) {
    SharedPreferences marker = context.getSharedPreferences(MARKER, Context.MODE_PRIVATE);
    if (marker.getBoolean("initialized", false)) return;
    // Only this application's private data; never another app or host files.
    for (String name : context.databaseList())
      if (context.getDatabasePath(name).exists() && !context.deleteDatabase(name))
        throw new IllegalStateException("Cannot reset old database");
    java.io.File preferences =
        new java.io.File(context.getApplicationInfo().dataDir, "shared_prefs");
    java.io.File[] files = preferences.listFiles();
    if (files != null)
      for (java.io.File file : files) {
        String name = file.getName();
        if (name.endsWith(".xml") && !name.equals(MARKER + ".xml")) {
          if (!context
              .getSharedPreferences(name.substring(0, name.length() - 4), Context.MODE_PRIVATE)
              .edit()
              .clear()
              .commit()) throw new IllegalStateException("Cannot reset old session");
        }
      }
    clearContents(context.getFilesDir());
    clearContents(context.getCacheDir());
    if (!marker.edit().putBoolean("initialized", true).commit())
      throw new IllegalStateException("Cannot persist fresh-install marker");
  }

  private static void clearContents(java.io.File directory) {
    java.io.File[] children = directory.listFiles();
    if (children == null) return;
    for (java.io.File child : children) {
      if (child.isDirectory()) clearContents(child);
      if (!child.delete()) throw new IllegalStateException("Cannot remove old local content");
    }
  }
}
