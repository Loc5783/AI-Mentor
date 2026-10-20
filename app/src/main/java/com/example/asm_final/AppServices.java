package com.example.asm_final;

import android.content.Context;
import com.example.asm_final.accounts.*;
import com.example.asm_final.dashboard.*;
import java.util.concurrent.*;

/** Application composition root. Screens consume focused contracts rather than database helpers. */
public final class AppServices {
  private final com.example.mentor.learning.SubjectCatalog subjects =
      com.example.asm_final.learning.SubjectModules.create();
  private final com.example.mentor.chat.TutorGateway tutor =
      com.example.mentor.chat.GeminiTutorGateway.configuredClient();
  private final LocalAccountRepository accounts;
  private final AccountAccess access;
  private final LearningOverviewRepository overview;
  private final OverviewUpdates overviewUpdates;
  private final OverviewUpdates rankingUpdates;
  private final com.example.asm_final.competition.CompetitionRepository competition;
  private final com.example.asm_final.competition.PublicProfileRepository publicProfiles;
  private final com.example.asm_final.chat.ChatStore chats;
  private final com.example.asm_final.onboarding.LearnerProfileRepository learners;
  private final RecoveryService recovery = new UnavailableRecoveryService();
  private final DashboardRepository dashboard;
  private final LearningStreakRepository streaks;
  private final WeeklyQuizRepository weeklyQuizzes;
  private final UserProfileRepository profiles;
  private final ExecutorService io = Executors.newFixedThreadPool(2);

  public AppServices(Context context) {
    accounts = new LocalAccountRepository(context);
    chats = new com.example.asm_final.chat.LocalChatStore(context);
    learners = new com.example.asm_final.onboarding.LocalLearnerProfiles(context);
    access = new AccountAccess(context, accounts, learners);
    overview = new LearningOverviewRepository(context);
    overviewUpdates =
        new LiveOverviewUpdates(
            context, owner -> new com.example.asm_final.backend.BackendEventStream(context, owner));
    profiles = new LocalUserProfileRepository(context);
    dashboard = LocalSubjectSources.create(context);
    streaks = new LocalLearningStreakRepository(context);
    competition =
        new com.example.asm_final.competition.BackendCompetitionRepository(
            context, overview, learners, streaks);
    publicProfiles = new com.example.asm_final.competition.BackendPublicProfiles(context);
    rankingUpdates =
        new LiveOverviewUpdates(
            context,
            owner -> new com.example.asm_final.backend.BackendEventStream(context, owner),
            com.example.asm_final.competition.RankingEventPolicy::affects);
    weeklyQuizzes = new LocalWeeklyQuizRepository(context);
  }

  public com.example.asm_final.onboarding.LearnerProfileRepository learners() {
    return learners;
  }

  public AccountAccess accountAccess() {
    return access;
  }

  public LearningOverviewRepository overview() {
    return overview;
  }

  public OverviewUpdates overviewUpdates() {
    return overviewUpdates;
  }

  public com.example.asm_final.competition.CompetitionRepository competition() {
    return competition;
  }

  public com.example.asm_final.competition.PublicProfileRepository publicProfiles() {
    return publicProfiles;
  }

  public OverviewUpdates rankingUpdates() {
    return rankingUpdates;
  }

  public OverviewUpdates publicProfileUpdates(Context context, String profileId) {
    return new LiveOverviewUpdates(
        context,
        owner -> new com.example.asm_final.backend.BackendEventStream(context, owner),
        (type, id, owner) -> "PROFILE".equals(type) && profileId.equals(id));
  }

  public LearnerRegistration learnerRegistration() {
    return accounts;
  }

  public RecoveryService recovery() {
    return recovery;
  }

  public UserAuthenticator authenticator() {
    return accounts;
  }

  public UserRegistration registration() {
    return accounts;
  }

  public AccountLookup accountLookup() {
    return accounts;
  }

  public UserProfileRepository profiles() {
    return profiles;
  }

  public LearningStreakRepository streaks() {
    return streaks;
  }

  public WeeklyQuizRepository weeklyQuizzes() {
    return weeklyQuizzes;
  }

  public DashboardRepository dashboard() {
    return dashboard;
  }

  public ExecutorService io() {
    return io;
  }

  public com.example.mentor.learning.SubjectCatalog subjects() {
    return subjects;
  }

  public com.example.mentor.chat.TutorGateway tutor() {
    return tutor;
  }

  public com.example.asm_final.chat.ChatStore chats() {
    return chats;
  }
}
