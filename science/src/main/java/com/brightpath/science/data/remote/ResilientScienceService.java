package com.brightpath.science.data.remote;

import com.brightpath.science.domain.model.QuizQuestion;
import com.brightpath.science.domain.model.QuizRequest;
import com.brightpath.science.domain.model.ScienceAnswer;
import com.brightpath.science.domain.model.ScienceQuestion;
import com.brightpath.science.domain.service.ResultCallback;
import com.brightpath.science.domain.service.ScienceAnswerService;
import com.brightpath.science.domain.service.ScienceQuizGenerator;

import java.util.List;
import java.util.Locale;

public final class ResilientScienceService implements ScienceAnswerService, ScienceQuizGenerator {
    private final GeminiScienceService remoteService;
    private final LocalScienceService localService;

    public ResilientScienceService(GeminiScienceService remoteService, LocalScienceService localService) {
        this.remoteService = remoteService;
        this.localService = localService;
    }

    @Override
    public void answer(ScienceQuestion question, ResultCallback<ScienceAnswer> callback) {
        if (!remoteService.isConfigured()) {
            localService.answer(question, callback);
            return;
        }

        remoteService.answer(question, new ResultCallback<ScienceAnswer>() {
            @Override
            public void onSuccess(ScienceAnswer result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String message, Throwable error) {
                if (isCredentialOrPermissionError(error, message)) {
                    callback.onError(message, error);
                    return;
                }
                // Network, quota, timeout and temporary server failures should not
                // make the Science screen unusable. Continue with the local tutor.
                localService.answer(question, callback);
            }
        });
    }

    @Override
    public void generateQuiz(QuizRequest request, ResultCallback<List<QuizQuestion>> callback) {
        if (!remoteService.isConfigured()) {
            localService.generateQuiz(request, callback);
            return;
        }
        remoteService.generateQuiz(request, new ResultCallback<List<QuizQuestion>>() {
            @Override
            public void onSuccess(List<QuizQuestion> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String message, Throwable error) {
                if (isCredentialOrPermissionError(error, message)) {
                    callback.onError(message, error);
                    return;
                }
                localService.generateQuiz(request, callback);
            }
        });
    }

    private boolean isCredentialOrPermissionError(Throwable error, String callbackMessage) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof GeminiApiException) {
                GeminiApiException apiError = (GeminiApiException) current;
                int status = apiError.getHttpStatus();
                String apiStatus = apiError.getApiStatus().toUpperCase(Locale.ROOT);
                if (status == 401 || status == 403
                        || apiStatus.contains("UNAUTHENTICATED")
                        || apiStatus.contains("PERMISSION_DENIED")) {
                    return true;
                }
            }
            if (containsCredentialMessage(current.getMessage())) return true;
            current = current.getCause();
        }
        return containsCredentialMessage(callbackMessage);
    }

    private boolean containsCredentialMessage(String message) {
        if (message == null) return false;
        String normalized = message.toLowerCase(Locale.ROOT);
        return (normalized.contains("api key") || normalized.contains("credential"))
                && (normalized.contains("invalid")
                || normalized.contains("permission")
                || normalized.contains("unauthorized")
                || normalized.contains("forbidden"));
    }
}
