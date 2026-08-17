package com.elixircodex.backend.specialelixir;

import com.elixircodex.backend.alchemy.ThemeCategory;

import java.time.LocalDateTime;

public record SpecialElixirResponse(Long id, String name, String imageUrl, String adviserComment,
                                     ThemeCategory themeCategory, LocalDateTime createdAt) {
}
