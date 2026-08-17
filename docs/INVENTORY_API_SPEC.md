# 인벤토리 API 명세서

- 문서 버전: 1.1
- 작성일: 2026-08-13
- 구현 기준 브랜치: develop
- 적용 범위: `GET /api/inventory` (보유 재료 카드 + 레시피 스크롤 조회, MAT 모듈)

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`Content-Type: application/json`

### 1.3 인증 규칙

`Authorization: Bearer {token}` 헤더 인증 필요(USER_API_SPEC.md 참고).

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

`com.elixircodex.backend.common.GlobalExceptionHandler`에서 전역 처리(USER_API_SPEC.md 1.4와 동일).

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 인벤토리 조회 | GET | `/api/inventory` | 200 OK |

## 3. GET /api/inventory

인증된 유저 본인이 보유한 재료 카드와 레시피 스크롤을 함께 반환한다. 재료 카드는 퀘스트 완료 보상으로, 레시피 스크롤은 주간 퀘스트 완료 보상으로 지급된다(QUEST_API_SPEC.md 참고).

### 요청

바디 없음.

### 성공 응답

`200 OK`

```json
{
  "ingredientCards": [
    { "name": "정제된 수분 결정", "grade": "COMMON", "quantity": 3, "sourceQuestTitle": "물 1.5L 마시기" },
    { "name": "황금 균형의 씨앗", "grade": "EPIC", "quantity": 1, "sourceQuestTitle": "매 끼니 채소 포함해서 먹기" }
  ],
  "recipeScrolls": [
    { "name": "혈당/다이어트의 비법 레시피 스크롤", "quantity": 2 }
  ]
}
```

`grade`: `COMMON` | `RARE` | `EPIC`. 동일한 이름의 재료를 이미 보유 중이면 새로 발급하지 않고 `quantity`만 증가한다.

### 오류 응답

없음 (인증 실패 401 제외)

## 4. 데이터 구조

### IngredientCard

`com.elixircodex.backend.stack.IngredientCard`. `POST /api/stack`(가마솥 투입, `STACK_API_SPEC.md` 참고)과 엔티티를 공유한다 — 퀘스트 완료 보상으로 여기서 지급된 카드를 그대로 가마솥에 투입할 수 있다.

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| ownerId | Long | 소유 유저 ID (`User.id`) |
| name | String | 재료 이름(현재 전부 임시 placeholder) |
| grade | Grade (COMMON/RARE/EPIC, `com.elixircodex.backend.stack.Grade`) | 등급 |
| sourceQuestTitle | String | 획득한 퀘스트 제목 |
| quantity | int | 보유 수량 (기본값 1) |
| createdAt | LocalDateTime | 생성 시각 |

### RecipeScroll

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| user | User (FK) | 소유 유저 |
| name | String | 스크롤 이름(카테고리별 고정) |
| quantity | int | 보유 수량 |
| createdAt | LocalDateTime | 생성 시각 |

## 5. 현재 범위 밖 기능

- **재료 이름 확정**: 재료 이름은 전부 임시 placeholder다. 기획팀의 "영양제 대표 성분-시너지-스탯 매핑" 결과가 나오면 교체 필요.
- **인벤토리=도감 통합 여부**: 팀 내 논의 중인 사안으로 아직 결정되지 않음.
