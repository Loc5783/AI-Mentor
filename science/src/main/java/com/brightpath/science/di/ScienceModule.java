package com.brightpath.science.di;

import android.content.Context;

import com.brightpath.science.data.local.AndroidKeystoreTextCipher;
import com.brightpath.science.data.local.ScienceDbHelper;
import com.brightpath.science.data.local.TextCipher;
import com.brightpath.science.data.remote.GeminiApiClient;
import com.brightpath.science.data.remote.GeminiScienceService;
import com.brightpath.science.data.remote.LocalScienceService;
import com.brightpath.science.data.remote.ResilientScienceService;
import com.brightpath.science.data.remote.GeminiScienceStudyPlanGenerator;
import com.brightpath.science.data.remote.LocalScienceStudyPlanGenerator;
import com.brightpath.science.data.remote.ResilientScienceStudyPlanGenerator;
import com.brightpath.science.data.repository.AssetPeriodicTableRepository;
import com.brightpath.science.data.repository.SQLiteQuizAttemptRepository;
import com.brightpath.science.data.repository.SQLiteScienceBookmarkRepository;
import com.brightpath.science.data.repository.SQLiteScienceHistoryRepository;
import com.brightpath.science.data.repository.SharedPreferencesQuizVariationRepository;
import com.brightpath.science.data.repository.SharedPreferencesStudyPlanRepository;
import com.brightpath.science.domain.repository.PeriodicTableRepository;
import com.brightpath.science.domain.repository.QuizAttemptRepository;
import com.brightpath.science.domain.repository.ScienceBookmarkRepository;
import com.brightpath.science.domain.repository.ScienceHistoryRepository;
import com.brightpath.science.domain.repository.QuizVariationRepository;
import com.brightpath.science.domain.repository.StudyPlanRepository;
import com.brightpath.science.domain.policy.AdaptiveQuizPolicy;
import com.brightpath.science.domain.policy.impl.PerformanceBasedQuizPolicy;
import com.brightpath.science.domain.usecase.AskScienceQuestionUseCase;
import com.brightpath.science.domain.usecase.GenerateScienceQuizUseCase;
import com.brightpath.science.domain.usecase.GetScienceStatsUseCase;
import com.brightpath.science.domain.usecase.SaveBookmarkUseCase;
import com.brightpath.science.domain.usecase.GenerateStudyPlanUseCase;
import com.brightpath.science.util.BiologyTutorPromptStrategy;
import com.brightpath.science.util.ChemistryTutorPromptStrategy;
import com.brightpath.science.util.DefaultSciencePromptFactory;
import com.brightpath.science.util.PhysicsTutorPromptStrategy;
import com.brightpath.science.util.QuestionFingerprint;
import com.brightpath.science.util.QuestionNormalizer;
import com.brightpath.science.util.ScienceQuestionValidator;

/** Composition root owned by the Science module. */
public final class ScienceModule {
    private static volatile ScienceModule instance;

    private final ScienceHistoryRepository historyRepository;
    private final ScienceBookmarkRepository bookmarkRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final PeriodicTableRepository periodicTableRepository;
    private final AskScienceQuestionUseCase askScienceQuestionUseCase;
    private final SaveBookmarkUseCase saveBookmarkUseCase;
    private final GenerateScienceQuizUseCase generateScienceQuizUseCase;
    private final GetScienceStatsUseCase getScienceStatsUseCase;
    private final StudyPlanRepository studyPlanRepository;
    private final GenerateStudyPlanUseCase generateStudyPlanUseCase;

    private ScienceModule(Context context) {
        Context appContext = context.getApplicationContext();
        ScienceDbHelper dbHelper = new ScienceDbHelper(appContext);
        QuestionNormalizer normalizer = new QuestionNormalizer();
        QuestionFingerprint fingerprint = new QuestionFingerprint();
        TextCipher textCipher = new AndroidKeystoreTextCipher();

        historyRepository = new SQLiteScienceHistoryRepository(dbHelper, normalizer, fingerprint, textCipher);
        bookmarkRepository = new SQLiteScienceBookmarkRepository(dbHelper, textCipher);
        quizAttemptRepository = new SQLiteQuizAttemptRepository(dbHelper);
        QuizVariationRepository quizVariationRepository =
                new SharedPreferencesQuizVariationRepository(appContext);
        AdaptiveQuizPolicy adaptiveQuizPolicy = new PerformanceBasedQuizPolicy(
                quizAttemptRepository, quizVariationRepository);
        periodicTableRepository = new AssetPeriodicTableRepository(appContext);
        studyPlanRepository = new SharedPreferencesStudyPlanRepository(appContext, textCipher);

        DefaultSciencePromptFactory promptFactory = new DefaultSciencePromptFactory(
                new PhysicsTutorPromptStrategy(),
                new ChemistryTutorPromptStrategy(),
                new BiologyTutorPromptStrategy()
        );
        GeminiApiClient aiClient = new GeminiApiClient();
        GeminiScienceService gemini = new GeminiScienceService(promptFactory, aiClient);
        LocalScienceService local = new LocalScienceService();
        ResilientScienceService resilientService = new ResilientScienceService(gemini, local);

        askScienceQuestionUseCase = new AskScienceQuestionUseCase(
                resilientService,
                historyRepository,
                new ScienceQuestionValidator(),
                normalizer
        );
        saveBookmarkUseCase = new SaveBookmarkUseCase(bookmarkRepository);
        generateScienceQuizUseCase = new GenerateScienceQuizUseCase(
                resilientService, adaptiveQuizPolicy, quizVariationRepository);
        getScienceStatsUseCase = new GetScienceStatsUseCase(
                historyRepository,
                bookmarkRepository,
                quizAttemptRepository
        );
        ResilientScienceStudyPlanGenerator studyPlanGenerator =
                new ResilientScienceStudyPlanGenerator(
                        new GeminiScienceStudyPlanGenerator(aiClient),
                        new LocalScienceStudyPlanGenerator());
        generateStudyPlanUseCase = new GenerateStudyPlanUseCase(
                studyPlanGenerator, studyPlanRepository);
    }

    public static ScienceModule get(Context context) {
        if (instance == null) {
            synchronized (ScienceModule.class) {
                if (instance == null) instance = new ScienceModule(context);
            }
        }
        return instance;
    }

    public ScienceHistoryRepository historyRepository() { return historyRepository; }
    public ScienceBookmarkRepository bookmarkRepository() { return bookmarkRepository; }
    public QuizAttemptRepository quizAttemptRepository() { return quizAttemptRepository; }
    public PeriodicTableRepository periodicTableRepository() { return periodicTableRepository; }
    public AskScienceQuestionUseCase askScienceQuestionUseCase() { return askScienceQuestionUseCase; }
    public SaveBookmarkUseCase saveBookmarkUseCase() { return saveBookmarkUseCase; }
    public GenerateScienceQuizUseCase generateScienceQuizUseCase() { return generateScienceQuizUseCase; }
    public GetScienceStatsUseCase getScienceStatsUseCase() { return getScienceStatsUseCase; }
    public StudyPlanRepository studyPlanRepository() { return studyPlanRepository; }
    public GenerateStudyPlanUseCase generateStudyPlanUseCase() { return generateStudyPlanUseCase; }
}
