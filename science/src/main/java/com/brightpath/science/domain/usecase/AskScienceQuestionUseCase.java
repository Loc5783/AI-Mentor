package com.brightpath.science.domain.usecase;

import com.brightpath.science.domain.model.ScienceAnswer;
import com.brightpath.science.domain.model.ScienceHistory;
import com.brightpath.science.domain.model.ScienceQuestion;
import com.brightpath.science.domain.repository.ScienceHistoryRepository;
import com.brightpath.science.domain.service.ResultCallback;
import com.brightpath.science.domain.service.ScienceAnswerService;
import com.brightpath.science.util.QuestionNormalizer;
import com.brightpath.science.util.ScienceQuestionValidator;

/** Coordinates validation, cache lookup, AI response and best-effort persistence. */
public final class AskScienceQuestionUseCase {
    private final ScienceAnswerService answerService;
    private final ScienceHistoryRepository historyRepository;
    private final ScienceQuestionValidator validator;
    private final QuestionNormalizer normalizer;

    public AskScienceQuestionUseCase(ScienceAnswerService answerService,
                                     ScienceHistoryRepository historyRepository,
                                     ScienceQuestionValidator validator,
                                     QuestionNormalizer normalizer) {
        this.answerService = answerService;
        this.historyRepository = historyRepository;
        this.validator = validator;
        this.normalizer = normalizer;
    }

    public void execute(ScienceQuestion question, ResultCallback<ScienceAnswer> callback) {
        ScienceQuestionValidator.ValidationResult validation = validator.validate(question);
        if (!validation.isValid()) {
            callback.onError(validation.getMessage(), null);
            return;
        }

        String normalizedQuestion = normalizer.normalize(question.getQuestionText());
        if (isSafeStandaloneCacheRequest(question, normalizedQuestion)) {
            try {
                ScienceHistory cached = historyRepository.findCached(
                        question.getUserId(),
                        question.getSubject(),
                        question.getEducationLevel().name(),
                        normalizedQuestion
                );
                if (cached != null) {
                    long historyId = cached.getId();
                    try {
                        historyId = historyRepository.save(question, cached.getAnswer());
                    } catch (Exception ignored) {
                        // The cached answer is still usable when a new history row cannot be saved.
                    }
                    callback.onSuccess(new ScienceAnswer(
                            cached.getAnswer(),
                            cached.getSubject().getDisplayName(),
                            cached.getEducationLevel(),
                            true,
                            historyId
                    ));
                    return;
                }
            } catch (Exception ignored) {
                // A damaged or temporarily locked local database must not block the AI response.
            }
        }

        answerService.answer(question, new ResultCallback<ScienceAnswer>() {
            @Override
            public void onSuccess(ScienceAnswer answer) {
                long historyId = 0L;
                try {
                    historyId = historyRepository.save(question, answer.getContent());
                } catch (Exception ignored) {
                    // The answer remains usable even if encrypted local persistence fails.
                }
                callback.onSuccess(answer.withPersistence(historyId, false));
            }

            @Override
            public void onError(String message, Throwable error) {
                callback.onError(message, error);
            }
        });
    }
    /**
     * Cached answers are safe only for standalone questions. A short follow-up such as
     * "why?" or "continue step 2" must be answered with the current conversation, never
     * by reusing an unrelated historical answer with the same short text.
     */
    private boolean isSafeStandaloneCacheRequest(ScienceQuestion question,
                                                 String normalizedQuestion) {
        if (question == null || question.hasImage() || question.hasConversationContext()) {
            return false;
        }
        if (normalizedQuestion == null || normalizedQuestion.length() < 12) {
            return false;
        }

        String value = normalizedQuestion.trim();
        String[] contextDependentPrefixes = {
                "why", "why is", "why does", "how so", "continue", "go on",
                "explain again", "what about", "and then", "step",
                "tai sao", "vi sao", "the con", "vay con", "con", "tiep tuc",
                "giai thich lai", "buoc", "do la gi", "cai nay", "truong hop kia"
        };
        for (String prefix : contextDependentPrefixes) {
            if (value.equals(prefix) || value.startsWith(prefix + " ")) {
                return false;
            }
        }
        return true;
    }

}
