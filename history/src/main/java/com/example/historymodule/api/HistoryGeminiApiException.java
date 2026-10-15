package com.example.historymodule.api;

/** HTTP error returned by the Gemini API used only by the History module. */
public final class HistoryGeminiApiException extends Exception {
    private final int httpStatus;
    private final String apiStatus;

    public HistoryGeminiApiException(int httpStatus, String apiStatus, String message) {
        super(message == null ? "" : message.trim());
        this.httpStatus = httpStatus;
        this.apiStatus = apiStatus == null ? "" : apiStatus.trim();
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getApiStatus() {
        return apiStatus;
    }
}
