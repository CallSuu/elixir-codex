package com.example.demo.Entity;

import java.util.Arrays;

public enum QuestCategory {
    SKIN_ANTIOXIDANT("피부/항산화"),
    FATIGUE_ENERGY("피로/에너지"),
    BLOOD_SUGAR_DIET("혈당/다이어트"),
    SLEEP_REST("수면/휴식");

    private final String label;

    QuestCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static QuestCategory fromLabel(String label) {
        return Arrays.stream(values())
                .filter(category -> category.label.equals(label) || category.name().equalsIgnoreCase(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("올바르지 않은 카테고리입니다: " + label));
    }
}
