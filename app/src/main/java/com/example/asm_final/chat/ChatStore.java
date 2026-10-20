package com.example.asm_final.chat;

import com.example.mentor.chat.*;
import com.example.mentor.learning.LearningContext;
import java.util.*;

public interface ChatStore {
  ChatRecords.Thread create(int owner, String folderId);

  ChatRecords.Thread thread(int owner, String id);

  ChatRecords.Snapshot snapshot(int owner, String id);

  List<ChatRecords.Thread> threads(int owner, String query);

  List<ChatRecords.Folder> folders(int owner);

  String createFolder(int owner, String title);

  void renameFolder(int owner, String id, String title);

  void deleteFolder(int owner, String id, boolean deleteChats);

  void renameThread(int owner, String id, String title);

  void moveThread(int owner, String id, String folderId);

  void deleteThread(int owner, String id);

  void saveDraft(int owner, String id, String text);

  void addDraftFile(int owner, String threadId, ChatRecords.FileItem file);

  void removeDraftFile(int owner, String threadId, String fileId);

  String stageMessage(int owner, String threadId, String text);

  TutorRequest request(int owner, String threadId, String messageId, LearningContext context);

  void markPending(int owner, String threadId, String messageId);

  boolean finish(int owner, String threadId, String messageId, TutorReply reply, String error);

  void recoverInterrupted(int owner);
}
