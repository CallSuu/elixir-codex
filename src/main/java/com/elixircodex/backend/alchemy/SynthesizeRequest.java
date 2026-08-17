package com.elixircodex.backend.alchemy;

import java.util.List;

public record SynthesizeRequest(List<Long> ingredientCardIds, ThemeCategory themeCategory) {
}
