# 온보딩 카테고리 분류 API 명세서

- 문서 버전: 1.1
- 작성일: 2026-08-15
- 구현 기준 브랜치: develop
- 적용 범위: `POST /api/onboarding/classify` (자유 텍스트를 GPT-4o로 분석해 4대 건강 카테고리 중 하나로 분류하고, 그 결과를 `User.selectedCategory`에 반영하는 기능)

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`Content-Type: application/json`

### 1.3 인증 규칙

`Authorization: Bearer <JWT>` 헤더가 필수다. 토큰이 없거나 무효하면 `401 { "message": "인증이 필요합니다" }`. `ownerId`는 더 이상 요청 바디에 포함하지 않으며, 토큰의 이메일로 서버가 직접 조회한다 (`AuthenticatedUserService.getCurrentUserId()`). 토큰은 유효한데 매칭되는 User가 없으면 `401 { "message": "인증된 사용자를 찾을 수 없습니다" }`.

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

`com.elixircodex.backend.common.GlobalExceptionHandler`(`@RestControllerAdvice`)에서 전역으로 처리한다. 처리되지 않은 예외는 `500 { "message": "서버 오류가 발생했습니다" }`로 응답한다.

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 온보딩 카테고리 분류 | POST | `/api/onboarding/classify` | 200 OK |

## 3. POST /api/onboarding/classify

온보딩 시점뿐 아니라 유저가 원할 때 언제든 다시 호출할 수 있다 — "한 번만 설정 가능" 같은 제약은 없으며, 호출할 때마다 그 결과로 `selectedCategory`를 무조건 덮어쓴다.

### 요청

`POST /api/onboarding/classify`

```json
{
  "freeText": "요즘 계속 피곤하고 무기력해요"
}
```

| 필드 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| freeText | String | Y | 유저가 입력한 자유 텍스트 |

`ownerId`는 더 이상 요청 바디에 포함하지 않는다. JWT에서 추출한 이메일로 `AuthenticatedUserService`가 `com.example.demo.Entity.User`를 조회해 얻는다.

### 처리 로직 (OnboardingController가 AuthenticatedUserService로 얻은 ownerId를 OnboardingClassificationService.classifyAndUpdateUser(ownerId, freeText)에 넘기는 구현 기준)

1. `ownerId`로 `User`를 다시 조회한다 (이 시점의 `ownerId`는 이미 `AuthenticatedUserService`가 이메일→User 조회로 검증한 값이라, 이 단계의 "유저 없음" 400은 사실상 발생하지 않는다 — User가 인증 이후 삭제되는 경합 상황 정도의 방어 코드로 남아 있음)
2. OpenAI Chat Completions API를 `gpt-4o` 모델로 호출한다. `response_format: json_object`로 응답 형식을 강제하고, 시스템 프롬프트로 4개 카테고리 중 하나를 코드값(`SKIN_ANTIOXIDANT`/`FATIGUE_ENERGY`/`DIET_BLOODSUGAR`/`SLEEP_REST`)으로만 답하도록 지시한다. 호출/응답 파싱 실패 시 502
3. 응답의 `category` 값을 `ThemeCategory` enum으로 변환한다. 4개 값 중 하나가 아니면 502
4. 분류된 `ThemeCategory`의 한글 라벨(`labelKo()`)로 `User.selectedCategory`를 덮어쓰고 저장한다. 기존 값이 무엇이었든 상관없이 매번 새로 덮어쓴다
5. 분류된 `ThemeCategory`와 그 한글 라벨을 응답으로 반환한다

`labelKo()` 값은 회원가입 API(`UserRequestDto.SignUp.selectedCategory`, `USER_API_SPEC.md` 참고)가 쓰는 것과 정확히 같은 문자열이다: `SKIN_ANTIOXIDANT`→"피부/항산화", `FATIGUE_ENERGY`→"피로/에너지", `DIET_BLOODSUGAR`→"혈당/다이어트", `SLEEP_REST`→"수면/휴식". (`com.example.demo.Entity.QuestCategory`의 라벨과도 동일하게 맞춰져 있어, 저장된 값을 그대로 `QuestCategory.fromLabel()`에 넘길 수 있다.)

### 성공 응답

`200 OK`

```json
{
  "themeCategory": "FATIGUE_ENERGY",
  "labelKo": "피로/에너지"
}
```

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 400 | `ownerId`로 User 조회 실패 (이론상 도달 불가에 가까움, 위 처리 로직 1단계 참고) | `{ "message": "존재하지 않는 유저입니다" }` |
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |
| 502 | OpenAI API 호출 실패 (타임아웃, 네트워크 오류, 4xx/5xx 등) — 실제 호출로 확인 | `{ "message": "카테고리 분류 요청이 실패했습니다" }` |
| 502 | OpenAI 응답의 `choices[0].message.content`가 비어 있음 | `{ "message": "카테고리 분류 응답이 비어 있습니다" }` |
| 502 | `content`를 JSON으로 파싱하지 못함 | `{ "message": "카테고리 분류 응답을 해석하지 못했습니다" }` |
| 502 | `category` 값이 4개 코드값 중 하나가 아님 | `{ "message": "유효하지 않은 카테고리입니다: {받은 값}" }` |

`ownerId` 검증은 GPT 호출 이전에 수행되므로, 존재하지 않는 유저에 대해서는 GPT API를 호출하지 않는다.

## 4. 데이터 구조

### OnboardingClassifyRequest / OnboardingClassifyResponse

| 필드 | 타입 | 설명 |
|---|---|---|
| freeText | String | 요청 시 지정하는 자유 텍스트 |
| themeCategory | ThemeCategory (`com.elixircodex.backend.alchemy.ThemeCategory`) | 응답: 분류된 카테고리 |
| labelKo | String | 응답: 분류된 카테고리의 한글 라벨 |

### 영향받는 User 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| selectedCategory | String | 이 API 호출 성공 시 분류 결과의 한글 라벨로 덮어써짐 (`com.example.demo.Entity.User`) |

## 5. 현재 범위 밖 기능

- 분류 이력 저장/재조회 (매 호출은 `User.selectedCategory`를 덮어쓸 뿐, 과거 분류 기록은 남기지 않음)
- `freeText`에 대한 별도 검증(최소 길이 등) 없음 — 빈 문자열이 와도 그대로 GPT에 전달됨
- 분류 신뢰도/근거 문구 응답 (현재는 카테고리 코드값만 받음)
