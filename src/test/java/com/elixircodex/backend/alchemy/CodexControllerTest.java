package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodexControllerTest {

    @Mock
    private ElixirCardRepository elixirCardRepository;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    private CodexController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new CodexController(elixirCardRepository, authenticatedUserService);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void 소유자의_카드목록을_요약형태로_반환하되_돌연변이는_제외한다() {
        ElixirCard normal = ElixirCard.builder()
                .id(1L).ownerId(1L).name("심해의 정화 오일").grade(ElixirGrade.EPIC)
                .themeCategory(ThemeCategory.SKIN_ANTIOXIDANT).imageUrl("url").serialNumber(null)
                .build();
        ElixirCard mutated = ElixirCard.builder()
                .id(2L).ownerId(1L).name("검은 잔영의 변종수").grade(ElixirGrade.RARE)
                .themeCategory(ThemeCategory.SKIN_ANTIOXIDANT).imageUrl("url2").serialNumber(null)
                .isMutated(true)
                .build();
        when(elixirCardRepository.findByOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(mutated, normal));

        List<CodexCardSummary> result = controller.getCodex();

        assertThat(result).containsExactly(
                new CodexCardSummary(1L, "심해의 정화 오일", ElixirGrade.EPIC, "url", null, false));
    }

    @Test
    void 돌연변이_목록_조회는_돌연변이_카드만_반환한다() {
        ElixirCard normal = ElixirCard.builder()
                .id(1L).ownerId(1L).name("심해의 정화 오일").grade(ElixirGrade.EPIC)
                .themeCategory(ThemeCategory.SKIN_ANTIOXIDANT).imageUrl("url").serialNumber(null)
                .build();
        ElixirCard mutated = ElixirCard.builder()
                .id(2L).ownerId(1L).name("검은 잔영의 변종수").grade(ElixirGrade.RARE)
                .themeCategory(ThemeCategory.SKIN_ANTIOXIDANT).imageUrl("url2").serialNumber(null)
                .isMutated(true)
                .build();
        when(elixirCardRepository.findByOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(mutated, normal));

        List<CodexCardSummary> result = controller.getMutations();

        assertThat(result).containsExactly(
                new CodexCardSummary(2L, "검은 잔영의 변종수", ElixirGrade.RARE, "url2", null, true));
    }

    @Test
    void 본인_카드id면_상세정보를_반환한다() {
        Map<String, Integer> stats = Map.of("피부 투명도", 70, "항산화 방어", 65, "스트레스 차단", 72);
        ElixirCard card = ElixirCard.builder()
                .id(1L).ownerId(1L).name("심해의 정화 오일").grade(ElixirGrade.EPIC)
                .themeCategory(ThemeCategory.SKIN_ANTIOXIDANT).imageUrl("url").serialNumber(null)
                .ingredientSummary("천년삼, 영지버섯").adviserComment("좋은 조합입니다.")
                .stats(stats)
                .build();
        when(elixirCardRepository.findById(1L)).thenReturn(Optional.of(card));

        CodexCardDetailResponse response = controller.getCodexDetail(1L);

        assertThat(response).isEqualTo(new CodexCardDetailResponse(1L, "심해의 정화 오일", ElixirGrade.EPIC, "url",
                null, "천년삼, 영지버섯", "좋은 조합입니다.", stats));
    }

    @Test
    void 존재하지_않는_카드id면_예외를_던진다() {
        when(elixirCardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getCodexDetail(999L))
                .isInstanceOf(CodexValidationException.class)
                .hasMessage("존재하지 않는 카드입니다");
    }

    @Test
    void 다른_유저의_카드를_조회하면_존재하지_않는_카드와_동일한_예외를_던진다() {
        ElixirCard othersCard = ElixirCard.builder()
                .id(5L).ownerId(2L).name("타인의 엘릭서").grade(ElixirGrade.EPIC)
                .themeCategory(ThemeCategory.SKIN_ANTIOXIDANT).imageUrl("url").serialNumber(null)
                .build();
        when(elixirCardRepository.findById(5L)).thenReturn(Optional.of(othersCard));

        assertThatThrownBy(() -> controller.getCodexDetail(5L))
                .isInstanceOf(CodexValidationException.class)
                .hasMessage("존재하지 않는 카드입니다");
    }
}
