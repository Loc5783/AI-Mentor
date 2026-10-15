package com.brightpath.science.domain.service;

public interface ResultCallback<T> {
    void onSuccess(T result);
    void onError(String message, Throwable error);
}
