package com.elixircodex.backend.auth;

public class AuthenticatedUserException extends RuntimeException {

    public AuthenticatedUserException(String message) {
        super(message);
    }
}
