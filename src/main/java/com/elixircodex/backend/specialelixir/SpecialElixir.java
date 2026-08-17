package com.elixircodex.backend.specialelixir;

import com.elixircodex.backend.alchemy.ThemeCategory;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SpecialElixir {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId;

    private String name;

    private String imageUrl;

    private String adviserComment;

    @Enumerated(EnumType.STRING)
    private ThemeCategory themeCategory;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
