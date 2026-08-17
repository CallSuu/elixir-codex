package com.elixircodex.backend.alchemy;

public enum ThemeCategory {
    SKIN_ANTIOXIDANT("피부/항산화"),
    FATIGUE_ENERGY("피로/에너지"),
    DIET_BLOODSUGAR("혈당/다이어트"),
    SLEEP_REST("수면/휴식");

    private final String labelKo;

    ThemeCategory(String labelKo) {
        this.labelKo = labelKo;
    }

    public String labelKo() {
        return labelKo;
    }
}
