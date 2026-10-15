package com.example.mathmodule.api;

/** Structured Gemini HTTP failure used to display an accurate error to the user. */
public final class MathGeminiApiException extends Exception {
    private final int httpStatus;
    private final String apiStatus;

    public MathGeminiApiException(int httpStatus, String apiStatus, String message) {
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
