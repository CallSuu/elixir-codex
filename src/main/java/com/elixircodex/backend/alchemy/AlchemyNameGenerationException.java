package com.elixircodex.backend.alchemy;

public class AlchemyNameGenerationException extends RuntimeException {

    public AlchemyNameGenerationException(String message) {
        super(message);
    }

    public AlchemyNameGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
