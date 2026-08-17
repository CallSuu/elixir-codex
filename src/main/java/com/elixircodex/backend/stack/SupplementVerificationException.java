package com.elixircodex.backend.stack;

public class SupplementVerificationException extends RuntimeException {

    public SupplementVerificationException(String message) {
        super(message);
    }

    public SupplementVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
