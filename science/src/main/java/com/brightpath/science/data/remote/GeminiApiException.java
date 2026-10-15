package com.brightpath.science.data.remote;

/** Structured Gemini HTTP failure used to show an accurate, user-friendly error. */
public final class GeminiApiException extends Exception {
    private final int httpStatus;
    private final String apiStatus;

    public GeminiApiException(int httpStatus, String apiStatus, String message) {
        super(message == null || message.trim().isEmpty()
                ? "Gemini request failed with HTTP " + httpStatus + "."
                : message.trim());
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
