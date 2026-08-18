package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.stack.Grade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ArtGenerationService {

    private static final String API_URL = "https://api.openai.com/v1/images/generations";
    private static final String MODEL = "dall-e-3";
    private static final String STYLE_SEED =
            "Dark fantasy gothic alchemist style, vintage elixir bottle, high detail illustration";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    @Autowired
    public ArtGenerationService(ObjectMapper objectMapper, @Value("${openai.api.key}") String apiKey) {
        this(newRestTemplate(), objectMapper, apiKey);
    }

    ArtGenerationService(RestTemplate restTemplate, ObjectMapper objectMapper, String apiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    private static RestTemplate newRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return new RestTemplate(factory);
    }

    public String generate(Grade grade, ThemeCategory themeCategory, String elixirName) {
        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "prompt", buildPrompt(grade, themeCategory, elixirName)
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
            throw new ArtGenerationException("실시간 아트 생성 요청이 실패했습니다: " + describe(e), e);
        }

        return parse(responseBody);
    }

    private String describe(RestClientException e) {
        if (e instanceof RestClientResponseException responseException) {
            return "status=%d, body=%s".formatted(
                    responseException.getStatusCode().value(), responseException.getResponseBodyAsString());
        }
        return e.getMessage();
    }

    private String buildPrompt(Grade grade, ThemeCategory themeCategory, String elixirName) {
        return "%s, %s, %s theme, elixir named '%s'"
                .formatted(STYLE_SEED, gradeExpression(grade), themeCategory.labelKo(), elixirName);
    }

    private String gradeExpression(Grade grade) {
        return switch (grade) {
            case EPIC -> "ornate, glowing with power";
            case RARE -> "elegant, subtly enchanted";
            case COMMON -> "simple, humble";
        };
    }

    private String parse(String responseBody) {
        try {
            // dall-e-3는 response_format 기본값이 "url"이라 data[0].url로 바로 꺼낼 수 있다
            // (gpt-image-1은 항상 b64_json만 반환해 이 파싱으로는 호환되지 않았음).
            JsonNode url = objectMapper.readTree(responseBody).path("data").path(0).path("url");
            if (url.isMissingNode() || url.asString().isBlank()) {
                throw new ArtGenerationException("실시간 아트 생성 응답이 비어 있습니다");
            }
            return url.asString();
        } catch (ArtGenerationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ArtGenerationException("실시간 아트 생성 응답을 해석하지 못했습니다", e);
        }
    }
}
