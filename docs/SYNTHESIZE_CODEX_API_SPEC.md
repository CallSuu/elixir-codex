# 등급 산정·코덱스 등록 API 명세서

- 문서 버전: 1.8
- 작성일: 2026-08-16
- 구현 기준 브랜치: develop
- 적용 범위: `POST /api/synthesize` (연금술 연성 실행, 등급 산정, 수치형 스탯 산출, 돌연변이 판정), `GET /api/codex` (연성된 일반 엘릭서 카드 목록 조회), `GET /api/codex/mutations` (돌연변이 엘릭서 카드 목록 조회), `GET /api/codex/{elixirCardId}` (카드 상세 조회)

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`Content-Type: application/json`

### 1.3 인증 규칙

`Authorization: Bearer <JWT>` 헤더가 필수다. 토큰이 없거나 무효하면 `401 { "message": "인증이 필요합니다" }`. `ownerId`는 더 이상 요청(바디/쿼리 파라미터)에 포함하지 않으며, 토큰의 이메일로 서버가 직접 조회한다 (`AuthenticatedUserService.getCurrentUserId()`). 토큰은 유효한데 매칭되는 User가 없으면 `401 { "message": "인증된 사용자를 찾을 수 없습니다" }`.

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

`com.elixircodex.backend.common.GlobalExceptionHandler`(`@RestControllerAdvice`)에서 전역으로 처리한다. `SynthesizeService`/`StackService`/`AlchemyNameService`가 던지는 커스텀 예외뿐 아니라, `GET /api/codex`에서 `ownerId` 쿼리 파라미터가 누락되는 경우나 `themeCategory`에 정의되지 않은 값이 오는 경우처럼 컨트롤러 이전 단계(Spring)에서 발생하는 오류도 모두 동일한 형식으로 통일된다. 처리되지 않은 예외는 `500 { "message": "서버 오류가 발생했습니다" }`로 응답한다.

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 연성 실행(등급 산정) | POST | `/api/synthesize` | 200 OK |
| 코덱스 조회(일반) | GET | `/api/codex` | 200 OK |
| 코덱스 조회(돌연변이) | GET | `/api/codex/mutations` | 200 OK |
| 코덱스 상세 조회 | GET | `/api/codex/{elixirCardId}` | 200 OK |

## 3. POST /api/synthesize

### 요청

`POST /api/synthesize`

```json
{
  "ingredientCardIds": [58, 59, 60],
  "themeCategory": "SKIN_ANTIOXIDANT"
}
```

| 필드 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| ingredientCardIds | List\<Long\> | Y | 투입할 IngredientCard id 목록 |
| themeCategory | ThemeCategory (`SKIN_ANTIOXIDANT`, `FATIGUE_ENERGY`, `DIET_BLOODSUGAR`, `SLEEP_REST`) | Y | 정의되지 않은 값이면 400 (아래 오류 응답 표 참고) |

`ownerId`/`supplementLogId`는 더 이상 요청에 포함하지 않는다. `ownerId`는 JWT에서, 어떤 영양제를 편입할지는 `StackService.evaluate()`가 그 `ownerId` 기준으로 당일 인증된 영양제를 자동으로 조회해 편입한다 (`STACK_API_SPEC.md` 참고).

### 처리 순서 (SynthesizeController가 AuthenticatedUserService로 얻은 ownerId를 SynthesizeService.synthesize(ownerId, request)에 넘기는 구현 기준)

