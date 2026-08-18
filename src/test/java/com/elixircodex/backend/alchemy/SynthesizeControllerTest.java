package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import com.elixircodex.backend.stack.StackValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SynthesizeControllerTest {

    @Mock
    private SynthesizeService synthesizeService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    private SynthesizeController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new SynthesizeController(synthesizeService, authenticatedUserService);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
    }

    private SynthesizeRequest request() {
        return new SynthesizeRequest(List.of(10L, 11L), ThemeCategory.SKIN_ANTIOXIDANT);
    }

    @Test
    void 스택_검증_실패_예외는_그대로_전파된다() {
        when(synthesizeService.synthesize(1L, request())).thenThrow(new StackValidationException("미인증된 섭취 기록입니다"));

        assertThatThrownBy(() -> controller.synthesize(request()))
                .isInstanceOf(StackValidationException.class)
                .hasMessage("미인증된 섭취 기록입니다");
    }

    @Test
    void 일일_제한_초과_예외는_그대로_전파된다() {
        when(synthesizeService.synthesize(1L, request())).thenThrow(new SynthesizeValidationException("오늘 이미 연성을 완료했습니다"));

        assertThatThrownBy(() -> controller.synthesize(request()))
                .isInstanceOf(SynthesizeValidationException.class)
                .hasMessage("오늘 이미 연성을 완료했습니다");
    }

    @Test
    void GPT_호출_실패_예외는_그대로_전파된다() {
        when(synthesizeService.synthesize(1L, request()))
                .thenThrow(new AlchemyNameGenerationException("엘릭서 이름 생성 요청이 실패했습니다"));

        assertThatThrownBy(() -> controller.synthesize(request()))
                .isInstanceOf(AlchemyNameGenerationException.class)
                .hasMessage("엘릭서 이름 생성 요청이 실패했습니다");
    }

    @Test
    void 정상_요청이면_결과를_그대로_반환한다() {
        Map<String, Integer> stats = Map.of("피부 투명도", 72, "항산화 방어", 65, "스트레스 차단", 80);
        SynthesizeResponse expected = new SynthesizeResponse(1L, "심해의 정화 오일", ElixirGrade.EPIC, "url", "조언", null,
                stats, null, null);
        when(synthesizeService.synthesize(1L, request())).thenReturn(expected);

        ResponseEntity<?> response = controller.synthesize(request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        assertThat(((SynthesizeResponse) response.getBody()).stats()).isEqualTo(stats);
    }
}
