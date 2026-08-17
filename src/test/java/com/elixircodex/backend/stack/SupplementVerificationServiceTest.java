package com.elixircodex.backend.stack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplementVerificationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SupplementLogRepository supplementLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> affiliateProducts = List.of("제휴브랜드A", "제휴브랜드B");

    private SupplementVerificationService service(int confidenceThreshold) {
        return new SupplementVerificationService(restTemplate, objectMapper, "test-key",
                confidenceThreshold, affiliateProducts, supplementLogRepository);
    }

    private MultipartFile fakeImage() {
        return new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private void givenGptResponds(String productName, int confidenceScore) {
        String responseJson = """
                {"choices":[{"message":{"content":"{\\"productName\\": \\"%s\\", \\"confidenceScore\\": %d}"}}]}
                """.formatted(productName, confidenceScore);
        when(restTemplate.postForEntity(eq("https://api.openai.com/v1/chat/completions"),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseJson));
    }

    @Test
    void 신뢰도가_임계값_이상이면_인증된_상태로_저장된다() {
        givenGptResponds("오메가3", 85);
        SupplementVerificationService service = service(70);

        SupplementVerifyResponse response = service.verify(1L, fakeImage());

        assertThat(response.confidenceScore()).isEqualTo(85);
        assertThat(response.isVerified()).isTrue();

        ArgumentCaptor<SupplementLog> captor = ArgumentCaptor.forClass(SupplementLog.class);
        verify(supplementLogRepository).save(captor.capture());
        assertThat(captor.getValue().isVerified()).isTrue();
    }

    @Test
    void 신뢰도가_임계값_미만이면_예외없이_미인증_상태로_저장된다() {
        givenGptResponds("정체불명 가루", 40);
        SupplementVerificationService service = service(70);

        SupplementVerifyResponse response = service.verify(1L, fakeImage());

        assertThat(response.confidenceScore()).isEqualTo(40);
        assertThat(response.isVerified()).isFalse();

        ArgumentCaptor<SupplementLog> captor = ArgumentCaptor.forClass(SupplementLog.class);
        verify(supplementLogRepository).save(captor.capture());
        assertThat(captor.getValue().isVerified()).isFalse();
    }

    @Test
    void 제품명이_제휴목록에_포함되면_제휴상품으로_판정된다() {
        givenGptResponds("제휴브랜드A 오메가3 1000mg", 90);
        SupplementVerificationService service = service(70);

        SupplementVerifyResponse response = service.verify(1L, fakeImage());

        assertThat(response.isAffiliateProduct()).isTrue();
    }

    @Test
    void 제품명이_제휴목록에_없으면_제휴상품이_아니다() {
        givenGptResponds("아무개 브랜드 비타민", 90);
        SupplementVerificationService service = service(70);

        SupplementVerifyResponse response = service.verify(1L, fakeImage());

        assertThat(response.isAffiliateProduct()).isFalse();
    }

    @Test
    void 제휴목록_매칭은_대소문자를_무시한다() {
        givenGptResponds("제휴브랜드b 프로바이오틱스", 90);
        SupplementVerificationService service = service(70);

        SupplementVerifyResponse response = service.verify(1L, fakeImage());

        assertThat(response.isAffiliateProduct()).isTrue();
    }

    @Test
    void 이미지가_비어있으면_400계열_예외를_던지고_GPT를_호출하지_않는다() {
        SupplementVerificationService service = service(70);
        MultipartFile emptyImage = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.verify(1L, emptyImage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지가 필요합니다");

        verifyNoInteractions(restTemplate);
        verifyNoInteractions(supplementLogRepository);
    }

    @Test
    void 이미지가_null이면_400계열_예외를_던지고_GPT를_호출하지_않는다() {
        SupplementVerificationService service = service(70);

        assertThatThrownBy(() -> service.verify(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지가 필요합니다");

        verifyNoInteractions(restTemplate);
        verifyNoInteractions(supplementLogRepository);
    }

    @Test
    void 이미지를_읽을_수_없으면_400계열_예외를_던지고_GPT를_호출하지_않는다() throws Exception {
        SupplementVerificationService service = service(70);
        MultipartFile unreadableImage = org.mockito.Mockito.mock(MultipartFile.class);
        when(unreadableImage.isEmpty()).thenReturn(false);
        when(unreadableImage.getBytes()).thenThrow(new java.io.IOException("disk error"));

        assertThatThrownBy(() -> service.verify(1L, unreadableImage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지를 읽을 수 없습니다");

        verifyNoInteractions(restTemplate);
        verifyNoInteractions(supplementLogRepository);
    }

    @Test
    void 신뢰도_점수가_SupplementLog에_저장된다() {
        givenGptResponds("오메가3", 85);
        SupplementVerificationService service = service(70);

        service.verify(1L, fakeImage());

        ArgumentCaptor<SupplementLog> captor = ArgumentCaptor.forClass(SupplementLog.class);
        verify(supplementLogRepository).save(captor.capture());
        assertThat(captor.getValue().getConfidenceScore()).isEqualTo(85);
    }

    @Test
    void 소유자의_인증기록_목록을_최신순으로_반환한다() {
        SupplementLog older = SupplementLog.builder()
                .id(1L).ownerId(1L).productName("오메가3").confidenceScore(80)
                .isVerified(true).isAffiliateProduct(false).build();
        SupplementLog newer = SupplementLog.builder()
                .id(2L).ownerId(1L).productName("비타민C").confidenceScore(90)
                .isVerified(true).isAffiliateProduct(true).build();
        when(supplementLogRepository.findByOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(newer, older));

        List<SupplementVerifyResponse> logs = service(70).getLogs(1L);

        assertThat(logs).extracting(SupplementVerifyResponse::supplementLogId).containsExactly(2L, 1L);
    }

    @Test
    void 신뢰도가_저장되지_않은_기록도_목록조회시_예외없이_0으로_반환된다() {
        SupplementLog legacyLog = SupplementLog.builder()
                .id(1L).ownerId(1L).productName("옛날 기록").confidenceScore(null)
                .isVerified(false).isAffiliateProduct(false).build();
        when(supplementLogRepository.findByOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(legacyLog));

        List<SupplementVerifyResponse> logs = service(70).getLogs(1L);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).confidenceScore()).isEqualTo(0);
    }

    @Test
    void GPT_호출이_실패하면_전용_예외를_던진다() {
        SupplementVerificationService service = service(70);
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> service.verify(1L, fakeImage()))
                .isInstanceOf(SupplementVerificationException.class)
                .hasMessage("영양제 인증 요청이 실패했습니다");
    }

    @Test
    void GPT_응답이_JSON이_아니면_전용_예외를_던진다() {
        SupplementVerificationService service = service(70);
        String malformed = """
                {"choices":[{"message":{"content":"이건 JSON이 아님"}}]}
                """;
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(malformed));

        assertThatThrownBy(() -> service.verify(1L, fakeImage()))
                .isInstanceOf(SupplementVerificationException.class)
                .hasMessage("영양제 인증 응답을 해석하지 못했습니다");
    }
}
