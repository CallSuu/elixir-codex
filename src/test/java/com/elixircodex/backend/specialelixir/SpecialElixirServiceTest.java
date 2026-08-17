package com.elixircodex.backend.specialelixir;

import com.elixircodex.backend.alchemy.AlchemyNameService;
import com.elixircodex.backend.alchemy.ArtMatchingService;
import com.elixircodex.backend.alchemy.NameGenerationResponse;
import com.elixircodex.backend.alchemy.ThemeCategory;
import com.elixircodex.backend.onboarding.OnboardingClassificationService;
import com.elixircodex.backend.stack.Grade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialElixirServiceTest {

    @Mock
    private SpecialElixirRepository specialElixirRepository;
    @Mock
    private OnboardingClassificationService onboardingClassificationService;
    @Mock
    private AlchemyNameService alchemyNameService;
    @Mock
    private ArtMatchingService artMatchingService;

    private SpecialElixirService service() {
        return new SpecialElixirService(specialElixirRepository, onboardingClassificationService,
                alchemyNameService, artMatchingService);
    }

    @Test
    void 정상_생성시_분류결과와_생성된_이름_이미지로_저장된다() {
        when(specialElixirRepository.countByOwnerId(1L)).thenReturn(0L);
        when(onboardingClassificationService.classify("잠을 잘 못 자요")).thenReturn(ThemeCategory.SLEEP_REST);
        when(alchemyNameService.generateName(ThemeCategory.SLEEP_REST, List.of("잠을 잘 못 자요"), false))
                .thenReturn(new NameGenerationResponse("은은한 밤의 안식 포션", "숙면에 도움을 줍니다."));
        when(artMatchingService.findImageUrl(Grade.EPIC, ThemeCategory.SLEEP_REST))
                .thenReturn("https://placehold.co/400x600?text=EPIC-SLEEP_REST-1");
        when(specialElixirRepository.save(any(SpecialElixir.class)))
                .thenAnswer(invocation -> {
                    SpecialElixir arg = invocation.getArgument(0);
                    return SpecialElixir.builder()
                            .id(10L).ownerId(arg.getOwnerId()).name(arg.getName()).imageUrl(arg.getImageUrl())
                            .adviserComment(arg.getAdviserComment()).themeCategory(arg.getThemeCategory())
                            .build();
                });

        SpecialElixirResponse response = service().create(1L, "잠을 잘 못 자요");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("은은한 밤의 안식 포션");
        assertThat(response.imageUrl()).isEqualTo("https://placehold.co/400x600?text=EPIC-SLEEP_REST-1");
        assertThat(response.adviserComment()).isEqualTo("숙면에 도움을 줍니다.");
        assertThat(response.themeCategory()).isEqualTo(ThemeCategory.SLEEP_REST);
    }

    @Test
    void 이미_3개를_보유하고_있으면_예외를_던지고_GPT를_호출하지_않는다() {
        when(specialElixirRepository.countByOwnerId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service().create(1L, "잠을 잘 못 자요"))
                .isInstanceOf(SpecialElixirValidationException.class)
                .hasMessage("스페셜 엘릭서는 최대 3개까지 저장할 수 있습니다. 기존 엘릭서를 삭제한 뒤 다시 시도해주세요.");

        verifyNoInteractions(onboardingClassificationService, alchemyNameService, artMatchingService);
    }

    @Test
    void 목록_조회는_ownerId의_엘릭서를_전부_반환한다() {
        SpecialElixir elixir = SpecialElixir.builder()
                .id(1L).ownerId(1L).name("은은한 밤의 안식 포션").imageUrl("url")
                .adviserComment("조언").themeCategory(ThemeCategory.SLEEP_REST).build();
        when(specialElixirRepository.findByOwnerId(1L)).thenReturn(List.of(elixir));

        List<SpecialElixirResponse> result = service().list(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("은은한 밤의 안식 포션");
    }

    @Test
    void 본인_엘릭서를_삭제하면_정상적으로_삭제된다() {
        SpecialElixir elixir = SpecialElixir.builder()
                .id(1L).ownerId(1L).name("이름").imageUrl("url")
                .adviserComment("조언").themeCategory(ThemeCategory.SLEEP_REST).build();
        when(specialElixirRepository.findById(1L)).thenReturn(Optional.of(elixir));

        service().delete(1L, 1L);

        ArgumentCaptor<SpecialElixir> captor = ArgumentCaptor.forClass(SpecialElixir.class);
        verify(specialElixirRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
    }

    @Test
    void 본인_것이_아닌_엘릭서를_삭제하려하면_예외를_던진다() {
        SpecialElixir elixir = SpecialElixir.builder()
                .id(1L).ownerId(2L).name("이름").imageUrl("url")
                .adviserComment("조언").themeCategory(ThemeCategory.SLEEP_REST).build();
        when(specialElixirRepository.findById(1L)).thenReturn(Optional.of(elixir));

        assertThatThrownBy(() -> service().delete(1L, 1L))
                .isInstanceOf(SpecialElixirValidationException.class)
                .hasMessage("본인의 엘릭서만 삭제할 수 있습니다");

        verify(specialElixirRepository, never()).delete(any());
    }

    @Test
    void 존재하지_않는_엘릭서를_삭제하려하면_예외를_던진다() {
        when(specialElixirRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(1L, 999L))
                .isInstanceOf(SpecialElixirValidationException.class)
                .hasMessage("존재하지 않는 엘릭서입니다");

        verify(specialElixirRepository, never()).delete(any());
    }
}
