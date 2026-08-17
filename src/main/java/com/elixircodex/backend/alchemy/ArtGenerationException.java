package com.elixircodex.backend.alchemy;

public class ArtGenerationException extends RuntimeException {

    public ArtGenerationException(String message) {
        super(message);
    }

    public ArtGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
