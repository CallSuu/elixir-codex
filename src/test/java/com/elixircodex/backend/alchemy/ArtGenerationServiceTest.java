package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.stack.Grade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtGenerationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ArtGenerationService service;

    @Test
    void 정상_응답을_파싱해서_데이터URI를_반환한다() {
        service = new ArtGenerationService(restTemplate, objectMapper, "test-key");
        String stabilityResponse = """
                {"artifacts":[{"base64":"BASE64DATA","seed":1234,"finishReason":"SUCCESS"}]}
                """;
        when(restTemplate.postForEntity(
                eq("https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image"),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(stabilityResponse));

        String imageUrl = service.generate(Grade.EPIC, ThemeCategory.SLEEP_REST, "은은한 밤의 안식 포션");

        assertThat(imageUrl).isEqualTo("data:image/png;base64,BASE64DATA");
    }

    @Test
    void API_호출이_실패하면_전용_예외에_원인_메시지가_포함된다() {
        service = new ArtGenerationService(restTemplate, objectMapper, "test-key");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("연결 실패"));

        assertThatThrownBy(() -> service.generate(Grade.EPIC, ThemeCategory.SLEEP_REST, "은은한 밤의 안식 포션"))
                .isInstanceOf(ArtGenerationException.class)
                .hasMessage("실시간 아트 생성 요청이 실패했습니다: 연결 실패");
    }

    @Test
    void 타임아웃이_발생하면_전용_예외에_원인_메시지가_포함된다() {
        service = new ArtGenerationService(restTemplate, objectMapper, "test-key");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> service.generate(Grade.EPIC, ThemeCategory.SLEEP_REST, "은은한 밤의 안식 포션"))
                .isInstanceOf(ArtGenerationException.class)
                .hasMessage("실시간 아트 생성 요청이 실패했습니다: Read timed out");
    }

    @Test
    void HTTP_오류_응답이면_상태코드와_응답본문이_예외_메시지에_포함된다() {
        service = new ArtGenerationService(restTemplate, objectMapper, "test-key");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                        new HttpHeaders(), "{\"error\":\"invalid_api_key\"}".getBytes(StandardCharsets.UTF_8), null));

        assertThatThrownBy(() -> service.generate(Grade.EPIC, ThemeCategory.SLEEP_REST, "은은한 밤의 안식 포션"))
                .isInstanceOf(ArtGenerationException.class)
                .hasMessage("실시간 아트 생성 요청이 실패했습니다: status=500, body={\"error\":\"invalid_api_key\"}");
    }

    @Test
    void 응답_형식이_예상과_다르면_전용_예외를_던진다() {
        service = new ArtGenerationService(restTemplate, objectMapper, "test-key");
        String malformed = """
                {"error":"something went wrong"}
                """;
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(malformed));

        assertThatThrownBy(() -> service.generate(Grade.EPIC, ThemeCategory.SLEEP_REST, "은은한 밤의 안식 포션"))
                .isInstanceOf(ArtGenerationException.class)
                .hasMessage("실시간 아트 생성 응답이 비어 있습니다");
    }
}
