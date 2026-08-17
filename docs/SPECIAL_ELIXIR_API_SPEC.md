# 스페셜 엘릭서 API 명세서

- 문서 버전: 1.1
- 작성일: 2026-08-15
- 구현 기준 브랜치: develop
- 적용 범위: `POST /api/special-elixirs`(생성), `GET /api/special-elixirs`(목록 조회), `DELETE /api/special-elixirs/{id}`(삭제)

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`Content-Type: application/json`

### 1.3 인증 규칙

`Authorization: Bearer <JWT>` 헤더가 필수다. 토큰이 없거나 무효하면 `401 { "message": "인증이 필요합니다" }`. `ownerId`는 더 이상 요청에 포함하지 않으며, 토큰의 이메일로 서버가 직접 조회한다 (`AuthenticatedUserService.getCurrentUserId()`). 토큰은 유효한데 매칭되는 User가 없으면 `401 { "message": "인증된 사용자를 찾을 수 없습니다" }`.

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

`com.elixircodex.backend.common.GlobalExceptionHandler`(`@RestControllerAdvice`)에서 전역으로 처리한다. 처리되지 않은 예외는 `500 { "message": "서버 오류가 발생했습니다" }`로 응답한다.

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 스페셜 엘릭서 생성 | POST | `/api/special-elixirs` | 200 OK |
| 스페셜 엘릭서 목록 조회 | GET | `/api/special-elixirs` | 200 OK |
| 스페셜 엘릭서 삭제 | DELETE | `/api/special-elixirs/{id}` | 200 OK |

## 3. POST /api/special-elixirs

일반 연성(`POST /api/synthesize`)과 달리 재료 카드나 영양제 인증 없이, 자유 텍스트 하나만으로 GPT-4o가 이름/조언/등급 매칭용 테마를 만들어내는 별도 생성 경로다.

### 요청

`POST /api/special-elixirs`

```json
{
  "freeText": "요즘 계속 피곤하고 무기력해요"
}
```

| 필드 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| freeText | String | Y | 자유 텍스트. 보유 개수가 3개면 GPT 호출 전에 즉시 400 |

`ownerId`는 더 이상 요청 바디에 포함하지 않는다. JWT에서 추출한다.

### 처리 로직 (SpecialElixirController가 AuthenticatedUserService로 얻은 ownerId를 SpecialElixirService.create(ownerId, freeText)에 넘기는 구현 기준)

1. `ownerId` 기준 보유 개수(`SpecialElixirRepository.countByOwnerId`)가 3개 이상이면 즉시 400 (GPT 호출 없음)
2. `OnboardingClassificationService.classify(freeText)`로 `freeText`를 4개 테마 카테고리 중 하나로 분류한다 (`ONBOARDING_API_SPEC.md` 3절과 동일한 GPT-4o 분류 로직을 재사용하되, **User는 업데이트하지 않는다** — 순수 분류 메서드만 호출)
3. `AlchemyNameService.generate()`를 호출해 이름/조언 문구를 생성한다. `ingredientNames` 자리에는 `freeText`를 담은 단일 원소 리스트(`List.of(freeText)`)를 넘긴다
4. `ArtMatchingService.findImageUrl()`을 호출해 이미지 URL을 받는다. 등급은 항상 `Grade.EPIC`으로 고정한다 (스페셜 엘릭서 전용 아트 템플릿이 아직 없어서 EPIC 아트를 임시로 재사용 — `ALCHEMY_ART_API_SPEC.md` 참고)
5. 위 결과로 `SpecialElixir`를 저장하고 응답을 반환한다

### 성공 응답

`200 OK` (실제 호출로 확인 — GPT 응답 부분은 API 키 없이는 502로 막혀서 그 앞 단계인 400 분기만 실제 확인, 형식은 코드 기준)

