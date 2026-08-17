package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.stack.Grade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ArtMatchingService {

    private static final String DEFAULT_IMAGE_URL = "https://placehold.co/400x600?text=Elixir";

    private final ArtTemplateRepository artTemplateRepository;

    public String findImageUrl(Grade grade, ThemeCategory themeCategory) {
        List<ArtTemplate> templates = artTemplateRepository.findByGradeAndThemeCategory(grade, themeCategory);
        if (templates.isEmpty()) {
            return DEFAULT_IMAGE_URL;
        }
        return templates.get(ThreadLocalRandom.current().nextInt(templates.size())).getImageUrl();
    }
}
