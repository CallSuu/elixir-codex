# 스택 조합(가마솥 투입) API 명세서

- 문서 버전: 2.2
- 작성일: 2026-08-15
- 구현 기준 브랜치: develop
- 적용 범위: `POST /api/stack` (가마솥에 재료를 투입하면, 당일 인증된 영양제를 서버가 자동으로 조회해 함께 편입하고 등급 점수를 합산하는 기능)

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`Content-Type: application/json`

### 1.3 인증 규칙

`Authorization: Bearer <JWT>` 헤더가 필수다. 토큰이 없거나 무효하면 `401 { "message": "인증이 필요합니다" }`. `ownerId`는 더 이상 요청에 포함하지 않으며, 토큰의 이메일(subject)로 `com.example.demo.Repository.UserRepository.findByEmail()`을 조회해 서버가 직접 얻는다 (`AuthenticatedUserService.getCurrentUserId()`). 토큰은 유효한데 그 이메일에 매칭되는 User가 없으면 `401 { "message": "인증된 사용자를 찾을 수 없습니다" }`.

### 1.4 공통 오류 응답 형식

검증 실패 시 `400 Bad Request`와 함께 아래 형식의 JSON을 반환한다.

```json
{ "message": "오류 메시지" }
```

이 형식은 `com.elixircodex.backend.common.GlobalExceptionHandler`(`@RestControllerAdvice`)에서 전역으로 일괄 처리된다. 컨트롤러/서비스가 던지는 커스텀 예외뿐 아니라 Spring이 요청 처리 전에 던지는 예외(잘못된 JSON 본문, 파라미터 누락, 타입 불일치 등)도 모두 이 형식으로 통일되며, 처리되지 않은 예외는 `500 { "message": "서버 오류가 발생했습니다" }`로 응답한다.

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 스택 조합(가마솥 투입) | POST | `/api/stack` | 200 OK |

## 3. POST /api/stack

### 요청

`POST /api/stack`

```json
{
  "ingredientCardIds": [58, 59, 60]
}
```

| 필드 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| ingredientCardIds | List\<Long\> | Y | 투입할 IngredientCard id 목록 |

`ownerId`/`supplementLogId`는 더 이상 요청에 포함하지 않는다. `ownerId`는 JWT에서, 어떤 영양제를 편입할지는 서버가 그 `ownerId` 기준으로 자동 조회한다 (아래 처리 로직 참고).

### 처리 로직 (StackController가 AuthenticatedUserService로 얻은 ownerId를 StackService.evaluate(ownerId, request)에 넘기는 구현 기준)

1. `ownerId` 기준으로 `consumedDate`가 오늘이고 `isVerified`가 true인 SupplementLog를 전부 조회한다 (`SupplementLogRepository.findByOwnerIdAndConsumedDateAndIsVerifiedTrue`). 조회 결과가 비어 있으면 400
2. `ingredientCardIds`로 IngredientCard를 조회한다. 하나도 없으면 400
3. IngredientCard 등급 점수를 합산해 `totalScore`를 계산한다 (COMMON=1, RARE=3, EPIC=7)
4. 1단계에서 조회된 SupplementLog 중 하나라도 `isAffiliateProduct`가 true면 `affiliateBoost`를 true로 산정한다
5. 1단계에서 조회된 SupplementLog들의 `productName`을 모아 `includedSupplements`로 응답에 포함한다

### 성공 응답

`200 OK` (실제 호출로 확인)

```json
{
  "ingredientCards": [
    { "id": 58, "name": "천년삼", "grade": "EPIC" },
    { "id": 59, "name": "영지버섯", "grade": "RARE" },
    { "id": 60, "name": "들꽃", "grade": "COMMON" }
  ],
  "totalScore": 11,
  "affiliateBoost": true,
  "canSynthesize": true,
  "includedSupplements": ["종합비타민", "오메가3"]
}
```

- `totalScore`: 투입된 IngredientCard 등급 점수 합산 (COMMON=1, RARE=3, EPIC=7)
- `affiliateBoost`: 자동 편입된 SupplementLog 중 하나라도 `isAffiliateProduct`가 true면 true
- `canSynthesize`: 현재 구현에서는 항상 true. 연성 일일 제한 체크는 `/api/synthesize`에서 별도로 처리한다 (`SYNTHESIZE_CODEX_API_SPEC.md` 참고)
- `includedSupplements`: 서버가 자동으로 조회해 편입한 당일 인증 영양제들의 `productName` 목록. 프론트에서 "이 영양제들이 자동으로 들어갔어요"를 보여주는 용도

### 오류 응답

검증은 `StackService.evaluate()`에서 아래 순서대로 수행되며, 먼저 걸리는 조건에서 즉시 중단된다.

| 상태 | 조건 | 메시지 |
|---|---|---|
| 400 | `ownerId` 기준 오늘 `consumedDate` + `isVerified=true`인 SupplementLog가 하나도 없음 — 실제 호출로 확인 | 오늘 인증된 영양제가 없습니다 |
| 400 | `ingredientCardIds`로 조회된 IngredientCard가 하나도 없음 | 투입할 재료 카드가 없습니다 |
| 400 | 요청 본문이 유효한 JSON이 아님 (GlobalExceptionHandler가 처리, `HttpMessageNotReadableException`) | 요청 본문을 읽을 수 없습니다 |

## 4. 데이터 구조

### IngredientCard

`develop` 브랜치 통합 이후 팀원(backend-2)의 퀘스트/인벤토리 기능(`INVENTORY_API_SPEC.md` 참고)과 이 엔티티를 공유한다. 테이블명도 `ingredient_card`(원래 이름)로 되돌아갔다 — 통합 전에는 이름 충돌 때문에 `stack_ingredient_card`라는 임시 테이블을 썼었다.

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| ownerId | Long | 소유 유저 ID |
| name | String | 재료 이름 |
| grade | Grade (COMMON / RARE / EPIC) | 등급 |
| sourceQuestTitle | String | 획득한 퀘스트 제목 (예: `sourceQuestId`였던 필드가 실제로는 퀘스트 제목을 저장하는 용도로만 쓰여서 이름을 맞춰 변경) |
| quantity | int | 보유 수량 (기본값 1) |
| createdAt | LocalDateTime | 생성 시각 |

### SupplementLog

마찬가지로 `develop` 통합 이후 팀원의 영양제 인증 기능(`SUPPLEMENT_VERIFICATION_API_SPEC.md`, `SUPPLEMENT_API_SPEC.md` 참고)과 엔티티를 공유하며, 테이블명도 `supplement_log`(원래 이름)로 되돌아갔다.

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| ownerId | Long | 소유 유저 ID |
| productName | String | 제품명 |
| confidenceScore | Integer (nullable) | GPT-4o Vision 인식 신뢰도(0~100). 이 필드 도입 이전에 생성된 기록은 null |
| isAffiliateProduct | boolean | 제휴 제품 여부 |
| isVerified | boolean | 인증 완료 여부 |
| consumedDate | LocalDate | 섭취 날짜 |
| createdAt | LocalDateTime | 생성 시각 |

## 5. 현재 범위 밖 기능

- 연성 횟수/일일 제한 체크 (`POST /api/synthesize`에서 처리)
- `ingredientCardIds`의 실제 소유권 일치 검증 (현재 미검증, 존재 여부만 확인 — `ownerId` 자체는 JWT로 검증됨)
- 자동 편입된 영양제 중 일부만 선택적으로 제외하는 기능 (현재는 당일 인증된 것 전부를 편입)
