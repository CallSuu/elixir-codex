package com.elixircodex.backend.onboarding;

import com.elixircodex.backend.alchemy.ThemeCategory;
import com.elixircodex.backend.auth.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {

    @Mock
    private OnboardingClassificationService onboardingClassificationService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    private OnboardingController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new OnboardingController(onboardingClassificationService, authenticatedUserService);
    }

    @Test
    void 정상_요청이면_결과를_그대로_반환한다() {
        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        OnboardingClassifyRequest request = new OnboardingClassifyRequest("잠을 잘 못 자요");
        OnboardingClassifyResponse expected = new OnboardingClassifyResponse(ThemeCategory.SLEEP_REST, "수면/휴식");
        when(onboardingClassificationService.classifyAndUpdateUser(1L, "잠을 잘 못 자요")).thenReturn(expected);

        OnboardingClassifyResponse response = controller.classify(request);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void 존재하지_않는_유저면_예외가_그대로_전파된다() {
        when(authenticatedUserService.getCurrentUserId()).thenReturn(999L);
        OnboardingClassifyRequest request = new OnboardingClassifyRequest("잠을 잘 못 자요");
        when(onboardingClassificationService.classifyAndUpdateUser(999L, "잠을 잘 못 자요"))
                .thenThrow(new OnboardingValidationException("존재하지 않는 유저입니다"));

        assertThatThrownBy(() -> controller.classify(request))
                .isInstanceOf(OnboardingValidationException.class)
                .hasMessage("존재하지 않는 유저입니다");
    }
}
