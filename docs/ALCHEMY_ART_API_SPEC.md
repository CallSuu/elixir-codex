# 연금술 아트(카드 이미지) 사전 템플릿 매칭 API 명세서

- 문서 버전: 1.1
- 작성일: 2026-08-11
- 구현 기준 브랜치: backend-1
- 적용 범위: `GET /api/alchemy/art` (등급·테마 조합에 맞는 카드 아트 이미지 URL 조회)

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`Content-Type: application/json`

### 1.3 인증 규칙

현재는 별도 인증 없이 요청을 처리한다. 이 API는 `ownerId`를 받지 않는다 (등급·테마 조합만으로 조회). 추후 JWT 인증 적용 예정.

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

`com.elixircodex.backend.common.GlobalExceptionHandler`(`@RestControllerAdvice`)에서 전역으로 처리한다. 이 API는 컨트롤러에서 별도 검증 코드 없이 `grade`, `themeCategory`를 각각 `Grade`, `ThemeCategory` 타입의 `@RequestParam`으로 직접 받으며, Spring이 쿼리 파라미터를 enum으로 변환하는 과정에서 실패하면 `MethodArgumentTypeMismatchException`이 발생하고 GlobalExceptionHandler가 이를 잡아 아래 형식으로 응답한다.

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 아트 템플릿 매칭 | GET | `/api/alchemy/art` | 200 OK |

## 3. GET /api/alchemy/art

### 요청

`GET /api/alchemy/art?grade=EPIC&themeCategory=SKIN_ANTIOXIDANT`

| 파라미터 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| grade | Grade (`COMMON`, `RARE`, `EPIC`) | Y | 정의된 값이 아니면 400 |
| themeCategory | ThemeCategory (`SKIN_ANTIOXIDANT`, `FATIGUE_ENERGY`, `DIET_BLOODSUGAR`, `SLEEP_REST`) | Y | 정의된 값이 아니면 400 |

### 성공 응답

`200 OK`

해당 조합의 ArtTemplate이 있는 경우 (실제 호출로 확인):

```json
{ "imageUrl": "https://placehold.co/400x600?text=EPIC-SKIN_ANTIOXIDANT-1" }
```

해당 조합의 ArtTemplate이 하나도 없는 경우, 오류가 아니라 고정된 기본 이미지로 폴백한다 (실제 호출로 확인):

```json
{ "imageUrl": "https://placehold.co/400x600?text=Elixir" }
```

같은 조합에 템플릿이 여러 개 있으면 그중 하나를 무작위로 선택해서 반환한다.

### 오류 응답

| 상태 | 조건 | 메시지 |
|---|---|---|
| 400 | `grade`가 정의된 enum 값이 아님 (`MethodArgumentTypeMismatchException`) — 실제 호출로 확인 | 파라미터 'grade'의 값이 올바르지 않습니다 |
| 400 | `themeCategory`가 정의된 enum 값이 아님 (`MethodArgumentTypeMismatchException`) | 파라미터 'themeCategory'의 값이 올바르지 않습니다 |
| 400 | `grade` 또는 `themeCategory` 파라미터 자체가 누락됨 (`MissingServletRequestParameterException`) | 필수 파라미터 '{파라미터명}'이(가) 누락되었습니다 |

## 4. 데이터 구조

### ArtTemplate

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| grade | Grade (COMMON / RARE / EPIC) | 등급 |
| themeCategory | ThemeCategory | 테마 |
| imageUrl | String | 이미지 URL |
| createdAt | LocalDateTime | 생성 시각 |

## 5. 현재 범위 밖 기능

- `PRISMATIC_LEGENDARY` 전용 아트 미구현. `Grade` enum 자체에 프리즈마틱 값이 없으며, `POST /api/synthesize`에서 최종 등급이 PRISMATIC_LEGENDARY인 경우 아트 매칭 시에는 EPIC으로 취급해서 이 API 내부 로직(`ArtMatchingService`)을 재사용한다
- 실제 JWT 인증 연동 전
