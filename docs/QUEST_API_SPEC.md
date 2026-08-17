# 퀘스트 API 명세서

- 문서 버전: 1.2
- 작성일: 2026-08-16
- 구현 기준 브랜치: develop
- 적용 범위: 일일 퀘스트(F-QST-01/02), 주간 퀘스트(F-QST-03)

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`Content-Type: application/json`

### 1.3 인증 규칙

모든 API는 `Authorization: Bearer {token}` 헤더 인증이 필요하다(USER_API_SPEC.md 참고).

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

`com.elixircodex.backend.common.GlobalExceptionHandler`에서 전역 처리(USER_API_SPEC.md 1.4와 동일).

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 일일 퀘스트 조회(없으면 자동 배정) | GET | `/api/quests/daily` | 200 OK |
| Health 데이터 동기화(자동 판정) | POST | `/api/quests/health-sync` | 200 OK |
| 일일 퀘스트 수동 완료 | PATCH | `/api/quests/{dailyQuestId}/complete` | 200 OK |
| 주간 퀘스트 조회(없으면 자동 배정) | GET | `/api/quests/weekly` | 200 OK |
| 주간 퀘스트 완료 | PATCH | `/api/quests/weekly/{weeklyQuestId}/complete` | 200 OK |

## 3. API 상세

### 3.1 일일 퀘스트 조회 — `GET /api/quests/daily`

당일 배정된 퀘스트가 없으면 그 자리에서 새로 배정한 뒤 반환한다(트리거: 최초 접속 시. 00:00 배치는 미구현). 이후 같은 날 재호출 시 동일한 세트를 반환한다.

**성공 응답 (200)**
```json
[
  {
    "dailyQuestId": 1,
    "title": "물 1.5L 마시기",
    "description": "오늘 하루 물 1.5L 이상 마시기",
    "dataSource": "MANUAL",
    "targetValue": null,
    "status": "IN_PROGRESS"
  },
  {
    "dailyQuestId": 2,
    "title": "8,000보 걷기",
    "description": "오늘 하루 8,000보 이상 걷기",
    "dataSource": "STEPS",
    "targetValue": 8000,
    "status": "IN_PROGRESS"
  }
]
```

`dataSource`: `STEPS` | `SLEEP` | `MANUAL`. `status`: `IN_PROGRESS` | `COMPLETED`. Health 권한 없는 유저(`healthDataEnabled=false`)는 `MANUAL` 미션만 배정된다. 같은 회차에 STEPS/SLEEP 데이터소스는 각각 최대 1개만 배정된다(중복 방지).

**오류 응답**: 없음 (인증 실패 401 제외)

---

### 3.2 Health 데이터 동기화 — `POST /api/quests/health-sync`

**요청**
```json
{ "steps": 8500, "sleepHours": 7 }
```
두 필드 모두 `null` 가능(하나만 보내도 됨). 이 API를 호출하는 트리거(포그라운드 진입, 백그라운드 fetch 스케줄링 등)는 프론트엔드 책임이며, 백엔드는 수신 API만 제공한다.

**성공 응답 (200)**: 오늘 퀘스트 전체 목록(3.1과 동일 형식). 기준을 충족한 `STEPS`/`SLEEP` 퀘스트는 `COMPLETED`로 전환되고 재료 카드가 자동 지급된다. `MANUAL` 퀘스트는 이 API로 완료되지 않는다.

**오류 응답**: 없음 (인증 실패 401 제외)

---

### 3.3 일일 퀘스트 수동 완료 — `PATCH /api/quests/{dailyQuestId}/complete`

`MANUAL` 타입 퀘스트만 이 API로 완료 처리할 수 있다.

**성공 응답 (200)**
```json
{
  "dailyQuestId": 1,
  "title": "물 1.5L 마시기",
  "description": "오늘 하루 물 1.5L 이상 마시기",
  "dataSource": "MANUAL",
  "targetValue": null,
  "status": "COMPLETED"
}
```

**오류 응답 (모두 400)**

| 조건 | 메시지 |
|---|---|
| 존재하지 않는 퀘스트 | 존재하지 않는 퀘스트입니다. |
| 타인의 퀘스트 | 본인의 퀘스트만 완료 처리할 수 있습니다. |
| STEPS/SLEEP 퀘스트를 수동 완료 시도 | 자동 판정 대상 퀘스트는 수동으로 완료할 수 없습니다. |
| 이미 완료된 퀘스트 | 이미 완료된 퀘스트입니다. |

**보상**: 완료 시 일반(Common) 등급 재료 카드 1개 자동 지급, 15% 확률로 희귀(Rare) 등급으로 승급. 동일한 재료를 이미 보유 중이면 수량만 증가한다.

---

### 3.4 주간 퀘스트 조회 — `GET /api/quests/weekly`

이번 주(월요일 기준) 배정된 퀘스트가 없으면, 유저의 `selectedCategory`에 매핑된 미션 풀에서 3개를 새로 배정한다.

**성공 응답 (200)**
```json
[
  {
    "weeklyQuestId": 1,
    "title": "주 5회 10,000보 걷기",
    "description": "이번 주 5일 이상 10,000보 이상 걷기",
    "category": "BLOOD_SUGAR_DIET",
    "status": "IN_PROGRESS"
  }
]
```

`category`(backend-1의 `ThemeCategory`와 동일 체계): `SKIN_ANTIOXIDANT`(피부/항산화) | `FATIGUE_ENERGY`(피로/에너지) | `BLOOD_SUGAR_DIET`(혈당/다이어트) | `SLEEP_REST`(수면/휴식)