1. `StackService.evaluate(ownerId, ...)`로 당일 인증된 영양제 자동 조회 + 스택 검증 + `totalScore`(등급 점수 합산) + `affiliateBoost` 산출. 검증 실패 시 400 (`STACK_API_SPEC.md`의 오류 2종과 동일)
2. `ownerId` 기준 오늘 0시 ~ 다음날 0시 사이 생성된 ElixirCard가 1개 이상이면 400 "오늘 이미 연성을 완료했습니다"
3. `baseGrade` 결정: `totalScore` 1~2 → COMMON, 3~6 → RARE, 7 이상 → EPIC
4. `totalScore >= 10`이면 프리즈마틱 판정을 먼저 시도한다: `affiliateBoost`가 false면 5%, true면 25% 확률로 성공 시 최종 등급을 `PRISMATIC_LEGENDARY`로 확정하고 5, 6단계는 건너뛴다. 확정 시 `serialNumber` = 기존 `PRISMATIC_LEGENDARY` 개수 + 1
5. 프리즈마틱이 아니고 `baseGrade`가 COMMON이면, 0~99 굴림에서 0이면 EPIC, 1~6이면 RARE, 나머지는 COMMON으로 최종 등급 확정
6. 프리즈마틱이 아니고 `baseGrade`가 RARE면, 0~99 굴림에서 0~5면 EPIC, 나머지는 RARE로 최종 등급 확정
7. `baseGrade`가 EPIC이면 별도 업그레이드 없이 그대로 EPIC이 최종 등급
8. 최종 등급(`finalGrade`, 프리즈마틱 판정까지 모두 끝난 값)이 확정된 직후, 4~7단계와 별개로 4% 확률(0~99 굴림에서 0~3)로 돌연변이 여부(`isMutated`)를 판정한다 (아래 3.2절 참고)
9. `StatRollService.rollStats()`에 넘길 등급을 정한다: 돌연변이가 아니면 `finalGrade` 그대로, 돌연변이면 한 단계 위 등급(COMMON→RARE, RARE→EPIC, EPIC·PRISMATIC_LEGENDARY는 이미 최상위라 그대로)을 사용해 수치형 스탯을 산출한다 (테마별 스탯 이름·등급별 값 범위는 아래 6절 참고). ElixirCard에 저장되는 `grade` 필드 자체는 이 단계와 무관하게 `finalGrade` 그대로 유지된다
10. 투입된 IngredientCard들의 이름을 모아 `AlchemyNameService.generateName(themeCategory, ingredientNames, isMutated)`를 호출해 `name`, `adviserComment` 생성 (실패 시 502, `ALCHEMY_NAME_API_SPEC.md` 오류 3종과 동일). 돌연변이면 시스템 프롬프트에 흑보랏빛·기괴한 톤 지시문이 추가된다 (아래 3.2절 참고)
11. 아트 조회: `finalGrade`가 `PRISMATIC_LEGENDARY`면 EPIC으로 취급하고, 그 외에는 `finalGrade` 그대로 사용한다 (돌연변이여도 아트는 9단계의 등급 상승과 무관하게 항상 `finalGrade` 기준). `elixir.art.generation-enabled=true`(기본값)이면 `ArtGenerationService.generate(grade, themeCategory, name)`으로 GPT 이미지 생성을 먼저 시도하고, 여기서 `ArtGenerationException`이 발생하면 경고 로그를 남기고 기존 `ArtMatchingService.findImageUrl(grade, themeCategory)` 템플릿 매칭으로 폴백한다. `elixir.art.generation-enabled=false`면 실시간 생성을 아예 시도하지 않고 바로 `ArtMatchingService`로 간다 (아래 3.1절 참고)
12. 위 결과로 ElixirCard를 저장하고 응답 반환 (`ingredientSummary`는 투입 재료 이름을 `, `로 join해 저장, `stats`는 9단계 결과, `isMutated`는 8단계 결과를 그대로 저장)

### 3.1 실시간 아트 생성과 템플릿 매칭 폴백

