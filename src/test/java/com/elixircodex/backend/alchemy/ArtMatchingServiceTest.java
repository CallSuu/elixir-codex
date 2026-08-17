package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.stack.Grade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtMatchingServiceTest {

    @Mock
    private ArtTemplateRepository artTemplateRepository;

    private ArtMatchingService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new ArtMatchingService(artTemplateRepository);
    }

    @Test
    void 템플릿이_없으면_기본_이미지를_반환한다() {
        when(artTemplateRepository.findByGradeAndThemeCategory(Grade.EPIC, ThemeCategory.SLEEP_REST))
                .thenReturn(List.of());

        String imageUrl = service.findImageUrl(Grade.EPIC, ThemeCategory.SLEEP_REST);

        assertThat(imageUrl).isEqualTo("https://placehold.co/400x600?text=Elixir");
    }

    @Test
    void 템플릿이_하나면_그대로_반환한다() {
        ArtTemplate template = ArtTemplate.builder()
                .grade(Grade.RARE).themeCategory(ThemeCategory.DIET_BLOODSUGAR)
                .imageUrl("https://placehold.co/400x600?text=RARE-DIET_BLOODSUGAR-1")
                .build();
        when(artTemplateRepository.findByGradeAndThemeCategory(Grade.RARE, ThemeCategory.DIET_BLOODSUGAR))
                .thenReturn(List.of(template));

        String imageUrl = service.findImageUrl(Grade.RARE, ThemeCategory.DIET_BLOODSUGAR);

        assertThat(imageUrl).isEqualTo("https://placehold.co/400x600?text=RARE-DIET_BLOODSUGAR-1");
    }

    @Test
    void 템플릿이_여러개면_그중_하나를_반환한다() {
        List<ArtTemplate> templates = List.of(
                ArtTemplate.builder().grade(Grade.EPIC).themeCategory(ThemeCategory.SKIN_ANTIOXIDANT)
                        .imageUrl("url-1").build(),
                ArtTemplate.builder().grade(Grade.EPIC).themeCategory(ThemeCategory.SKIN_ANTIOXIDANT)
                        .imageUrl("url-2").build()
        );
        when(artTemplateRepository.findByGradeAndThemeCategory(Grade.EPIC, ThemeCategory.SKIN_ANTIOXIDANT))
                .thenReturn(templates);

        String imageUrl = service.findImageUrl(Grade.EPIC, ThemeCategory.SKIN_ANTIOXIDANT);

        assertThat(imageUrl).isIn("url-1", "url-2");
    }
}
