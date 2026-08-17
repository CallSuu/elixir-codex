package com.elixircodex.backend.alchemy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlchemyNameServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AlchemyNameService service;

    private NameGenerationRequest sampleRequest() {
        return new NameGenerationRequest(ThemeCategory.SKIN_ANTIOXIDANT, List.of("천년삼", "영지버섯"));
    }

    @Test
    void 정상_응답을_파싱해서_이름과_조언을_반환한다() {
        service = new AlchemyNameService(restTemplate, objectMapper, "test-key");
        String openAiResponse = """
                {"choices":[{"message":{"content":"{\\"name\\": \\"심해의 정화 오일\\", \\"adviserComment\\": \\"좋은 조합입니다.\\"}"}}]}
                """;
        when(restTemplate.postForEntity(eq("https://api.openai.com/v1/chat/completions"),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(openAiResponse));

        NameGenerationResponse response = service.generate(sampleRequest());

        assertThat(response.name()).isEqualTo("심해의 정화 오일");
        assertThat(response.adviserComment()).isEqualTo("좋은 조합입니다.");
    }

    @Test
    void API_호출이_실패하면_전용_예외를_던진다() {
        service = new AlchemyNameService(restTemplate, objectMapper, "test-key");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> service.generate(sampleRequest()))
                .isInstanceOf(AlchemyNameGenerationException.class)
                .hasMessage("엘릭서 이름 생성 요청이 실패했습니다");
    }

    @Test
    @SuppressWarnings("unchecked")
    void isMutated가_true면_시스템_프롬프트에_변종_지시문이_추가된다() {
        service = new AlchemyNameService(restTemplate, objectMapper, "test-key");
        String openAiResponse = """
                {"choices":[{"message":{"content":"{\\"name\\": \\"검은 잔영의 변종수\\", \\"adviserComment\\": \\"기괴한 조합입니다.\\"}"}}]}
                """;
        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(eq("https://api.openai.com/v1/chat/completions"),
                captor.capture(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(openAiResponse));

        service.generateName(ThemeCategory.SKIN_ANTIOXIDANT, List.of("천년삼", "영지버섯"), true);

        List<Map<String, String>> messages = (List<Map<String, String>>) captor.getValue().getBody().get("messages");
        String systemContent = messages.get(0).get("content");
        assertThat(systemContent).contains("돌연변이(변종) 버전");
        assertThat(systemContent).contains("불길하고 낯선 분위기");
    }

    @Test
    void 응답_형식이_JSON이_아니면_전용_예외를_던진다() {
        service = new AlchemyNameService(restTemplate, objectMapper, "test-key");
        String malformed = """
                {"choices":[{"message":{"content":"이건 JSON이 아님"}}]}
                """;
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(malformed));

        assertThatThrownBy(() -> service.generate(sampleRequest()))
                .isInstanceOf(AlchemyNameGenerationException.class)
                .hasMessage("엘릭서 이름 생성 응답을 해석하지 못했습니다");
    }
}
