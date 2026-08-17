package com.elixircodex.backend.onboarding;

public class OnboardingClassificationException extends RuntimeException {

    public OnboardingClassificationException(String message) {
        super(message);
    }

    public OnboardingClassificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
