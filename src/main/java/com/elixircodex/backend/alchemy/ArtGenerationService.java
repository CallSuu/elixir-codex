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
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ArtGenerationService {

    private static final String API_URL =
            "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image";
    private static final String STYLE_SEED =
            "Dark fantasy gothic alchemist style, vintage elixir bottle, high detail illustration";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    @Autowired
    public ArtGenerationService(ObjectMapper objectMapper, @Value("${stability.api.key}") String apiKey) {
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
                "text_prompts", List.of(Map.of("text", buildPrompt(grade, themeCategory, elixirName), "weight", 1)),
                "cfg_scale", 7,
                "height", 1024,
                "width", 1024,
                "samples", 1,
                "steps", 30
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(apiKey);

        String responseBody;
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL,
                    new HttpEntity<>(requestBody, headers), String.class);
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
            JsonNode base64 = objectMapper.readTree(responseBody).path("artifacts").path(0).path("base64");
            if (base64.isMissingNode() || base64.asString().isBlank()) {
                throw new ArtGenerationException("실시간 아트 생성 응답이 비어 있습니다");
            }
            return "data:image/png;base64,%s".formatted(base64.asString());
        } catch (ArtGenerationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ArtGenerationException("실시간 아트 생성 응답을 해석하지 못했습니다", e);
        }
    }
}
