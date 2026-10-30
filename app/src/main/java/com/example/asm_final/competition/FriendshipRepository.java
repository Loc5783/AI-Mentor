package com.example.asm_final.competition;

import java.util.List;

/** Authenticated friend request operations, independent of the selected backend transport. */
public interface FriendshipRepository {
  final class Item {
    public final String id;
    public final String profileId;
    public final String state;
    public final String direction;
    public final String nickname;
    public final long dailyXp;
    public final int streakDays;
    public final int revision;

    public Item(
        String id,
        String profileId,
        String state,
        String direction,
        String nickname,
        long dailyXp,
        int streakDays,
        int revision) {
      this.id = id;
      this.profileId = profileId;
      this.state = state;
      this.direction = direction;
      this.nickname = nickname;
      this.dailyXp = dailyXp;
      this.streakDays = streakDays;
      this.revision = revision;
    }
  }

  List<Item> list(int owner) throws Exception;

  Item request(int owner, String profileId, String requestKey) throws Exception;

  Item decide(int owner, String friendshipId, String decision, int revision) throws Exception;

  void remove(int owner, String friendshipId, int revision) throws Exception;
}
