# 마법사의 방 꾸미기 API 명세서

- 문서 버전: 1.0
- 작성일: 2026-08-18
- 구현 기준 브랜치: develop
- 적용 범위: `GET /api/wizard-room`(내 방 조회), `PUT /api/wizard-room`(배치 갱신), `PUT /api/wizard-room/visibility`(공개 설정 갱신), `GET /api/wizard-room/{ownerId}`(공개된 타인의 방 조회) — 출석체크로 지급되는 `FurnitureReward`(`ATTENDANCE_API_SPEC.md` 참고)를 방에 배치·공개하는 기능

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`Content-Type: application/json`

### 1.3 인증 규칙

`Authorization: Bearer <JWT>` 헤더가 필수다. 토큰이 없거나 무효하면 `401 { "message": "인증이 필요합니다" }`. `ownerId`는 요청에 포함하지 않으며, 토큰의 이메일로 서버가 직접 조회한다 (`AuthenticatedUserService.getCurrentUserId()`). 토큰은 유효한데 매칭되는 User가 없으면 `401 { "message": "인증된 사용자를 찾을 수 없습니다" }`. `GET /api/wizard-room/{ownerId}`(타인의 방 조회)도 로그인 자체는 필요하지만, 조회 대상이 요청자 본인일 필요는 없다.

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

`com.elixircodex.backend.common.GlobalExceptionHandler`(`@RestControllerAdvice`)에서 전역으로 처리한다. 처리되지 않은 예외는 `500 { "message": "서버 오류가 발생했습니다" }`로 응답한다.

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 내 방 조회 | GET | `/api/wizard-room` | 200 OK |
| 배치 갱신 | PUT | `/api/wizard-room` | 200 OK |
| 공개 설정 갱신 | PUT | `/api/wizard-room/visibility` | 200 OK |
| 타인의 공개된 방 조회 | GET | `/api/wizard-room/{ownerId}` | 200 OK |

## 3. GET /api/wizard-room

로그인한 유저 본인의 가구 배치 목록과 공개 여부를 함께 반환한다.

### 요청

`GET /api/wizard-room` (파라미터 없음, `ownerId`는 JWT에서 추출)

### 처리 로직 (WizardRoomService.getMyRoom() 구현 기준)

1. `WizardRoomPlacementRepository.findByOwnerId(ownerId)`로 배치 목록을 조회한다 (없으면 빈 배열)
2. `WizardRoomSettingsRepository.findByOwnerId(ownerId)`로 공개 설정을 조회한다. 설정 자체가 한 번도 저장된 적 없으면(한 번도 `PUT /api/wizard-room/visibility`를 호출하지 않은 유저) `isPublic=false`로 취급한다

### 성공 응답

`200 OK` (실제 호출로 확인)

```json
{
  "placements": [
    { "furnitureRewardId": 3, "x": 100, "y": 200, "rotation": 15, "zIndex": 1 }
  ],
  "isPublic": false
}
```

배치한 가구가 없으면 `placements`는 빈 배열이다.

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |

## 4. PUT /api/wizard-room

가구 배치를 통째로 덮어쓴다 (부분 수정이 아니라 전체 교체).

### 요청

`PUT /api/wizard-room`

```json
{
  "placements": [
    { "furnitureRewardId": 1, "x": 100, "y": 200, "rotation": 15, "zIndex": 1 },
    { "furnitureRewardId": 2, "x": 300, "y": 150, "rotation": 0, "zIndex": 2 }
  ]
}
```

| 필드 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| placements | List\<PlacementItem\> | Y | 빈 배열이면 배치를 전부 비운다 |
| placements[].furnitureRewardId | Long | Y | 요청자 본인이 소유한 `FurnitureReward`의 id여야 함 (아래 오류 응답 참고) |
| placements[].x / y | int | Y | 배치 좌표 |
| placements[].rotation | int | Y | 회전 각도 |
| placements[].zIndex | int | Y | 렌더링 순서 |

### 처리 로직 (WizardRoomService.updatePlacements() 구현 기준)

1. 요청에 포함된 `placements` 전체를 순회하며, 각 `furnitureRewardId`가 실제로 존재하고 그 `FurnitureReward.ownerId`가 요청자와 같은지 확인한다. 하나라도 아니면(존재하지 않거나 타인 소유) 즉시 400 — 이 검증에서 하나라도 걸리면 기존 배치는 전혀 건드리지 않는다(원자적으로 전부 성공하거나 전부 실패)
2. 검증을 통과하면 `WizardRoomPlacementRepository.deleteByOwnerId(ownerId)`로 기존 배치를 전부 삭제한다
3. 요청받은 `placements`를 그대로 새 `WizardRoomPlacement` 행으로 저장한다

### 성공 응답

`200 OK`, 빈 본문 (실제 호출로 확인)

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 400 | `furnitureRewardId`가 존재하지 않거나 요청자 소유가 아님 — 실제 호출로 확인 | `{ "message": "본인이 받은 가구만 배치할 수 있습니다" }` |
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |

## 5. PUT /api/wizard-room/visibility

방 공개 여부를 갱신한다. `WizardRoomSettings`가 없으면 새로 만들고, 있으면 갱신한다(upsert).

### 요청

`PUT /api/wizard-room/visibility`

```json
{ "isPublic": true }
```

| 필드 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| isPublic | boolean | Y | true면 공개, false면 비공개 |

### 성공 응답

`200 OK`, 빈 본문 (실제 호출로 확인)

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |

## 6. GET /api/wizard-room/{ownerId}

다른 유저의 공개된 방을 조회한다. 요청자가 그 방의 주인인지는 확인하지 않는다 — 공개(`isPublic=true`) 여부만 확인한다.

### 요청

`GET /api/wizard-room/2`

| 경로 변수 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| ownerId | Long | Y | 조회 대상 유저 id. 공개 설정이 없거나 비공개면 400 (아래 오류 응답 참고) |

### 처리 로직 (WizardRoomService.getPublicRoom() 구현 기준)

1. `WizardRoomSettingsRepository.findByOwnerId(ownerId)`로 대상 유저의 공개 설정을 조회한다. 없으면 400
2. 설정은 있지만 `isPublic=false`면 400
3. 둘 다 통과하면 `WizardRoomPlacementRepository.findByOwnerId(ownerId)` 결과를 그대로 반환한다

### 성공 응답

`200 OK` (실제 호출로 확인)

```json
[
  { "furnitureRewardId": 3, "x": 100, "y": 200, "rotation": 15, "zIndex": 1 }
]
```

### 오류 응답

| 상태 | 조건 | 응답 본문 |
|---|---|---|
| 400 | 대상 유저의 `WizardRoomSettings`가 존재하지 않음 — 실제 호출로 확인 | `{ "message": "공개되지 않은 방입니다" }` |
| 400 | 대상 유저의 `WizardRoomSettings.isPublic`이 false — 실제 호출로 확인 | `{ "message": "공개되지 않은 방입니다" }` |
| 400 | `ownerId`에 Long으로 변환할 수 없는 값 전달 (`MethodArgumentTypeMismatchException`) | `{ "message": "파라미터 'ownerId'의 값이 올바르지 않습니다" }` |
| 401 | 인증 토큰 없음/무효 | `{ "message": "인증이 필요합니다" }` |
| 401 | 토큰은 유효하나 매칭되는 User 없음 | `{ "message": "인증된 사용자를 찾을 수 없습니다" }` |

두 오류 조건(설정 없음 / 비공개)을 동일한 메시지로 통일한 이유는 다른 리소스 존재 여부 은닉 컨벤션(`SYNTHESIZE_CODEX_API_SPEC.md`의 코덱스 상세 조회 소유권 검증 참고)과 같다 — "이 유저가 방을 아예 안 만들었는지" vs "만들었는데 비공개인지"를 외부에 구분해서 알려줄 이유가 없다.

## 7. 데이터 구조

### WizardRoomPlacement

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| ownerId | Long | 소유 유저 ID |
| furnitureRewardId | Long | 배치된 `com.elixircodex.backend.attendance.FurnitureReward`의 id |
| x | int | 배치 좌표 X |
| y | int | 배치 좌표 Y |
| rotation | int | 회전 각도 |
| zIndex | int | 렌더링 순서 |

### WizardRoomSettings

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| ownerId | Long | 소유 유저 ID (unique — 유저당 설정 1행) |
| isPublic | boolean | 방 공개 여부 (기본값 false) |

### PlacementItem (요청/응답 공통 항목)

| 필드 | 타입 | 설명 |
|---|---|---|
| furnitureRewardId | Long | `FurnitureReward` id |
| x / y | int | 배치 좌표 |
| rotation | int | 회전 각도 |
| zIndex | int | 렌더링 순서 |

### WizardRoomResponse (GET /api/wizard-room 응답)

| 필드 | 타입 | 설명 |
|---|---|---|
| placements | List\<PlacementItem\> | 내 배치 목록 |
| isPublic | boolean | 내 방 공개 여부 |

## 8. 현재 범위 밖 기능

- 가구 배치 좌표/회전 값에 대한 유효성 검증 (음수, 화면 밖 좌표, 겹침 등 클라이언트가 알아서 처리한다고 가정)
- `furnitureRewardId` 중복 배치 방지 (같은 가구를 여러 위치에 동시에 배치하는 것을 막지 않음 — `WizardRoomPlacement`와 `FurnitureReward`는 1:N 관계로 설계됨)
- 방 배경/테마 등 가구 배치 외의 꾸미기 요소
- 타인의 공개된 방에 대한 좋아요/댓글 등 소셜 기능
- `GET /api/wizard-room/{ownerId}`에서 방문 기록·조회수 집계
- 방 공개 설정 변경 이력 저장 (현재는 `WizardRoomSettings` 1행을 계속 덮어쓰기만 함)
