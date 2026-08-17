package com.elixircodex.backend.stack;

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
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SupplementVerificationService {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o";
    private static final String SYSTEM_PROMPT = """
            영양제/보충제 제품 사진을 보고 제품명을 읽어내고, 실제 영양제/보충제 제품 사진처럼 보이는지
            신뢰도를 0~100 점수로 평가해. JSON으로 {"productName": "...", "confidenceScore": 0-100} 형식으로만 답해.
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final int confidenceThreshold;
    private final List<String> affiliateProducts;
    private final SupplementLogRepository supplementLogRepository;

    @Autowired
    public SupplementVerificationService(ObjectMapper objectMapper,
                                          @Value("${openai.api.key}") String apiKey,
                                          @Value("${supplement.ocr.confidence-threshold}") int confidenceThreshold,
                                          @Value("${supplement.affiliate-products}") List<String> affiliateProducts,
                                          SupplementLogRepository supplementLogRepository) {
        this(newRestTemplate(), objectMapper, apiKey, confidenceThreshold, affiliateProducts, supplementLogRepository);
    }

    SupplementVerificationService(RestTemplate restTemplate, ObjectMapper objectMapper, String apiKey,
                                   int confidenceThreshold, List<String> affiliateProducts,
                                   SupplementLogRepository supplementLogRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.confidenceThreshold = confidenceThreshold;
        this.affiliateProducts = affiliateProducts;
        this.supplementLogRepository = supplementLogRepository;
    }

    private static RestTemplate newRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return new RestTemplate(factory);
    }

    public SupplementVerifyResponse verify(Long ownerId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("이미지가 필요합니다");
        }

        String dataUri = toDataUri(image);

        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", "이 사진 속 영양제/보충제 제품명을 읽고 신뢰도를 평가하라."),
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUri))
                        ))
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
            throw new SupplementVerificationException("영양제 인증 요청이 실패했습니다", e);
        }

        GptVisionResult result = parse(responseBody);
        String productName = Objects.requireNonNullElse(result.productName(), "");

        boolean isVerified = result.confidenceScore() >= confidenceThreshold;
        boolean isAffiliateProduct = affiliateProducts.stream()
                .anyMatch(brand -> productName.toLowerCase().contains(brand.toLowerCase()));

        SupplementLog log = SupplementLog.builder()
                .ownerId(ownerId)
                .productName(productName)
                .confidenceScore(result.confidenceScore())
                .isAffiliateProduct(isAffiliateProduct)
                .isVerified(isVerified)
                .consumedDate(LocalDate.now())
                .build();
        supplementLogRepository.save(log);

        return toResponse(log);
    }

    public List<SupplementVerifyResponse> getLogs(Long ownerId) {
        return supplementLogRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private SupplementVerifyResponse toResponse(SupplementLog log) {
        int confidenceScore = Objects.requireNonNullElse(log.getConfidenceScore(), 0);
        return new SupplementVerifyResponse(log.getId(), log.getProductName(), confidenceScore,
                log.isVerified(), log.isAffiliateProduct());
    }

    private String toDataUri(MultipartFile image) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String contentType = image.getContentType() != null ? image.getContentType() : "image/jpeg";
            return "data:" + contentType + ";base64," + base64Image;
        } catch (IOException e) {
            throw new IllegalArgumentException("이미지를 읽을 수 없습니다", e);
        }
    }

    private GptVisionResult parse(String responseBody) {
        try {
            JsonNode content = objectMapper.readTree(responseBody)
                    .path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asString().isBlank()) {
                throw new SupplementVerificationException("영양제 인증 응답이 비어 있습니다");
            }
            return objectMapper.readValue(content.asString(), GptVisionResult.class);
        } catch (SupplementVerificationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new SupplementVerificationException("영양제 인증 응답을 해석하지 못했습니다", e);
        }
    }

    private record GptVisionResult(String productName, int confidenceScore) {
    }
}