- `ArtGenerationService`(`com.elixircodex.backend.alchemy`)가 OpenAI 이미지 생성 API(`gpt-image-1`)를 호출해 카드 아트를 실시간으로 생성한다. 프롬프트는 고정 Style Seed("Dark fantasy gothic alchemist style, vintage elixir bottle, high detail illustration") + 등급 표현(EPIC: "ornate, glowing with power", RARE: "elegant, subtly enchanted", COMMON: "simple, humble") + `themeCategory.labelKo()` + 엘릭서 이름을 조합해서 만든다.
- API 호출 실패, 10초 타임아웃(연결/응답 모두), 응답에서 이미지 URL을 추출하지 못하는 경우 모두 `ArtGenerationException`으로 통일되고, `SynthesizeService`는 이를 잡아 경고 로그만 남긴 뒤 기존 `ArtMatchingService`(사전 등록된 `ArtTemplate` 중 무작위 매칭) 결과로 대체한다. 이 폴백은 사용자에게 오류로 노출되지 않고 연성 자체는 정상 완료된다.
- `elixir.art.generation-enabled` 설정값(`application.properties`, 기본 `true`)으로 실시간 생성 자체를 켜고 끌 수 있다. `false`로 두면 비용 문제 등으로 실시간 생성을 완전히 중단하고, 항상 템플릿 매칭만 사용하는 이전 동작으로 되돌아간다.
- `GET /api/alchemy/art`(`AlchemyArtController`)는 이 변경과 무관하다. 여전히 `ArtMatchingService`만 직접 호출하는 템플릿 매칭 테스트 전용 엔드포인트로 남아 있다.

### 3.2 돌연변이 시스템

- 최종 등급이 확정된 직후, 등급 산정과는 별개의 독립 굴림으로 4% 확률(`IntSupplier` 주입 패턴, 기존 프리즈마틱/업그레이드 굴림과 동일한 방식이지만 별도의 난수 소스)로 돌연변이 여부를 판정한다.
- 돌연변이가 확정되면: ① `StatRollService.rollStats()`에는 실제 확정 등급이 아니라 한 단계 위 등급을 넘겨 더 높은 범위의 스탯이 나오게 하고(EPIC·PRISMATIC_LEGENDARY는 이미 최상위라 그대로 유지), ② `AlchemyNameService.generateName(..., isMutated=true)`를 호출해 이름/조언 문구를 "흑보랏빛 이색 발광, 기괴하고 신비로운" 톤으로 생성하며, ③ `ElixirCard.isMutated`를 `true`로 저장한다.
- 돌연변이 여부는 카드의 `grade` 필드(등급 자체)나 아트 매칭에는 영향을 주지 않는다 — 등급은 8단계에서 이미 확정된 값 그대로 저장되고, 아트도 11단계에서 그 확정 등급 그대로 매칭된다. 돌연변이는 오직 스탯 수치와 이름/조언 문구의 분위기만 바꾸는 별도 연출 레이어다.
- 돌연변이 카드는 `GET /api/codex`(일반 목록)에서는 제외되고, `GET /api/codex/mutations`(아래 4.1절)에서만 조회된다.

### 성공 응답

`200 OK`

```json
{
  "elixirCardId": 1,
  "name": "심해의 정화 오일",
  "grade": "EPIC",
  "imageUrl": "https://placehold.co/400x600?text=EPIC-SKIN_ANTIOXIDANT-1",
  "adviserComment": "천년삼과 영지버섯의 조합은 깊은 진정 효과를 극대화합니다.",
  "serialNumber": null,
  "stats": {
    "피부 투명도": 72,
    "항산화 방어": 65,
    "장벽 결속력": 58,
    "수분 보습도": 81
  }
}
```

`serialNumber`는 최종 등급이 `PRISMATIC_LEGENDARY`로 확정된 경우에만 값이 채워지고, 그 외에는 항상 `null`이다. `stats`는 9단계에서 산출한 결과를 ElixirCard 저장 시점과 동일하게 이 응답에도 그대로 담아서 반환한다 (`GET /api/codex/{elixirCardId}`로도 동일한 값을 다시 조회할 수 있다, 아래 5절 참고).

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 400 | 스택 검증 실패 2종 (`STACK_API_SPEC.md` 참고) | `{ "message": "오늘 인증된 영양제가 없습니다" }` 등 |
| 400 | 오늘 이미 연성을 완료함 | `{ "message": "오늘 이미 연성을 완료했습니다" }` |
| 400 | `themeCategory`에 정의되지 않은 값 전달 (`HttpMessageNotReadableException`) | `{ "message": "요청 본문을 읽을 수 없습니다" }` |
| 400 | 요청 본문이 유효한 JSON이 아님 (`HttpMessageNotReadableException`) | `{ "message": "요청 본문을 읽을 수 없습니다" }` |
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |
| 502 | GPT 이름 생성 실패 3종 (`ALCHEMY_NAME_API_SPEC.md` 참고) — 실제 호출로 확인 | `{ "message": "엘릭서 이름 생성 요청이 실패했습니다" }` 등 |

