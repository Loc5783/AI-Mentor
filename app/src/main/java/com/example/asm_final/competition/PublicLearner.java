package com.example.asm_final.competition;

/** Public fields only. Private account identifiers and credentials are not part of this model. */
public final class PublicLearner {
  public final String id, nickname, bio, learningDay;
  public final long dailyXp;
  public final int streakDays;
  public final boolean hasAvatar;

  public PublicLearner(
      String id, String nickname, String bio, String day, long xp, int streak, boolean avatar) {
    if (id == null
        || !id.matches("[1-9][0-9]{0,18}")
        || nickname == null
        || nickname.trim().isEmpty()
        || nickname.length() > 100
        || bio == null
        || bio.length() > 2000
        || day == null
        || !day.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
        || xp < 0
        || streak < 0) throw new IllegalArgumentException("Invalid public learner");
    this.id = id;
    this.nickname = nickname;
    this.bio = bio;
    learningDay = day;
    dailyXp = xp;
    streakDays = streak;
    hasAvatar = avatar;
  }
}
