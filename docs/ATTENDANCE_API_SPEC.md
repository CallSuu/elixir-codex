# 출석체크 API 명세서

- 문서 버전: 1.1
- 작성일: 2026-08-15
- 구현 기준 브랜치: develop
- 적용 범위: `POST /api/attendance/check` (출석체크), `GET /api/attendance` (출석 상태 조회)

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`Content-Type: application/json`

### 1.3 인증 규칙

`Authorization: Bearer <JWT>` 헤더가 필수다. 토큰이 없거나 무효하면 `401 { "message": "인증이 필요합니다" }`. `ownerId`는 더 이상 쿼리 파라미터로 받지 않으며, 토큰의 이메일로 서버가 직접 조회한다 (`AuthenticatedUserService.getCurrentUserId()`). 토큰은 유효한데 매칭되는 User가 없으면 `401 { "message": "인증된 사용자를 찾을 수 없습니다" }`.

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

`com.elixircodex.backend.common.GlobalExceptionHandler`(`@RestControllerAdvice`)에서 전역으로 처리한다. `AttendanceService`가 던지는 커스텀 예외뿐 아니라, `ownerId` 파라미터 누락이나 타입 불일치처럼 컨트롤러 이전 단계(Spring)에서 발생하는 오류도 모두 동일한 형식으로 통일된다. 처리되지 않은 예외는 `500 { "message": "서버 오류가 발생했습니다" }`로 응답한다.

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 출석체크 | POST | `/api/attendance/check` | 200 OK |
| 출석 상태 조회 | GET | `/api/attendance` | 200 OK |

## 3. POST /api/attendance/check

### 요청

`POST /api/attendance/check` (파라미터 없음, `ownerId`는 JWT에서 추출)

### 처리 로직 (AttendanceService.checkIn() 구현 기준)

1. `ownerId` 기준으로 오늘 날짜(`attendedDate`)의 AttendanceLog가 이미 있으면 400
2. `findFirstByOwnerIdOrderByAttendedDateDesc`로 가장 최근 AttendanceLog를 조회한다. 그 기록의 `attendedDate`가 어제면 `streak = 최근기록.streakAtCheckIn + 1`, 그 외(기록이 없거나 어제가 아님)에는 `streak = 1`
3. 오늘 날짜와 계산된 `streak`으로 AttendanceLog를 저장한다
4. `streak`이 7의 배수(7, 14, 21...)면 FurnitureReward(`itemName="연속 출석 보상 상자"`)를 저장하고 `rewardGranted=true`, 아니면 `rewardGranted=false`

### 성공 응답

`200 OK` (실제 호출로 확인)

보상이 지급되지 않은 경우:

```json
{ "currentStreak": 6, "rewardGranted": false, "rewardItemName": null }
```

연속 7일째라 보상이 지급된 경우:

```json
{ "currentStreak": 7, "rewardGranted": true, "rewardItemName": "연속 출석 보상 상자" }
```

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 400 | 오늘 이미 출석체크를 완료함 — 실제 호출로 확인 | `{ "message": "오늘 이미 출석체크를 완료했습니다" }` |
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |

## 4. GET /api/attendance

### 요청

`GET /api/attendance` (파라미터 없음, `ownerId`는 JWT에서 추출)

### 처리 로직 (AttendanceService.getStatus() 구현 기준)

- `currentStreak`: 가장 최근 AttendanceLog의 `streakAtCheckIn` (기록이 없으면 0)
- `rewards`: `FurnitureRewardRepository.findByOwnerId(ownerId)` 결과 전체

### 성공 응답

`200 OK` (실제 호출로 확인)

```json
{
  "currentStreak": 7,
  "rewards": [
    { "id": 1, "itemName": "연속 출석 보상 상자", "grantedAt": "2026-08-12T00:34:02.373411" }
  ]
}
```

한 번도 출석한 적 없는 유저(실제 호출로 확인):

```json
{ "currentStreak": 0, "rewards": [] }
```

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |

## 5. 데이터 구조

### AttendanceLog

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| ownerId | Long | 소유 유저 ID |
| attendedDate | LocalDate | 출석 날짜 |
| streakAtCheckIn | int | 그 체크인 시점의 연속 출석 일수 |
| createdAt | LocalDateTime | 생성 시각 |

### FurnitureReward

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| ownerId | Long | 소유 유저 ID |
| itemName | String | 보상 아이템 이름 (현재는 "연속 출석 보상 상자" 고정) |
| grantedAt | LocalDateTime | 지급 시각 |

### AttendanceCheckResponse (POST /api/attendance/check 응답)

| 필드 | 타입 | 설명 |
|---|---|---|
| currentStreak | int | 이번 체크인으로 갱신된 연속 출석 일수 |
| rewardGranted | boolean | 이번 체크인으로 보상이 지급됐는지 여부 |
| rewardItemName | String (nullable) | 지급된 보상 아이템 이름, 지급 안 됐으면 null |

### AttendanceStatusResponse / FurnitureRewardSummary (GET /api/attendance 응답)

| 필드 | 타입 | 설명 |
|---|---|---|
| currentStreak | int | 현재 연속 출석 일수 (기록 없으면 0) |
| rewards[].id | Long | FurnitureReward id |
| rewards[].itemName | String | 보상 아이템 이름 |
| rewards[].grantedAt | LocalDateTime | 지급 시각 |

## 6. 현재 범위 밖 기능

- 출석 캘린더 등 과거 출석 기록 전체 조회 API (현재는 최근 연속 일수와 보상 목록만 제공)
- 보상 아이템 종류 다양화 (현재는 "연속 출석 보상 상자" 1종만 고정 지급)
- 서버 타임존 외 유저별 타임존 처리 (`LocalDate.now()`는 서버 시스템 타임존 기준)
