package com.elixircodex.backend.specialelixir;

import com.elixircodex.backend.alchemy.ThemeCategory;
import com.elixircodex.backend.auth.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialElixirControllerTest {

    @Mock
    private SpecialElixirService specialElixirService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    private SpecialElixirController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new SpecialElixirController(specialElixirService, authenticatedUserService);
    }

    @Test
    void 생성_요청은_서비스_결과를_그대로_반환한다() {
        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        SpecialElixirCreateRequest request = new SpecialElixirCreateRequest("잠을 잘 못 자요");
        SpecialElixirResponse expected = new SpecialElixirResponse(1L, "은은한 밤의 안식 포션", "url", "조언",
                ThemeCategory.SLEEP_REST, LocalDateTime.now());
        when(specialElixirService.create(1L, "잠을 잘 못 자요")).thenReturn(expected);

        SpecialElixirResponse response = controller.create(request);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void 목록_조회는_서비스_결과를_그대로_반환한다() {
        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        SpecialElixirResponse item = new SpecialElixirResponse(1L, "이름", "url", "조언",
                ThemeCategory.SLEEP_REST, LocalDateTime.now());
        when(specialElixirService.list(1L)).thenReturn(List.of(item));

        List<SpecialElixirResponse> response = controller.list();

        assertThat(response).containsExactly(item);
    }

    @Test
    void 삭제_요청은_서비스로_그대로_위임된다() {
        when(authenticatedUserService.getCurrentUserId()).thenReturn(2L);

        controller.delete(1L);

        verify(specialElixirService).delete(2L, 1L);
    }
}
