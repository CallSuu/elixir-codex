package com.elixircodex.backend.onboarding;

import com.elixircodex.backend.alchemy.ThemeCategory;

public record OnboardingClassifyResponse(ThemeCategory themeCategory, String labelKo) {
}
