package com.elixircodex.backend.alchemy;

import java.util.List;

public record NameGenerationRequest(ThemeCategory themeCategory, List<String> ingredientNames) {
}