```json
{
  "id": 1,
  "name": "은은한 밤의 안식 포션",
  "imageUrl": "https://placehold.co/400x600?text=EPIC-SLEEP_REST-1",
  "adviserComment": "숙면에 도움을 줍니다.",
  "themeCategory": "SLEEP_REST",
  "createdAt": "2026-08-14T16:50:22"
}
```

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 400 | `ownerId` 기준 보유 개수가 이미 3개 — 실제 호출로 확인 | `{ "message": "스페셜 엘릭서는 최대 3개까지 저장할 수 있습니다. 기존 엘릭서를 삭제한 뒤 다시 시도해주세요." }` |
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |
| 502 | 카테고리 분류(GPT-4o) 호출/응답 파싱 실패 — 실제 호출로 확인 | `{ "message": "카테고리 분류 요청이 실패했습니다" }` 등 (`ONBOARDING_API_SPEC.md` 오류 표와 동일) |
| 502 | 엘릭서 이름/조언 생성(GPT-4o) 호출/응답 파싱 실패 | `{ "message": "엘릭서 이름 생성 요청이 실패했습니다" }` 등 (`ALCHEMY_NAME_API_SPEC.md` 오류 표와 동일) |

## 4. GET /api/special-elixirs

### 요청

`GET /api/special-elixirs` (파라미터 없음, `ownerId`는 JWT에서 추출)

### 성공 응답

`200 OK` (실제 호출로 확인) — `findByOwnerId` 결과를 배열로 반환 (정렬 순서 별도 지정 없음). 보유한 게 없으면 빈 배열.

```json
[
  { "id": 2, "name": "엘릭서2", "imageUrl": "url2", "adviserComment": "조언2", "themeCategory": "SLEEP_REST", "createdAt": "2026-08-14T16:50:22" },
  { "id": 3, "name": "엘릭서3", "imageUrl": "url3", "adviserComment": "조언3", "themeCategory": "SLEEP_REST", "createdAt": "2026-08-14T16:50:22" }
]
```

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |

## 5. DELETE /api/special-elixirs/{id}

### 요청

`DELETE /api/special-elixirs/1` (`ownerId`는 JWT에서 추출)

| 파라미터 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| id | Long (경로 변수) | Y | 존재하지 않으면 400 |

검증 순서: id로 먼저 조회하고(없으면 400), 있으면 그 카드의 `ownerId`와 JWT에서 얻은 `ownerId`가 일치하는지 확인한다(다르면 400).

### 성공 응답

`200 OK`, 빈 본문 (실제 호출로 확인)

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 400 | `id`로 조회된 SpecialElixir가 없음 — 실제 호출로 확인 | `{ "message": "존재하지 않는 엘릭서입니다" }` |
| 400 | 조회는 됐지만 `ownerId`가 소유자와 다름 — 실제 호출로 확인 | `{ "message": "본인의 엘릭서만 삭제할 수 있습니다" }` |
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |

## 6. 데이터 구조

### SpecialElixir

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| ownerId | Long | 소유 유저 ID |
| name | String | GPT-4o가 생성한 엘릭서 이름 |
| imageUrl | String | 매칭된 아트 이미지 URL (항상 EPIC 등급 아트) |
| adviserComment | String | GPT-4o가 생성한 연금술사 조언 문구 |
| themeCategory | ThemeCategory | 자유 텍스트 분류 결과 |
| createdAt | LocalDateTime | 생성 시각 |

### SpecialElixirCreateRequest / SpecialElixirResponse

| 필드 | 타입 | 설명 |
|---|---|---|
| freeText | String | 요청 시 지정하는 자유 텍스트 (`ownerId`는 더 이상 요청에 없음, JWT에서 추출) |
| id / name / imageUrl / adviserComment / themeCategory / createdAt | - | `SpecialElixir` 필드와 동일 |

## 7. 현재 범위 밖 기능

- 3개 제한을 우회하는 "교체" 기능 (현재는 기존 것을 먼저 삭제해야만 새로 생성 가능)
- 스페셜 엘릭서 전용 아트 템플릿 (현재는 `Grade.EPIC` 아트를 임시로 재사용 — `ALCHEMY_ART_API_SPEC.md`에 전용 템플릿이 추가되면 교체 필요)
- 도감(`GET /api/codex`)과의 통합 여부 (스페셜 엘릭서는 별도 테이블/엔드포인트로 관리되며 현재 도감에는 노출되지 않음)
- 생성 시 재료/영양제 소비 없음 (일반 연성과 달리 자유 텍스트만으로 생성되므로 `POST /api/stack` 결과와 무관)