> GPT 호출 실패는 ElixirCard 저장 이전 단계에서 발생하므로, 실패한 연성 시도는 ElixirCard로 남지 않고 일일 제한(2단계)에도 영향을 주지 않는다. 즉 GPT 호출이 실패하면 같은 날 다시 시도할 수 있다.

## 4. GET /api/codex

일반(비돌연변이) 엘릭서 카드만 반환한다. `findByOwnerIdOrderByCreatedAtDesc` 결과에서 `isMutated=true`인 카드는 걸러낸다.

### 요청

`GET /api/codex` (파라미터 없음, `ownerId`는 JWT에서 추출)

### 성공 응답

`200 OK` — `findByOwnerIdOrderByCreatedAtDesc` 결과를 최신순으로 배열 반환하되 `isMutated=true`인 카드는 제외한다 (카드가 없으면 빈 배열)

```json
[
  {
    "id": 1,
    "name": "심해의 정화 오일",
    "grade": "EPIC",
    "imageUrl": "https://placehold.co/400x600?text=EPIC-SKIN_ANTIOXIDANT-1",
    "serialNumber": null,
    "isMutated": false
  }
]
```

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |

## 4.1 GET /api/codex/mutations

돌연변이(`isMutated=true`) 엘릭서 카드만, `GET /api/codex`와 동일한 응답 형식(`CodexCardSummary`)으로 반환한다.

### 요청

`GET /api/codex/mutations` (파라미터 없음, `ownerId`는 JWT에서 추출)

### 성공 응답

`200 OK` — `findByOwnerIdOrderByCreatedAtDesc` 결과에서 `isMutated=true`인 카드만 최신순으로 반환 (없으면 빈 배열)

```json
[
  {
    "id": 5,
    "name": "검은 잔영의 변종수",
    "grade": "RARE",
    "imageUrl": "https://placehold.co/400x600?text=RARE-SKIN_ANTIOXIDANT-1",
    "serialNumber": null,
    "isMutated": true
  }
]
```

### 오류 응답

`GET /api/codex`의 오류 응답 표와 동일하다.

## 5. GET /api/codex/{elixirCardId}

일반 카드와 돌연변이 카드는 구분 없이 그대로 상세를 반환한다 (필터링 없음, 돌연변이 여부 자체는 6절 참고). 다만 **소유권 검증은 있다**: 조회된 카드의 `ownerId`가 JWT로 얻은 `ownerId`와 다르면, 카드가 실제로는 존재하더라도 "없는 카드"와 동일하게 처리한다 — 카드 존재 여부 자체가 다른 유저에게 노출되지 않도록, 소유권 불일치도 "존재하지 않는 카드"의 404 대응 400과 완전히 같은 상태/메시지로 응답한다 (구분 가능한 별도 메시지를 주지 않음). `CodexCardDetailResponse`에는 `isMutated` 필드가 없으므로, 이 응답만으로는 해당 카드가 돌연변이인지 알 수 없다 — 돌연변이 여부는 `GET /api/codex`(제외됨) 또는 `GET /api/codex/mutations`(포함됨) 목록 조회로만 판단 가능하다 (아래 7절 참고).

### 요청

`GET /api/codex/1`

| 경로 변수 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| elixirCardId | Long | Y | 존재하지 않거나 본인 소유가 아니면 400, 동일한 메시지 (아래 오류 응답 참고) |

### 성공 응답

`200 OK` (실제 호출로 확인)

