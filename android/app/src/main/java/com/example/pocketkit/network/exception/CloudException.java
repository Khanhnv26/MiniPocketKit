package com.example.pocketkit.network.exception;

import lombok.NonNull;

public class CloudException extends Exception {

    private final int httpCode;

    public CloudException(int httpCode, @NonNull String message) {
        super(message);
        this.httpCode = httpCode;
    }

    public int getHttpCode() {
        return httpCode;
    }
}
