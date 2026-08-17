# 연금술 이름·조언 생성 API 명세서

- 문서 버전: 1.1
- 작성일: 2026-08-11
- 구현 기준 브랜치: backend-1
- 적용 범위: `POST /api/alchemy/generate-name` (GPT-4o를 호출해 엘릭서 이름과 연금술사 조언 문구를 생성하는 기능)

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`Content-Type: application/json`

### 1.3 인증 규칙

현재는 별도 인증 없이 요청을 처리한다. 이 API 자체는 `ownerId`를 받지 않는다 (재료 이름 목록만 전달). 추후 JWT 인증 적용 예정.

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

`com.elixircodex.backend.common.GlobalExceptionHandler`(`@RestControllerAdvice`)에서 전역으로 처리한다. 컨트롤러가 직접 검증해서 막는 오류(`ingredientNames` 개수 미달)뿐 아니라, 요청 바디의 enum 필드(`themeCategory`)에 정의되지 않은 값이 오는 경우처럼 컨트롤러 이전 단계(JSON 역직렬화)에서 실패하는 경우도 동일한 형식으로 통일된다. 자세한 내용은 아래 3절의 오류 응답 표를 참고.

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 연금술 이름·조언 생성 | POST | `/api/alchemy/generate-name` | 200 OK |

## 3. POST /api/alchemy/generate-name

### 요청

`POST /api/alchemy/generate-name`

```json
{
  "themeCategory": "SKIN_ANTIOXIDANT",
  "ingredientNames": ["천년삼", "영지버섯"]
}
```

| 필드 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| themeCategory | ThemeCategory (`SKIN_ANTIOXIDANT`, `FATIGUE_ENERGY`, `DIET_BLOODSUGAR`, `SLEEP_REST`) | Y | 정의되지 않은 값이면 400 (아래 오류 응답 표 참고) |
| ingredientNames | List\<String\> | Y | null이거나 원소 2개 미만이면 400 |

### 성공 응답

`200 OK`

```json
{
  "name": "심해의 정화 오일",
  "adviserComment": "천년삼과 영지버섯의 조합은 깊은 진정 효과를 극대화합니다. 밤에 사용하면 더욱 좋습니다."
}
```

이름/조언 문구는 OpenAI `gpt-4o` 모델이 매 요청마다 생성하므로 실제 값은 매번 달라진다. 내부적으로 시스템 프롬프트에 등급별 네이밍 규칙("[수식어 + 핵심 테마 + 연금술 제형]" 조합, 중세 판타지 톤)을 지정하고, `response_format: json_object`로 응답을 강제한 뒤 파싱한다.

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 400 | `ingredientNames`가 null이거나 크기가 2 미만 (GPT 호출 전에 컨트롤러에서 차단) | `{ "message": "재료가 최소 2개 이상 필요합니다" }` |
| 400 | `themeCategory`에 정의되지 않은 값 전달 (JSON 역직렬화 실패, `HttpMessageNotReadableException`) — 실제 호출로 확인 | `{ "message": "요청 본문을 읽을 수 없습니다" }` |
| 400 | 요청 본문이 유효한 JSON이 아님 (`HttpMessageNotReadableException`) — 실제 호출로 확인 | `{ "message": "요청 본문을 읽을 수 없습니다" }` |
| 502 | OpenAI API 호출 실패 (타임아웃, 네트워크 오류, 4xx/5xx 등) — 실제 호출로 확인 | `{ "message": "엘릭서 이름 생성 요청이 실패했습니다" }` |
| 502 | OpenAI 응답의 `choices[0].message.content`가 비어 있음 | `{ "message": "엘릭서 이름 생성 응답이 비어 있습니다" }` |
| 502 | `content`를 JSON으로 파싱하지 못함 | `{ "message": "엘릭서 이름 생성 응답을 해석하지 못했습니다" }` |

## 4. 데이터 구조

### NameGenerationRequest / NameGenerationResponse

| 필드 | 타입 | 설명 |
|---|---|---|
| themeCategory | ThemeCategory | 요청 시 지정하는 테마 |
| ingredientNames | List\<String\> | 요청 시 투입 재료 이름 목록 |
| name | String | 응답: 생성된 엘릭서 이름 |
| adviserComment | String | 응답: 연금술사의 시너지 조언 문구 (2~3문장) |

## 5. 현재 범위 밖 기능

- DB 저장 없음. 이 API 자체는 순수 GPT 호출 검증용이며, `POST /api/synthesize`가 내부적으로 동일한 `AlchemyNameService`를 재사용해 실제 엘릭서 카드 생성에 사용한다 (`SYNTHESIZE_CODEX_API_SPEC.md` 참고)
- OpenAI 호출에 대한 재시도(retry)/속도 제한(rate limit) 미구현
- 실제 JWT 인증 연동 전
