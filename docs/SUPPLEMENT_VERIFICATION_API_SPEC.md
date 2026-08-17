# 영양제 인증 사진 업로드 API 명세서

- 문서 버전: 2.1
- 작성일: 2026-08-15
- 구현 기준 브랜치: develop
- 적용 범위: `POST /api/supplements/verify` (영양제 인증 사진을 GPT-4o 비전으로 분석해 SupplementLog로 저장), `GET /api/supplements` (내 인증 기록 목록 조회)

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`POST /api/supplements/verify`는 `multipart/form-data`, `GET /api/supplements`는 쿼리 파라미터만 사용. 응답은 둘 다 `application/json`.

### 1.3 인증 규칙

`Authorization: Bearer <JWT>` 헤더가 필수다. 토큰이 없거나 무효하면 `401 { "message": "인증이 필요합니다" }`. `ownerId`는 더 이상 요청에 포함하지 않으며, 토큰의 이메일로 서버가 직접 조회한다 (`AuthenticatedUserService.getCurrentUserId()`). 토큰은 유효한데 매칭되는 User가 없으면 `401 { "message": "인증된 사용자를 찾을 수 없습니다" }`.

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

`com.elixircodex.backend.common.GlobalExceptionHandler`(`@RestControllerAdvice`)에서 전역으로 처리한다. `SupplementVerificationService`가 던지는 커스텀 예외뿐 아니라, `image` 폼 파트가 아예 누락된 경우(`MissingServletRequestPartException`)처럼 컨트롤러 이전 단계(Spring)에서 발생하는 오류도 모두 동일한 형식으로 통일된다. 처리되지 않은 예외는 `500 { "message": "서버 오류가 발생했습니다" }`로 응답한다.

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 영양제 인증 사진 업로드 | POST | `/api/supplements/verify` | 200 OK |
| 내 인증 기록 목록 조회 | GET | `/api/supplements` | 200 OK |

## 3. POST /api/supplements/verify

### 요청

`POST /api/supplements/verify` (multipart/form-data, `ownerId`는 JWT에서 추출하므로 폼 필드로 보내지 않음)

| 필드 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| image | File | Y | 폼 파트 자체가 없으면 400. 최대 10MB(`spring.servlet.multipart.max-file-size`), 초과하면 400. 내용이 비어 있는(0바이트) 파일이거나 읽을 수 없으면 400(아래 오류 응답 참고) |

### 처리 로직 (SupplementVerificationService.verify() 구현 기준)

1. 이미지가 없거나(폼 파트 누락) 비어 있으면 400. 이미지 바이트를 읽지 못해도(`IOException`) 400
2. 이미지를 base64로 인코딩해 `data:{contentType};base64,{...}` 형태의 data URI로 만든다
3. OpenAI Chat Completions API를 `gpt-4o` 모델로 호출한다. 이미지는 `image_url` 콘텐츠 파트(위 data URI)로 전달하고, `response_format: json_object`로 응답 형식을 강제한다. 시스템 프롬프트는 제품명과 0~100 신뢰도 점수를 JSON으로만 답하도록 지시한다. 이 호출이나 응답 파싱이 실패하면 502(아래 오류 응답 참고)
4. `confidenceScore`가 `supplement.ocr.confidence-threshold`(기본 70) 이상이면 `isVerified=true`, 미만이면 `isVerified=false`. 미만이어도 예외를 던지지 않고 미인증 상태로 계속 진행한다
5. 인식된 `productName`에 `supplement.affiliate-products`(콤마 구분, 기본값 `제휴브랜드A,제휴브랜드B`) 목록 중 하나라도 대소문자 무시하고 포함되어 있으면 `isAffiliateProduct=true`
6. `ownerId`, `productName`, `confidenceScore`, `isAffiliateProduct`, `isVerified`, `consumedDate`(오늘)로 SupplementLog를 저장하고 응답을 반환한다

### 성공 응답

`200 OK`

인증된 경우:

```json
{
  "supplementLogId": 1,
  "productName": "오메가3",
  "confidenceScore": 85,
  "isVerified": true,
  "isAffiliateProduct": false
}
```

신뢰도가 임계값 미만이라 미인증인 경우도 정상 응답이다 (에러 아님):

```json
{
  "supplementLogId": 2,
  "productName": "정체불명 가루",
  "confidenceScore": 40,
  "isVerified": false,
  "isAffiliateProduct": false
}
```

