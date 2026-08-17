package com.elixircodex.backend.onboarding;

import com.elixircodex.backend.alchemy.ThemeCategory;
import com.example.demo.Entity.User;
import com.example.demo.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OnboardingClassificationService {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o";
    private static final String SYSTEM_PROMPT = """
            유저가 입력한 건강 관련 텍스트를 읽고, 아래 4개 카테고리 중 가장 잘 맞는 하나를 선택해.
            카테고리: 피부/항산화(SKIN_ANTIOXIDANT), 피로/에너지(FATIGUE_ENERGY), 혈당/다이어트(DIET_BLOODSUGAR), 수면/휴식(SLEEP_REST).
            JSON으로 {"category": "영문 코드값 중 하나"} 형식으로만 답해.
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final UserRepository userRepository;

    @Autowired
    public OnboardingClassificationService(ObjectMapper objectMapper,
                                            @Value("${openai.api.key}") String apiKey,
                                            UserRepository userRepository) {
        this(newRestTemplate(), objectMapper, apiKey, userRepository);
    }

    OnboardingClassificationService(RestTemplate restTemplate, ObjectMapper objectMapper, String apiKey,
                                     UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.userRepository = userRepository;
    }

    private static RestTemplate newRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return new RestTemplate(factory);
    }

    /**
     * 순수 분류만 수행한다. User 조회/업데이트는 하지 않는다.
     */
    public ThemeCategory classify(String freeText) {
        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", buildUserMessage(freeText))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String responseBody;
        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(API_URL, new HttpEntity<>(requestBody, headers), String.class);
            responseBody = response.getBody();
        } catch (RestClientException e) {
            throw new OnboardingClassificationException("카테고리 분류 요청이 실패했습니다", e);
        }

        return parseCategory(responseBody);
    }

    /**
     * 분류 후 그 결과로 User.selectedCategory를 덮어쓴다.
     */
    public OnboardingClassifyResponse classifyAndUpdateUser(Long ownerId, String freeText) {
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new OnboardingValidationException("존재하지 않는 유저입니다"));

        ThemeCategory themeCategory = classify(freeText);

        User updated = User.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .selectedCategory(themeCategory.labelKo())
                .subscriptionStatus(user.getSubscriptionStatus())
                .healthDataEnabled(user.isHealthDataEnabled())
                .build();
        userRepository.save(updated);

        return new OnboardingClassifyResponse(themeCategory, themeCategory.labelKo());
    }

    private String buildUserMessage(String freeText) {
        return """
                텍스트: %s
                위 텍스트를 읽고 가장 잘 맞는 카테고리 하나를 아래 JSON 형식으로만 응답하라.
                {"category": "SKIN_ANTIOXIDANT 또는 FATIGUE_ENERGY 또는 DIET_BLOODSUGAR 또는 SLEEP_REST 중 하나"}
                """.formatted(freeText);
    }

    private ThemeCategory parseCategory(String responseBody) {
        JsonNode content;
        try {
            content = objectMapper.readTree(responseBody).path("choices").path(0).path("message").path("content");
        } catch (RuntimeException e) {
            throw new OnboardingClassificationException("카테고리 분류 응답을 해석하지 못했습니다", e);
        }
        if (content.isMissingNode() || content.asString().isBlank()) {
            throw new OnboardingClassificationException("카테고리 분류 응답이 비어 있습니다");
        }

        GptCategoryResult result;
        try {
            result = objectMapper.readValue(content.asString(), GptCategoryResult.class);
        } catch (RuntimeException e) {
            throw new OnboardingClassificationException("카테고리 분류 응답을 해석하지 못했습니다", e);
        }

        try {
            return ThemeCategory.valueOf(result.category());
        } catch (IllegalArgumentException e) {
            throw new OnboardingClassificationException("유효하지 않은 카테고리입니다: " + result.category(), e);
        }
    }

    private record GptCategoryResult(String category) {
    }
}
