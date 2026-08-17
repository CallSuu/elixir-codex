package com.elixircodex.backend.stack;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StackControllerTest {

    @Mock
    private StackService stackService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    private StackController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new StackController(stackService, authenticatedUserService);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void 검증_실패_예외는_그대로_전파된다() {
        StackRequest request = new StackRequest(List.of(10L));
        when(stackService.evaluate(1L, request)).thenThrow(new StackValidationException("오늘 인증된 영양제가 없습니다"));

        assertThatThrownBy(() -> controller.stack(request))
                .isInstanceOf(StackValidationException.class)
                .hasMessage("오늘 인증된 영양제가 없습니다");
    }

    @Test
    void 정상_요청이면_점수합산과_제휴여부와_자동편입_영양제목록을_반환한다() {
        IngredientCard epic = IngredientCard.builder().id(10L).name("천년삼").grade(Grade.EPIC).build();
        IngredientCard common = IngredientCard.builder().id(11L).name("들꽃").grade(Grade.COMMON).build();
        StackRequest request = new StackRequest(List.of(10L, 11L));
        when(stackService.evaluate(1L, request))
                .thenReturn(new StackEvaluation(List.of(epic, common), 8, true, List.of("종합비타민")));

        ResponseEntity<?> response = controller.stack(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StackResponse body = (StackResponse) response.getBody();
        assertThat(body.totalScore()).isEqualTo(8);
        assertThat(body.affiliateBoost()).isTrue();
        assertThat(body.canSynthesize()).isTrue();
        assertThat(body.ingredientCards()).hasSize(2);
        assertThat(body.includedSupplements()).containsExactly("종합비타민");
    }
}
