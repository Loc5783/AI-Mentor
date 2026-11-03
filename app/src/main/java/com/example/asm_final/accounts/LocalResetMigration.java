package com.example.asm_final.accounts;

import java.io.*;
import java.util.*;

/** Startup-only migration. Deletion never touches live paths after quarantine. */
public final class LocalResetMigration {
 public enum State { NOT_STARTED, QUARANTINING, QUARANTINED, DELETING, COMPLETED }
 public interface Journal {
  State state();
  List<String> paths();
  void commit(State state, List<String> paths);
 }
 public interface Observer {
  default void state(State state) {}
  default void beforeMove(String path) {}
  default void moved(String path) {}
 }
 private final File root, quarantine;
 private final Journal journal;
 private final Observer observer;
 public LocalResetMigration(File root, Journal journal, Observer observer) throws IOException {
  this.root=root.getCanonicalFile();
  this.quarantine=new File(this.root,"no_backup/mentor-reset-v1");
  this.journal=journal; this.observer=observer;
 }
 private void transition(State state,List<String> paths) {
  journal.commit(state,paths); observer.state(state);
 }
 public void run() throws IOException {
  if(journal.state()==State.COMPLETED) return;
  if(journal.state()==State.NOT_STARTED) {
   observer.state(State.NOT_STARTED);
   // A stale quarantine has no live data and must not be mistaken for this plan.
   deleteQuarantined(quarantine);
   List<String> paths=new ArrayList<>();
   for(String path:Arrays.asList("databases","files","cache"))
    if(new File(root,path).exists()) paths.add(path);
   File[] preferences=new File(root,"shared_prefs").listFiles();
   if(preferences!=null) for(File file:preferences) {
    String name=file.getName();
    if(!name.startsWith("mentor_school_install.") && !name.startsWith("mentor_school_reset_journal."))
     paths.add("shared_prefs/"+name);
   }
   transition(State.QUARANTINING,paths);
  }
  List<String> paths=journal.paths();
  if(journal.state()==State.QUARANTINING) {
   if(!quarantine.getCanonicalFile().equals(quarantine.getAbsoluteFile())) throw new IOException("Invalid quarantine path");
   if(!quarantine.isDirectory() && !quarantine.mkdirs()) throw new IOException("Cannot create quarantine");
   for(int i=0;i<paths.size();i++) {
    String path=paths.get(i);
    boolean preference=path.startsWith("shared_prefs/") && path.substring(13).indexOf('/')<0
      && path.indexOf('\\')<0 && path.length()>13;
    if(!(path.equals("databases") || path.equals("files") || path.equals("cache") || preference)
       || path.contains("..")) throw new IOException("Invalid reset path");
    File source=new File(root,path);
    File destination=new File(quarantine,Integer.toString(i));
    // Rename may have succeeded immediately before process death: never move the new source.
    if(destination.exists() || !source.exists()) continue;
    if(!source.getCanonicalFile().equals(source.getAbsoluteFile()))
     throw new IOException("Reset does not follow links");
    observer.beforeMove(path);
    if(!source.renameTo(destination)) throw new IOException("Cannot quarantine old data");
    observer.moved(path);
   }
   transition(State.QUARANTINED,paths);
  }
  if(journal.state()==State.QUARANTINED) transition(State.DELETING,paths);
  if(journal.state()==State.DELETING) {
   deleteQuarantined(quarantine);
   transition(State.COMPLETED,paths);
  }
 }
 private void deleteQuarantined(File file) throws IOException {
  if(!file.exists()) return;
  if(!file.getCanonicalFile().equals(file.getAbsoluteFile()))
   throw new IOException("Quarantine does not follow links");
  if(file.isDirectory()) {
   File[] children=file.listFiles();
   if(children==null) throw new IOException("Cannot inspect quarantine");
   for(File child:children) deleteQuarantined(child);
  }
  if(!file.delete()) throw new IOException("Cannot remove quarantined data");
 }
}
