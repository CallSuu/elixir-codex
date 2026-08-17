package com.elixircodex.backend.alchemy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AlchemyControllerTest {

    @Mock
    private AlchemyNameService alchemyNameService;

    private AlchemyController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new AlchemyController(alchemyNameService);
    }

    @Test
    void 재료가_null이면_GPT를_호출하지_않고_400을_반환한다() {
        NameGenerationRequest request = new NameGenerationRequest(ThemeCategory.SKIN_ANTIOXIDANT, null);

        ResponseEntity<?> response = controller.generateName(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<String, String>) response.getBody())).containsEntry("message", "재료가 최소 2개 이상 필요합니다");
        verifyNoInteractions(alchemyNameService);
    }

    @Test
    void 재료가_1개면_GPT를_호출하지_않고_400을_반환한다() {
        NameGenerationRequest request = new NameGenerationRequest(ThemeCategory.SKIN_ANTIOXIDANT, List.of("천년삼"));

        ResponseEntity<?> response = controller.generateName(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<String, String>) response.getBody())).containsEntry("message", "재료가 최소 2개 이상 필요합니다");
        verifyNoInteractions(alchemyNameService);
    }
}
