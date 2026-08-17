package com.elixircodex.backend.stack;

public enum Grade {
    COMMON(1),
    RARE(3),
    EPIC(7);

    private final int score;

    Grade(int score) {
        this.score = score;
    }

    public int score() {
        return score;
    }
}
