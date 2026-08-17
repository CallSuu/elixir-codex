package com.elixircodex.backend.alchemy;

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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AlchemyNameService {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o";
    private static final String SYSTEM_PROMPT = """
            엘릭서 이름은 [수식어 + 핵심 테마 + 연금술 제형] 조합으로 만든다.
            예: 피부/항산화 테마는 '심해의 정화 오일', '달빛 영원의 에센스' 같은 스타일.
            피로/에너지 테마는 '태양 심장의 정수', '새벽의 활력 포션' 같은 스타일.
            혈당/다이어트 테마는 '칠흑의 당독소 봉인수' 같은 스타일.
            수면/휴식 테마는 '은은한 밤의 안식 포션' 같은 스타일.
            고풍스럽고 신비로운 중세 판타지 톤을 유지한다.
            """;
    private static final String MUTATION_ADDENDUM =
            "이번엔 돌연변이(변종) 버전이야. 이름과 조언 문구에 흑보랏빛 이색 발광, 기괴하고 신비로운 느낌을 담아줘. "
                    + "기존의 우아하고 고풍스러운 톤 대신 불길하고 낯선 분위기로 바꿔줘.";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    @Autowired
    public AlchemyNameService(ObjectMapper objectMapper, @Value("${openai.api.key}") String apiKey) {
        this(newRestTemplate(), objectMapper, apiKey);
    }

    AlchemyNameService(RestTemplate restTemplate, ObjectMapper objectMapper, String apiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    private static RestTemplate newRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return new RestTemplate(factory);
    }

    public NameGenerationResponse generate(NameGenerationRequest request) {
        return generateName(request.themeCategory(), request.ingredientNames(), false);
    }

    public NameGenerationResponse generateName(ThemeCategory themeCategory, List<String> ingredientNames,
                                                boolean isMutated) {
        String systemPrompt = isMutated ? SYSTEM_PROMPT + "\n" + MUTATION_ADDENDUM : SYSTEM_PROMPT;
        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", buildUserMessage(themeCategory, ingredientNames))
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
            throw new AlchemyNameGenerationException("엘릭서 이름 생성 요청이 실패했습니다", e);
        }

        return parse(responseBody);
    }

    private String buildUserMessage(ThemeCategory themeCategory, List<String> ingredientNames) {
        return """
                테마: %s
                투입 재료: %s
                위 테마와 재료에 어울리는 엘릭서 이름 1개와, 2~3문장짜리 연금술사의 시너지 조언 문구를 아래 JSON 형식으로만 응답하라.
                {"name": "...", "adviserComment": "..."}
                """.formatted(themeCategory.labelKo(), String.join(", ", ingredientNames));
    }

    private NameGenerationResponse parse(String responseBody) {
        try {
            JsonNode content = objectMapper.readTree(responseBody)
                    .path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asString().isBlank()) {
                throw new AlchemyNameGenerationException("엘릭서 이름 생성 응답이 비어 있습니다");
            }
            return objectMapper.readValue(content.asString(), NameGenerationResponse.class);
        } catch (AlchemyNameGenerationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AlchemyNameGenerationException("엘릭서 이름 생성 응답을 해석하지 못했습니다", e);
        }
    }
}
