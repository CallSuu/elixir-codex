package com.elixircodex.backend.alchemy;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ElixirCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId;

    private String name;

    @Enumerated(EnumType.STRING)
    private ElixirGrade grade;

    @Enumerated(EnumType.STRING)
    private ThemeCategory themeCategory;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String imageUrl;

    private String adviserComment;

    private Long serialNumber;

    private String ingredientSummary;

    private boolean isMutated;

    @Column(columnDefinition = "TEXT")
    private String scientificExplanation;

    @Column(columnDefinition = "TEXT")
    private String cardDescription;

    @ElementCollection
    @CollectionTable(name = "elixir_card_stats", joinColumns = @JoinColumn(name = "elixir_card_id"))
    @MapKeyColumn(name = "stat_name")
    @Column(name = "stat_value")
    private Map<String, Integer> stats;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
