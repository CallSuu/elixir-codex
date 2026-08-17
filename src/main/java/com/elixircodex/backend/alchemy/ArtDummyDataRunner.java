package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.stack.Grade;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Profile("!prod")
@RequiredArgsConstructor
public class ArtDummyDataRunner implements CommandLineRunner {

    private static final Map<Grade, List<ThemeCategory>> SEEDED_COMBINATIONS = Map.of(
            Grade.EPIC, List.of(ThemeCategory.SKIN_ANTIOXIDANT, ThemeCategory.FATIGUE_ENERGY),
            Grade.RARE, List.of(ThemeCategory.SKIN_ANTIOXIDANT, ThemeCategory.DIET_BLOODSUGAR, ThemeCategory.SLEEP_REST),
            Grade.COMMON, List.of(ThemeCategory.SLEEP_REST, ThemeCategory.FATIGUE_ENERGY)
    );

    private static final Map<String, Integer> TEMPLATE_COUNT_BY_COMBO_KEY = Map.of(
            "EPIC-SKIN_ANTIOXIDANT", 2,
            "EPIC-FATIGUE_ENERGY", 1,
            "RARE-SKIN_ANTIOXIDANT", 2,
            "RARE-DIET_BLOODSUGAR", 1,
            "RARE-SLEEP_REST", 1,
            "COMMON-SLEEP_REST", 2,
            "COMMON-FATIGUE_ENERGY", 1
    );

    private final ArtTemplateRepository artTemplateRepository;

    @Override
    public void run(String... args) {
        if (artTemplateRepository.count() > 0) {
            return;
        }

        List<ArtTemplate> templates = new ArrayList<>();

        SEEDED_COMBINATIONS.forEach((grade, themes) -> themes.forEach(theme -> {
            String comboKey = grade.name() + "-" + theme.name();
            int count = TEMPLATE_COUNT_BY_COMBO_KEY.get(comboKey);
            for (int i = 1; i <= count; i++) {
                templates.add(ArtTemplate.builder()
                        .grade(grade)
                        .themeCategory(theme)
                        .imageUrl("https://placehold.co/400x600?text=" + comboKey + "-" + i)
                        .build());
            }
        }));

        artTemplateRepository.saveAll(templates);
    }
}
