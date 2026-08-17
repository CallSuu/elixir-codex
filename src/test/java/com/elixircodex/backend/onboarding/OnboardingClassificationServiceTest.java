package com.elixircodex.backend.onboarding;

import com.elixircodex.backend.alchemy.ThemeCategory;
import com.example.demo.Entity.User;
import com.example.demo.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingClassificationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OnboardingClassificationService service() {
        return new OnboardingClassificationService(restTemplate, objectMapper, "test-key", userRepository);
    }

    private User existingUser() {
        return User.builder()
                .id(1L).email("user@example.com").password("encoded-pw")
                .selectedCategory("피로/에너지").subscriptionStatus("FREE").healthDataEnabled(false)
                .build();
    }

    private void givenGptRespondsWith(String category) {
        String responseJson = """
                {"choices":[{"message":{"content":"{\\"category\\": \\"%s\\"}"}}]}
                """.formatted(category);
        when(restTemplate.postForEntity(eq("https://api.openai.com/v1/chat/completions"),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseJson));
    }

    // --- classify(String): 순수 분류, User와 무관 ---

    @Test
    void classify는_User_조회_없이_카테고리만_반환한다() {
        givenGptRespondsWith("SKIN_ANTIOXIDANT");

        ThemeCategory result = service().classify("요즘 피부가 칙칙해요");

        assertThat(result).isEqualTo(ThemeCategory.SKIN_ANTIOXIDANT);
        verifyNoInteractions(userRepository);
    }

    @Test
    void classify는_GPT_호출이_실패하면_전용_예외를_던지고_User와_무관하다() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> service().classify("아무 텍스트"))
                .isInstanceOf(OnboardingClassificationException.class)
                .hasMessage("카테고리 분류 요청이 실패했습니다");

        verifyNoInteractions(userRepository);
    }

    // --- classifyAndUpdateUser(Long, String): 분류 + User 업데이트 ---

    @Test
    void 피부_항산화로_분류되면_라벨과_함께_반환된다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));
        givenGptRespondsWith("SKIN_ANTIOXIDANT");

        OnboardingClassifyResponse response = service().classifyAndUpdateUser(1L, "요즘 피부가 칙칙해요");

        assertThat(response.themeCategory()).isEqualTo(ThemeCategory.SKIN_ANTIOXIDANT);
        assertThat(response.labelKo()).isEqualTo("피부/항산화");
    }

    @Test
    void 피로_에너지로_분류된다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));
        givenGptRespondsWith("FATIGUE_ENERGY");

        OnboardingClassifyResponse response = service().classifyAndUpdateUser(1L, "항상 피곤해요");

        assertThat(response.themeCategory()).isEqualTo(ThemeCategory.FATIGUE_ENERGY);
        assertThat(response.labelKo()).isEqualTo("피로/에너지");
    }

    @Test
    void 혈당_다이어트로_분류된다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));
        givenGptRespondsWith("DIET_BLOODSUGAR");

        OnboardingClassifyResponse response = service().classifyAndUpdateUser(1L, "다이어트 중이에요");

        assertThat(response.themeCategory()).isEqualTo(ThemeCategory.DIET_BLOODSUGAR);
        assertThat(response.labelKo()).isEqualTo("혈당/다이어트");
    }

    @Test
    void 수면_휴식으로_분류된다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));
        givenGptRespondsWith("SLEEP_REST");

        OnboardingClassifyResponse response = service().classifyAndUpdateUser(1L, "잠을 잘 못 자요");

        assertThat(response.themeCategory()).isEqualTo(ThemeCategory.SLEEP_REST);
        assertThat(response.labelKo()).isEqualTo("수면/휴식");
    }

    @Test
    void 분류_결과로_User의_selectedCategory가_업데이트된다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));
        givenGptRespondsWith("SLEEP_REST");

        service().classifyAndUpdateUser(1L, "잠을 잘 못 자요");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getSelectedCategory()).isEqualTo("수면/휴식");
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void 같은_유저를_두번_호출하면_두번째_결과로_다시_덮어써진다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));

        givenGptRespondsWith("SLEEP_REST");
        OnboardingClassifyResponse first = service().classifyAndUpdateUser(1L, "잠을 잘 못 자요");
        assertThat(first.themeCategory()).isEqualTo(ThemeCategory.SLEEP_REST);

        givenGptRespondsWith("DIET_BLOODSUGAR");
        OnboardingClassifyResponse second = service().classifyAndUpdateUser(1L, "다이어트 중이에요");
        assertThat(second.themeCategory()).isEqualTo(ThemeCategory.DIET_BLOODSUGAR);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getSelectedCategory()).isEqualTo("수면/휴식");
        assertThat(captor.getAllValues().get(1).getSelectedCategory()).isEqualTo("혈당/다이어트");
    }

    @Test
    void 존재하지_않는_유저면_GPT호출없이_예외를_던진다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().classifyAndUpdateUser(999L, "아무 텍스트"))
                .isInstanceOf(OnboardingValidationException.class)
                .hasMessage("존재하지 않는 유저입니다");

        verifyNoInteractions(restTemplate);
    }

    @Test
    void 잘못된_카테고리_값이_오면_예외를_던진다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));
        givenGptRespondsWith("UNKNOWN_CATEGORY");

        assertThatThrownBy(() -> service().classifyAndUpdateUser(1L, "아무 텍스트"))
                .isInstanceOf(OnboardingClassificationException.class)
                .hasMessageContaining("유효하지 않은 카테고리입니다");
    }

    @Test
    void GPT_호출이_실패하면_전용_예외를_던진다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> service().classifyAndUpdateUser(1L, "아무 텍스트"))
                .isInstanceOf(OnboardingClassificationException.class)
                .hasMessage("카테고리 분류 요청이 실패했습니다");
    }

    @Test
    void GPT_응답이_JSON이_아니면_전용_예외를_던진다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));
        String malformed = """
                {"choices":[{"message":{"content":"이건 JSON이 아님"}}]}
                """;
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(malformed));

        assertThatThrownBy(() -> service().classifyAndUpdateUser(1L, "아무 텍스트"))
                .isInstanceOf(OnboardingClassificationException.class)
                .hasMessage("카테고리 분류 응답을 해석하지 못했습니다");
    }
}