```json
{
  "id": 1,
  "name": "영원의 꽃잎 정화 오일",
  "grade": "EPIC",
  "imageUrl": "https://placehold.co/400x600?text=EPIC-SKIN_ANTIOXIDANT-2",
  "serialNumber": null,
  "ingredientSummary": "천년삼, 영지버섯, 들꽃",
  "adviserComment": "천년삼은 피부 속 에너지를 일깨우고, 영지버섯은 보호막을 강화하도다.",
  "stats": {
    "피부 투명도": 72,
    "항산화 방어": 65,
    "장벽 결속력": 58,
    "수분 보습도": 81
  }
}
```

이 기능 배포 이전에 생성된 카드처럼 `stats`가 한 번도 저장된 적 없는 경우, 오류가 아니라 빈 객체 `{}`로 응답한다 (실제 호출로 확인).

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 400 | `elixirCardId`로 ElixirCard 조회 실패 (`ElixirCardRepository.findById`) — 실제 호출로 확인 | `{ "message": "존재하지 않는 카드입니다" }` |
| 400 | 조회는 됐지만 그 카드의 `ownerId`가 JWT의 `ownerId`와 다름 — 실제 호출로 확인, 위 "존재하지 않는 카드" 케이스와 완전히 동일한 응답 | `{ "message": "존재하지 않는 카드입니다" }` |
| 400 | `elixirCardId`에 Long으로 변환할 수 없는 값 전달 (`MethodArgumentTypeMismatchException`) — 실제 호출로 확인 | `{ "message": "파라미터 'elixirCardId'의 값이 올바르지 않습니다" }` |
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |

REST 컨벤션상 404가 자연스러운 상황이지만, 이 프로젝트의 기존 컨벤션(존재하지 않는 리소스도 400 + 커스텀 메시지)을 그대로 따른다. 소유권 불일치도 이 컨벤션을 그대로 확장해, 별도 403/404가 아니라 "존재하지 않는 카드"와 동일한 400으로 응답해 카드 존재 여부 자체를 감춘다.

## 6. 데이터 구조

### ElixirGrade (Enum)

`COMMON`, `RARE`, `EPIC`, `PRISMATIC_LEGENDARY`

### ElixirCard

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| ownerId | Long | 소유 유저 ID |
| name | String | 생성된 엘릭서 이름 |
| grade | ElixirGrade | 최종 확정 등급 |
| themeCategory | ThemeCategory | 연성 시 지정한 테마 |
| imageUrl | String | 아트 이미지 URL. `elixir.art.generation-enabled=true`이고 실시간 생성이 성공하면 GPT가 생성한 이미지 URL, 그렇지 않으면(비활성화 또는 생성 실패로 폴백) 템플릿 매칭 결과 (3.1절 참고) |
| adviserComment | String | 연금술사 시너지 조언 문구 |
| serialNumber | Long (nullable) | PRISMATIC_LEGENDARY 확정 시에만 채번, 그 외 null |
| ingredientSummary | String | 투입 재료 이름을 `, `로 join한 문자열 |
| isMutated | boolean | 돌연변이 판정 결과 (기본값 false). 3.2절 참고 |
| stats | Map\<String, Integer\> | 테마별 스탯 이름 → 등급 범위 내 값 (돌연변이면 한 단계 위 등급 범위). 별도 테이블 `elixir_card_stats`에 `@ElementCollection`으로 저장 |
| createdAt | LocalDateTime | 생성 시각 (일일 연성 제한 판정 기준) |

### CodexCardSummary (GET /api/codex, GET /api/codex/mutations 응답 항목)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | ElixirCard id |
| name | String | 엘릭서 이름 |
| grade | ElixirGrade | 등급 |
| imageUrl | String | 아트 이미지 URL |
| serialNumber | Long (nullable) | 프리즈마틱 시리얼 번호 |
| isMutated | boolean | 돌연변이 여부. `GET /api/codex`는 항상 false만, `GET /api/codex/mutations`는 항상 true만 반환 |

### CodexCardDetailResponse (GET /api/codex/{elixirCardId} 응답)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | ElixirCard id |
| name | String | 엘릭서 이름 |
| grade | ElixirGrade | 등급 |
| imageUrl | String | 아트 이미지 URL |
| serialNumber | Long (nullable) | 프리즈마틱 시리얼 번호 |
| ingredientSummary | String | 투입 재료 이름 join 문자열 |
| adviserComment | String | 연금술사 시너지 조언 문구 |
| stats | Map\<String, Integer\> | 수치형 스탯 |