### 오류 응답

이미지 자체가 문제인 경우(비어 있음/없음/읽기 실패)는 400, GPT 호출·파싱이 실패한 경우만 502로 분리한다.

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 400 | `image` 폼 파트 자체가 누락됨 (`MissingServletRequestPartException`) — 실제 호출로 확인 | `{ "message": "필수 파라미터 'image'이(가) 누락되었습니다" }` |
| 400 | 업로드 파일(또는 요청 전체)이 10MB를 초과함 (`MaxUploadSizeExceededException`) — 실제 호출로 확인 | `{ "message": "이미지 파일 크기가 너무 큽니다(최대 10MB)" }` |
| 400 | 첨부된 이미지 내용이 비어 있음(0바이트) 또는 `image`가 null | `{ "message": "이미지가 필요합니다" }` |
| 400 | 이미지 바이트를 읽는 도중 오류(`IOException`) | `{ "message": "이미지를 읽을 수 없습니다" }` |
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |
| 502 | OpenAI API 호출 실패 (타임아웃, 네트워크 오류, 4xx/5xx 등) — 실제 호출로 확인 | `{ "message": "영양제 인증 요청이 실패했습니다" }` |
| 502 | OpenAI 응답의 `choices[0].message.content`가 비어 있음 | `{ "message": "영양제 인증 응답이 비어 있습니다" }` |
| 502 | `content`를 JSON으로 파싱하지 못함 | `{ "message": "영양제 인증 응답을 해석하지 못했습니다" }` |

## 4. GET /api/supplements

### 요청

`GET /api/supplements` (파라미터 없음, `ownerId`는 JWT에서 추출)

### 처리 로직 (SupplementVerificationService.getLogs() 구현 기준)

`SupplementLogRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)` 결과를 최신순으로 반환한다. 각 항목은 3절의 성공 응답과 동일한 형태(`SupplementVerifyResponse`)다. `confidenceScore`가 저장되지 않은(이 필드 도입 이전에 만들어진) 기록은 0으로 채워서 반환한다.

### 성공 응답

`200 OK`

```json
[
  { "supplementLogId": 2, "productName": "정체불명 가루", "confidenceScore": 40, "isVerified": false, "isAffiliateProduct": false },
  { "supplementLogId": 1, "productName": "오메가3", "confidenceScore": 85, "isVerified": true, "isAffiliateProduct": false }
]
```

기록이 없으면 빈 배열.

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |

## 5. 데이터 구조

### SupplementVerifyResponse

| 필드 | 타입 | 설명 |
|---|---|---|
| supplementLogId | Long | 저장된 SupplementLog의 id |
| productName | String | GPT-4o가 인식한 제품명 |
| confidenceScore | int | GPT-4o가 평가한 신뢰도 (0~100). 저장된 값이 없는 옛 기록은 0 |
| isVerified | boolean | `confidenceScore >= supplement.ocr.confidence-threshold` 여부 |
| isAffiliateProduct | boolean | `productName`에 제휴 브랜드가 포함됐는지 여부 |

### 관련 설정 (application.properties)

| 키 | 기본값 | 설명 |
|---|---|---|
| `supplement.ocr.confidence-threshold` | 70 | 이 값 이상이면 인증(`isVerified=true`)으로 판정 |
| `supplement.affiliate-products` | `제휴브랜드A,제휴브랜드B` | 콤마로 구분된 제휴 브랜드 목록. `productName`에 포함(대소문자 무시)되면 제휴 상품으로 판정 |

이 API로 저장/조회되는 SupplementLog 엔티티 자체의 필드 구조(`confidenceScore` 포함)는 `STACK_API_SPEC.md`를 참고. `develop` 통합 이후 팀원(backend-2)의 영양제 인증 기능(`SUPPLEMENT_API_SPEC.md` 참고)과 이 엔티티·엔드포인트를 공유한다.

## 6. 현재 범위 밖 기능

- 이미지 파일 형식/용량 사전 검증 (현재는 GPT-4o 호출 결과로만 판단)
- 자동 테스트는 GPT 응답을 모킹한 것이며, 실제 사진을 이용한 GPT-4o 인식 정확도 검증은 하지 않음 (실제 이미지로 직접 호출해서 확인 필요)
- 동일 영양제 사진 중복 업로드 방지 (현재는 매 호출마다 새 SupplementLog를 생성)
