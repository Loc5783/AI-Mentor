package com.example.asm_final.accounts;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;
public class LocalResetMigrationTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 static class DiskJournal implements LocalResetMigration.Journal {
  final File file;
  DiskJournal(File file){this.file=file;}
  Properties read(){Properties p=new Properties();if(file.exists())try(FileInputStream in=new FileInputStream(file)){p.load(in);}catch(IOException e){throw new RuntimeException(e);}return p;}
  public LocalResetMigration.State state(){return LocalResetMigration.State.valueOf(read().getProperty("state","NOT_STARTED"));}
  public List<String> paths(){String s=read().getProperty("paths","");return s.isEmpty()?new ArrayList<>():Arrays.asList(s.split(","));}
  public void commit(LocalResetMigration.State state,List<String> paths){
   Properties p=new Properties();p.setProperty("state",state.name());p.setProperty("paths",String.join(",",paths));
   file.getParentFile().mkdirs();
   try(FileOutputStream out=new FileOutputStream(file)){p.store(out,"");out.getFD().sync();}catch(IOException e){throw new RuntimeException(e);}
  }
 }
 static void touch(File f) throws Exception {f.getParentFile().mkdirs();Files.write(f.toPath(),new byte[]{7});}
 @Test public void resumesEveryDurableStateWithNewJournalInstance() throws Exception {
  for(LocalResetMigration.State stop:LocalResetMigration.State.values()){
   File root=temp.newFolder();File log=new File(root,"no_backup/journal");
   touch(new File(root,"shared_prefs/auth.xml"));touch(new File(root,"shared_prefs/mentor_school_install.xml"));touch(new File(root,"databases/old.db"));touch(new File(root,"databases/old.db-wal"));
   touch(new File(root,"databases/old.db-shm"));touch(new File(root,"databases/old.db-journal"));
   try {new LocalResetMigration(root,new DiskJournal(log),new LocalResetMigration.Observer(){
    public void state(LocalResetMigration.State s){if(s==stop)throw new IllegalStateException("process killed");}
   }).run();fail();}catch(IllegalStateException expected){}
   if(stop.ordinal()>=LocalResetMigration.State.QUARANTINED.ordinal())touch(new File(root,"databases/new.db"));
   new LocalResetMigration(root,new DiskJournal(log),new LocalResetMigration.Observer(){}).run();
   assertEquals(LocalResetMigration.State.COMPLETED,new DiskJournal(log).state());
   assertFalse(new File(root,"databases/old.db").exists());
   assertFalse(new File(root,"databases/old.db-wal").exists());
   assertTrue(new File(root,"shared_prefs/mentor_school_install.xml").exists());
   assertFalse(new File(root,"shared_prefs/auth.xml").exists());
   if(stop.ordinal()>=LocalResetMigration.State.QUARANTINED.ordinal()) assertTrue(new File(root,"databases/new.db").exists());
   touch(new File(root,"databases/new.db"));
   new LocalResetMigration(root,new DiskJournal(log),new LocalResetMigration.Observer(){}).run();
   assertTrue(new File(root,"databases/new.db").exists());
  }
 }
 @Test public void crashAfterRenameDoesNotMoveNewLiveData() throws Exception {
  File root=temp.newFolder();File log=new File(root,"no_backup/journal");touch(new File(root,"databases/old.db"));
  try{new LocalResetMigration(root,new DiskJournal(log),new LocalResetMigration.Observer(){
   public void moved(String path){throw new IllegalStateException("process killed");}
  }).run();fail();}catch(IllegalStateException expected){}
  touch(new File(root,"databases/new.db"));
  new LocalResetMigration(root,new DiskJournal(log),new LocalResetMigration.Observer(){}).run();
  assertTrue(new File(root,"databases/new.db").exists());assertFalse(new File(root,"no_backup/mentor-reset-v1").exists());
 }
 @Test public void orphanQuarantineCannotHideOldLiveData() throws Exception {
  File root=temp.newFolder();File log=new File(root,"no_backup/journal");
  touch(new File(root,"no_backup/mentor-reset-v1/0/stale.db"));
  touch(new File(root,"databases/old.db"));
  new LocalResetMigration(root,new DiskJournal(log),new LocalResetMigration.Observer(){}).run();
  assertFalse(new File(root,"databases/old.db").exists());
  assertFalse(new File(root,"no_backup/mentor-reset-v1").exists());
 }
}