**오류 응답**

| 상태 | 조건 | 메시지 |
|---|---|---|
| 400 | `selectedCategory`가 4개 카테고리 라벨과 일치하지 않음 | 올바르지 않은 카테고리입니다: {값} |

---

### 3.5 주간 퀘스트 완료 — `PATCH /api/quests/weekly/{weeklyQuestId}/complete`

전부 수동 체크 방식(자동판정 없음). 누적/연속일수 자동 추적은 범위 밖.

**성공 응답 (200)**: 3.4와 동일한 단일 객체 형식, `status: "COMPLETED"`

**오류 응답 (모두 400)**: 존재하지 않음 / 본인 아님 / 이미 완료됨 — 3.3과 동일한 메시지 패턴

**보상**: 완료 시 재료 카드 2개가 지급된다 — ① `rewardMaterialName`, 템플릿마다 고정된 희귀~에픽(Rare~Epic) 등급, ② `commonRewardMaterialName`, 일반(Common) 등급. 두 재료 모두 같은 카테고리의 확정 재료 목록(아래 4절 참고) 중에서 서로 다른 것으로 채워져 있다(동일 카테고리 내 재료 수가 부족한 일부 조합만 예외적으로 겹칠 수 있음). 이미 보유 중인 재료는 새로 만들지 않고 수량만 증가한다(3.3과 동일한 방식, 재료별로 각각 판단). 레시피 스크롤은 더 이상 지급되지 않는다 — `RecipeScroll` 엔티티/테이블과 인벤토리 조회 API 자체는 남아 있지만, 이 완료 로직에서는 제외됐다.

## 4. 데이터 구조

| Enum/타입 | 값 |
|---|---|
| `QuestDataSource` | `STEPS`, `SLEEP`, `MANUAL` |
| `QuestStatus` | `IN_PROGRESS`, `COMPLETED` |
| `QuestCategory` | `SKIN_ANTIOXIDANT`(피부/항산화), `FATIGUE_ENERGY`(피로/에너지), `BLOOD_SUGAR_DIET`(혈당/다이어트), `SLEEP_REST`(수면/휴식) — backend-1의 `ThemeCategory`와 동일 체계 |
| `Grade`(`com.elixircodex.backend.stack.Grade`) | `COMMON`, `RARE`, `EPIC` |

| 엔티티 | 주요 필드 |
|---|---|
| `QuestTemplate` | id, title, description, dataSource, targetValue, rewardMaterialName |
| `DailyQuest` | id, user, questTemplate, assignedDate, status |
| `WeeklyQuestTemplate` | id, title, description, category, rewardGrade, rewardMaterialName, commonRewardMaterialName, recipeScrollName |
| `WeeklyQuest` | id, user, weeklyQuestTemplate, weekStartDate, status |

`commonRewardMaterialName`은 이번 문서 버전에서 신규 추가된 필드로, 3.5절 보상의 두 번째 재료(Common 등급 고정)를 지정한다. `recipeScrollName` 필드 자체는 엔티티에 남아 있지만 3.5절 보상 로직에서는 더 이상 참조되지 않는다.

퀘스트 완료 보상으로 지급되는 재료 카드는 `com.elixircodex.backend.stack.IngredientCard`(`ownerId`, `name`, `grade`, `sourceQuestTitle`, `quantity`, `createdAt`)를 그대로 사용한다. `POST /api/stack`(가마솥 투입)과 엔티티를 공유하므로 여기서 지급된 카드를 그대로 스택 조합에 투입할 수 있다(INVENTORY_API_SPEC.md, STACK_API_SPEC.md 참고).

### 확정 재료 목록 (기획팀 확정, 테마별 4개씩 총 15개)

| 카테고리 | 재료 |
|---|---|
| 피부/항산화 (`SKIN_ANTIOXIDANT`) | 이슬 한 방울, 탱탱 젤리, 황금 레몬, 백옥 진주 |
| 피로/에너지 (`FATIGUE_ENERGY`) | 활력초, 천년 뿌리, 마룡 뿔, 심장 태엽 |
| 혈당/다이어트 (`BLOOD_SUGAR_DIET`) | 홀쭉 열매, 바나바잎, 녹차잎, 포만 이끼 |
| 수면/휴식 (`SLEEP_REST`) | 평온초, 안정석, 해독 엉겅퀴 |

일일 퀘스트 10개(`QuestDataInitializer`)는 이 15개 중 10개를 테마 구분 없이 겹치지 않게 배분한 값을 그대로 사용한다. 주간 퀘스트(`WeeklyQuestDataInitializer`)는 각 템플릿의 `category`와 같은 카테고리의 재료로만 `rewardMaterialName`/`commonRewardMaterialName`을 채운다(수면/휴식은 재료가 3개뿐이라 카테고리 내에서 순환 배정됨).

## 5. 현재 범위 밖 기능

- **00:00 배치를 통한 일일 퀘스트 사전 생성**: 현재는 "최초 접속 시 생성" 방식만 구현. 배치 스케줄러 미구현.
- **주간 퀘스트 카테고리 변경 시 재계산**: `selectedCategory`를 바꾸는 API 자체가 없어 트리거 구현 불가 상태.
- **일일 퀘스트 보상 등급 확률**: 15% 고정값으로 하드코딩. 운영 파라미터화되어 있지 않음.