### StatRollService 상수 (테마별 스탯 이름)

| ThemeCategory | 스탯 이름 |
|---|---|
| SKIN_ANTIOXIDANT (피부/항산화) | 피부 투명도, 항산화 방어, 장벽 결속력, 수분 보습도 |
| FATIGUE_ENERGY (피로/에너지) | 활력 마나량, 신속 순환력, 심장 박동력, 피로 무력화 |
| DIET_BLOODSUGAR (혈당/다이어트) | 당독소 봉인, 지방 연소열, 포만 유지력, 흡수 차단력 |
| SLEEP_REST (수면/휴식) | 스트레스 차단, 심연 수면도, 근육 이완도, 독소 정화력 |

### StatRollService 상수 (등급별 값 범위)

| ElixirGrade | 값 범위 |
|---|---|
| COMMON | 20 ~ 50 |
| RARE | 40 ~ 70 |
| EPIC | 60 ~ 90 |
| PRISMATIC_LEGENDARY | 85 ~ 100 |

각 스탯 값은 테마의 스탯 이름 개수만큼 각각 독립적으로 굴려 산출한다 (동일 카드 안에서도 스탯끼리 값이 다를 수 있음).

### 돌연변이 판정 확률 및 등급 상승표

| 항목 | 값 |
|---|---|
| 돌연변이 확률 | 4% (0~99 굴림에서 0~3) |
| COMMON → | RARE |
| RARE → | EPIC |
| EPIC → | EPIC (상승 없음, 이미 최상위 비-프리즈마틱 등급) |
| PRISMATIC_LEGENDARY → | PRISMATIC_LEGENDARY (상승 없음, 최상위 등급) |

위 등급 상승은 `StatRollService.rollStats()` 호출 시에만 적용되고, `ElixirCard.grade`(실제 확정 등급)에는 반영되지 않는다.

## 7. 현재 범위 밖 기능

- `PRISMATIC_LEGENDARY` 전용 아트 미구현 (아트 매칭 시 EPIC으로 대체)
- `ingredientCardIds`의 실제 소유권 일치 검증 (현재 미검증, 존재 여부만 확인 — `ownerId` 자체는 JWT로 검증됨)
- `GET /api/codex` 페이징/필터링 (현재 전체 목록만 최신순으로 반환)
- 연성 확률(프리즈마틱/등급 업그레이드)에 대한 로그·통계 기능
- 자동 편입된 영양제 중 일부만 선택적으로 제외하는 기능 (`STACK_API_SPEC.md` 참고)
- 스탯 값 범위/밸런싱에 대한 관리자 설정 기능 (현재 코드 상수로 고정)
- 실시간 생성 아트에 대한 별도 저장소/재사용 캐시 (매 연성마다 새로 호출, 동일 조합이어도 캐시하지 않음)
- 실시간 생성 실패 사유(타임아웃/API 오류/응답 파싱 실패)를 구분해서 응답이나 로그 레벨을 달리하는 기능 (현재는 모두 `ArtGenerationException`으로 통일되고 경고 로그 1건만 남김)
- 돌연변이 전용 아트 (현재 아트는 실제 확정 등급 그대로 매칭되며, `ArtGenerationService`의 프롬프트/`ArtMatchingService`의 템플릿 모두 돌연변이 여부를 반영하지 않는다 — 스탯·이름/조언 문구만 변종 톤으로 바뀌고 이미지는 일반 카드와 동일한 방식으로 결정된다)
- 돌연변이 확률(4%)에 대한 관리자 설정 기능 (현재 코드 상수로 고정)
- `POST /api/synthesize` 응답(`SynthesizeResponse`)과 `GET /api/codex/{elixirCardId}`(`CodexCardDetailResponse`)에 `isMutated` 필드 노출 (현재는 `GET /api/codex`/`GET /api/codex/mutations` 목록 조회로만 돌연변이 여부를 구분할 수 있음)
