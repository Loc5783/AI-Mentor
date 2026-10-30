package com.example.asm_final;

import android.content.Context;
import com.example.asm_final.accounts.*;
import com.example.asm_final.dashboard.*;
import java.util.concurrent.*;

/** Application composition root. Screens consume focused contracts rather than database helpers. */
public final class AppServices {
  private final com.example.mentor.learning.SubjectCatalog subjects =
      com.example.asm_final.learning.SubjectModules.create();
  private final com.example.mentor.chat.TutorGateway tutor;
  private final LocalAccountRepository accounts;
  private final AccountAccess access;
  private final LearningOverviewRepository overview;
  private final OverviewUpdates overviewUpdates;
  private final OverviewUpdates rankingUpdates;
  private final com.example.asm_final.competition.CompetitionRepository competition;
  private final com.example.asm_final.competition.PublicProfileRepository publicProfiles;
  private final com.example.asm_final.competition.FriendshipRepository friendships;
  private final com.example.asm_final.chat.ChatStore chats;
  private final com.example.asm_final.onboarding.LearnerProfileRepository learners;
  private final RecoveryService recovery;
  private final com.example.asm_final.supabase.SupabaseStudyRepository study;
  private final Context context;
  private final UserProfileRepository profiles;
  private final ExecutorService io = Executors.newFixedThreadPool(2);

  public AppServices(Context context) {
    this.context = context.getApplicationContext();
    tutor = com.example.asm_final.supabase.SupabaseHttp.enabled(context)
      ? new com.example.asm_final.supabase.SupabaseTutorGateway(context)
      : com.example.mentor.chat.GeminiTutorGateway.configuredClient();
    accounts = new LocalAccountRepository(context);
    recovery = com.example.asm_final.supabase.SupabaseHttp.enabled(context)
        ? new com.example.asm_final.supabase.SupabaseRecoveryService(context)
        : new UnavailableRecoveryService();
    study = com.example.asm_final.supabase.SupabaseHttp.enabled(context)
        ? new com.example.asm_final.supabase.SupabaseStudyRepository(context)
        : null;
    chats = com.example.asm_final.supabase.SupabaseHttp.enabled(context)
      ? new com.example.asm_final.supabase.SupabaseChatStore(context)
      : new com.example.asm_final.chat.LocalChatStore(context);
    learners = com.example.asm_final.supabase.SupabaseHttp.enabled(context)
      ? new com.example.asm_final.supabase.SupabaseLearnerProfiles(context)
      : new com.example.asm_final.onboarding.LocalLearnerProfiles(context);
    access = new AccountAccess(context, accounts, learners);
    overview = new LearningOverviewRepository(context);
    overviewUpdates =
        new LiveOverviewUpdates(
            context, owner -> com.example.asm_final.supabase.SupabaseHttp.enabled(context)
                ? new com.example.asm_final.supabase.SupabaseEventStream(context, owner)
                : new com.example.asm_final.backend.BackendEventStream(context, owner));
    profiles = com.example.asm_final.supabase.SupabaseHttp.enabled(context)
        ? new com.example.asm_final.supabase.SupabaseUserProfiles(context)
        : new LocalUserProfileRepository(context);
    competition = com.example.asm_final.supabase.SupabaseHttp.enabled(context)
        ? new com.example.asm_final.competition.SupabaseCompetitionRepository(context, overview, learners)
        : new com.example.asm_final.competition.BackendCompetitionRepository(
            context, overview, learners);
    publicProfiles = new com.example.asm_final.competition.BackendPublicProfiles(context);
    friendships = com.example.asm_final.supabase.SupabaseHttp.enabled(context)
        ? new com.example.asm_final.competition.SupabaseFriendshipRepository(context)
        : new com.example.asm_final.competition.BackendFriendshipRepository(context);
    rankingUpdates =
        new LiveOverviewUpdates(
            context,
            owner -> com.example.asm_final.supabase.SupabaseHttp.enabled(context)
                ? new com.example.asm_final.supabase.SupabaseEventStream(context, owner)
                : new com.example.asm_final.backend.BackendEventStream(context, owner),
            com.example.asm_final.competition.RankingEventPolicy::affects);
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

  public OverviewUpdates chatUpdates() {
    return new LiveOverviewUpdates(
        context,
        owner -> com.example.asm_final.supabase.SupabaseHttp.enabled(context)
            ? new com.example.asm_final.supabase.SupabaseEventStream(context, owner)
            : new com.example.asm_final.backend.BackendEventStream(context, owner),
        (type, id, owner) -> "CHAT".equals(type));
  }

  public com.example.asm_final.competition.CompetitionRepository competition() {
    return competition;
  }

  public com.example.asm_final.competition.PublicProfileRepository publicProfiles() {
    return publicProfiles;
  }

  public com.example.asm_final.competition.FriendshipRepository friendships() {
    return friendships;
  }

  public OverviewUpdates rankingUpdates() {
    return rankingUpdates;
  }

  public OverviewUpdates publicProfileUpdates(Context context, String profileId) {
    return new LiveOverviewUpdates(
        context,
        owner -> com.example.asm_final.supabase.SupabaseHttp.enabled(context)
            ? new com.example.asm_final.supabase.SupabaseEventStream(context, owner)
            : new com.example.asm_final.backend.BackendEventStream(context, owner),
        (type, id, owner) -> "PROFILE".equals(type) && profileId.equals(id));
  }

  public LearnerRegistration learnerRegistration() {
    return accounts;
  }

  public RecoveryService recovery() {
    return recovery;
  }

  public com.example.asm_final.supabase.SupabaseStudyRepository study() {
    return study;
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

  public com.example.asm_final.learning.LibraryRepository library(int owner) {
    return com.example.asm_final.supabase.SupabaseHttp.enabled(context)
        ? new com.example.asm_final.learning.SupabaseLibraryRepository(context, owner)
        : new com.example.asm_final.learning.BackendLibraryRepository(context, owner);
  }
  public com.example.mentor.schedule.domain.ScheduleImportRepository scheduleImport() {
    return com.example.asm_final.supabase.SupabaseHttp.enabled(context)
      ? new com.example.asm_final.supabase.SupabaseScheduleRepository(context)
      : new com.example.mentor.schedule.data.ScheduleRepositoryImpl(context);
  }
}
