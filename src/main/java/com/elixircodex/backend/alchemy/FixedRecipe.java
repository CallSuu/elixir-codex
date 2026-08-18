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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FixedRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private ThemeCategory themeCategory;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ElixirGrade grade = ElixirGrade.EPIC;

    @ElementCollection
    @CollectionTable(name = "fixed_recipe_required_ingredients", joinColumns = @JoinColumn(name = "fixed_recipe_id"))
    @Column(name = "ingredient_name")
    private List<String> requiredIngredientNames;

    @ElementCollection
    @CollectionTable(name = "fixed_recipe_bonus_stats", joinColumns = @JoinColumn(name = "fixed_recipe_id"))
    @Column(name = "stat_name")
    private List<String> bonusStatNames;

    private int bonusPercent;

    private String cardDescription;

    private String adviserComment;

    @Column(columnDefinition = "TEXT")
    private String scientificExplanation;
}
