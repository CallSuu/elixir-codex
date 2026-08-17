package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.stack.Grade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlchemyArtControllerTest {

    @Mock
    private ArtMatchingService artMatchingService;

    private AlchemyArtController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new AlchemyArtController(artMatchingService);
    }

    @Test
    void 유효한_등급과_테마면_이미지URL을_반환한다() {
        when(artMatchingService.findImageUrl(Grade.EPIC, ThemeCategory.SKIN_ANTIOXIDANT))
                .thenReturn("https://placehold.co/400x600?text=EPIC-SKIN_ANTIOXIDANT-1");

        ResponseEntity<?> response = controller.getArt(Grade.EPIC, ThemeCategory.SKIN_ANTIOXIDANT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<String, String>) response.getBody()))
                .containsEntry("imageUrl", "https://placehold.co/400x600?text=EPIC-SKIN_ANTIOXIDANT-1");
    }
}
