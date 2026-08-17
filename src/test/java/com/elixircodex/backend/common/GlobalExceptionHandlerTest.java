package com.elixircodex.backend.common;

import com.elixircodex.backend.alchemy.AlchemyController;
import com.elixircodex.backend.alchemy.AlchemyArtController;
import com.elixircodex.backend.alchemy.AlchemyNameGenerationException;
import com.elixircodex.backend.alchemy.AlchemyNameService;
import com.elixircodex.backend.alchemy.ArtMatchingService;
import com.elixircodex.backend.alchemy.CodexController;
import com.elixircodex.backend.alchemy.ElixirCardRepository;
import com.elixircodex.backend.alchemy.SynthesizeController;
import com.elixircodex.backend.alchemy.SynthesizeService;
import com.elixircodex.backend.alchemy.SynthesizeValidationException;
import com.elixircodex.backend.attendance.AttendanceController;
import com.elixircodex.backend.attendance.AttendanceService;
import com.elixircodex.backend.attendance.AttendanceValidationException;
import com.elixircodex.backend.auth.AuthenticatedUserException;
import com.elixircodex.backend.auth.AuthenticatedUserService;
import com.elixircodex.backend.onboarding.OnboardingClassificationException;
import com.elixircodex.backend.onboarding.OnboardingClassificationService;
import com.elixircodex.backend.onboarding.OnboardingController;
import com.elixircodex.backend.onboarding.OnboardingValidationException;
import com.elixircodex.backend.specialelixir.SpecialElixirController;
import com.elixircodex.backend.specialelixir.SpecialElixirService;
import com.elixircodex.backend.specialelixir.SpecialElixirValidationException;
import com.elixircodex.backend.stack.StackController;
import com.elixircodex.backend.stack.StackService;
import com.elixircodex.backend.stack.StackValidationException;
import com.elixircodex.backend.stack.SupplementController;
import com.elixircodex.backend.stack.SupplementVerificationException;
import com.elixircodex.backend.stack.SupplementVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        StackController.class,
        AlchemyController.class,
        AlchemyArtController.class,
        SynthesizeController.class,
        CodexController.class,
        AttendanceController.class,
        SupplementController.class,
        OnboardingController.class,
        SpecialElixirController.class
})
@ContextConfiguration(classes = GlobalExceptionHandlerTest.TestConfig.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    // @WebMvcTest는 @SpringBootConfiguration을 테스트 클래스 패키지의 상위로만 탐색하는데,
    // com.elixircodex.backend에는 그런 클래스가 없다(메인 클래스는 com.example.demo.DemoApplication 하나뿐).
    // DemoApplication을 직접 가리키면 거기 붙은 @EnableJpaRepositories/@EntityScan/SecurityConfig까지
    // 전부 같이 로드되어 슬라이스 테스트 취지가 깨지므로, 이 테스트 전용의 최소 구성을 따로 둔다.
    // 이 클래스가 com.elixircodex.backend.common에 중첩돼 있어 기본 스캔 범위가 그 이하로 한정되므로,
    // stack/alchemy/attendance 같은 형제 패키지의 컨트롤러도 찾도록 basePackages를 명시한다.
    // CommandLineRunner(더미 데이터 러너)는 리포지토리/DB 없이는 생성할 수 없고 이 슬라이스 테스트와
    // 무관하므로 스캔에서 제외한다.
    @SpringBootConfiguration
    @ComponentScan(basePackages = "com.elixircodex.backend",
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CommandLineRunner.class))
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StackService stackService;
    @MockitoBean
    private AlchemyNameService alchemyNameService;
    @MockitoBean
    private ArtMatchingService artMatchingService;
    @MockitoBean
    private SynthesizeService synthesizeService;
    @MockitoBean
    private ElixirCardRepository elixirCardRepository;
    @MockitoBean
    private AttendanceService attendanceService;
    @MockitoBean
    private SupplementVerificationService supplementVerificationService;
    @MockitoBean
    private OnboardingClassificationService onboardingClassificationService;
    @MockitoBean
    private SpecialElixirService specialElixirService;
    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void StackValidationException은_400과_메시지로_변환된다() throws Exception {
        when(stackService.evaluate(any(), any())).thenThrow(new StackValidationException("오늘 인증된 영양제가 없습니다"));

        mockMvc.perform(post("/api/stack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientCardIds\":[1]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("오늘 인증된 영양제가 없습니다"));
    }

    @Test
    void SynthesizeValidationException은_400과_메시지로_변환된다() throws Exception {
        when(synthesizeService.synthesize(any(), any())).thenThrow(new SynthesizeValidationException("오늘 이미 연성을 완료했습니다"));

        mockMvc.perform(post("/api/synthesize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientCardIds\":[1],\"themeCategory\":\"SKIN_ANTIOXIDANT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("오늘 이미 연성을 완료했습니다"));
    }

    @Test
    void AttendanceValidationException은_400과_메시지로_변환된다() throws Exception {
        when(attendanceService.checkIn(1L)).thenThrow(new AttendanceValidationException("오늘 이미 출석체크를 완료했습니다"));

        mockMvc.perform(post("/api/attendance/check"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("오늘 이미 출석체크를 완료했습니다"));
    }

    @Test
    void AuthenticatedUserException은_401과_메시지로_변환된다() throws Exception {
        when(authenticatedUserService.getCurrentUserId())
                .thenThrow(new AuthenticatedUserException("인증된 사용자를 찾을 수 없습니다"));

        mockMvc.perform(post("/api/attendance/check"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("인증된 사용자를 찾을 수 없습니다"));
    }

    @Test
    void AlchemyNameGenerationException은_502와_메시지로_변환된다() throws Exception {
        when(alchemyNameService.generate(any())).thenThrow(new AlchemyNameGenerationException("엘릭서 이름 생성 요청이 실패했습니다"));

        mockMvc.perform(post("/api/alchemy/generate-name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"themeCategory\":\"SKIN_ANTIOXIDANT\",\"ingredientNames\":[\"천년삼\",\"영지버섯\"]}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("엘릭서 이름 생성 요청이 실패했습니다"));
    }

    @Test
    void SupplementVerificationException은_502와_메시지로_변환된다() throws Exception {
        when(supplementVerificationService.verify(any(), any()))
                .thenThrow(new SupplementVerificationException("영양제 인증 요청이 실패했습니다"));

        org.springframework.mock.web.MockMultipartFile image =
                new org.springframework.mock.web.MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/supplements/verify").file(image))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("영양제 인증 요청이 실패했습니다"));
    }

    @Test
    void CodexValidationException은_400과_메시지로_변환된다() throws Exception {
        when(elixirCardRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/codex/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 카드입니다"));
    }

    @Test
    void OnboardingValidationException은_400과_메시지로_변환된다() throws Exception {
        when(authenticatedUserService.getCurrentUserId()).thenReturn(999L);
        when(onboardingClassificationService.classifyAndUpdateUser(any(), any()))
                .thenThrow(new OnboardingValidationException("존재하지 않는 유저입니다"));

        mockMvc.perform(post("/api/onboarding/classify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"freeText\":\"아무 텍스트\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 유저입니다"));
    }

    @Test
    void OnboardingClassificationException은_502와_메시지로_변환된다() throws Exception {
        when(onboardingClassificationService.classifyAndUpdateUser(any(), any()))
                .thenThrow(new OnboardingClassificationException("카테고리 분류 요청이 실패했습니다"));

        mockMvc.perform(post("/api/onboarding/classify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"freeText\":\"아무 텍스트\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("카테고리 분류 요청이 실패했습니다"));
    }

    @Test
    void SpecialElixirValidationException은_400과_메시지로_변환된다() throws Exception {
        when(specialElixirService.create(1L, "아무 텍스트"))
                .thenThrow(new SpecialElixirValidationException(
                        "스페셜 엘릭서는 최대 3개까지 저장할 수 있습니다. 기존 엘릭서를 삭제한 뒤 다시 시도해주세요."));

        mockMvc.perform(post("/api/special-elixirs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"freeText\":\"아무 텍스트\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("스페셜 엘릭서는 최대 3개까지 저장할 수 있습니다. 기존 엘릭서를 삭제한 뒤 다시 시도해주세요."));
    }

    @Test
    void 잘못된_enum_쿼리파라미터는_400과_메시지로_변환된다() throws Exception {
        mockMvc.perform(get("/api/alchemy/art").param("grade", "LEGENDARY").param("themeCategory", "SKIN_ANTIOXIDANT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("파라미터 'grade'의 값이 올바르지 않습니다"));
    }

    @Test
    void 필수_쿼리파라미터_누락은_400과_메시지로_변환된다() throws Exception {
        mockMvc.perform(get("/api/alchemy/art").param("themeCategory", "SKIN_ANTIOXIDANT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("필수 파라미터 'grade'이(가) 누락되었습니다"));
    }

    @Test
    void 멀티파트_요청에서_필수_파트_누락은_400과_메시지로_변환된다() throws Exception {
        mockMvc.perform(multipart("/api/supplements/verify"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("필수 파라미터 'image'이(가) 누락되었습니다"));
    }

    @Test
    void 잘못된_JSON_요청본문은_400과_메시지로_변환된다() throws Exception {
        mockMvc.perform(post("/api/stack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("이건 JSON이 아님"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다"));
    }

    @Test
    void 예상하지_못한_예외는_500과_공통메시지로_변환된다() throws Exception {
        when(stackService.evaluate(any(), any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/stack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientCardIds\":[1]}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다"));
    }

    @Test
    void MaxUploadSizeExceededException은_400과_메시지로_변환된다() {
        // MockMvc는 실제 서블릿 컨테이너를 통하지 않아 업로드 크기 제한이 실제로 걸리지 않으므로,
        // MethodArgumentNotValidException과 동일하게 핸들러 메서드를 직접 호출해 검증한다.
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10 * 1024 * 1024L);

        ResponseEntity<Map<String, String>> response = handler.handleMaxUploadSizeExceeded(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "이미지 파일 크기가 너무 큽니다(최대 10MB)");
    }

    @Test
    void MethodArgumentNotValidException은_400과_공통메시지로_변환된다() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);

        ResponseEntity<Map<String, String>> response = handler.handleMethodArgumentNotValid(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "요청 값이 유효하지 않습니다");
    }
}
