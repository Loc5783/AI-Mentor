package com.example.asm_final.competition;

import java.util.List;

/** Account-scoped public learner search and identity lookup. */
public interface LearnerDirectory {
  String currentProfileId(int owner) throws Exception;

  List<FriendsViewModel.SearchItem> search(int owner, String query) throws Exception;
}
