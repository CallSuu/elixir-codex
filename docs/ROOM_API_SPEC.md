# 비밀 서재 배치 API 명세서

- 문서 버전: 1.0
- 작성일: 2026-08-14
- 구현 기준 브랜치: develop
- 적용 범위: `GET /api/room`, `PUT /api/room` (마이페이지 "비밀 서재" 캔버스에 도감 엘릭서 카드를 자유 배치·저장하는 기능, COD-02)

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`Content-Type: application/json`

### 1.3 인증 규칙

현재는 별도 인증 없이 `ownerId`를 쿼리 파라미터로 직접 포함해서 전달한다(backend-1의 `POST /api/stack`과 동일한 패턴). 다만 Spring Security 설정(`SecurityConfig`)이 `/api/users/login`, `/api/users/signup`을 제외한 모든 경로에 유효한 JWT를 요구하므로, 실제 호출 시에는 `Authorization: Bearer {token}` 헤더도 함께 있어야 한다 — 다만 그 토큰의 신원과 `ownerId`가 일치하는지는 검증하지 않는다. 추후 JWT 인증으로 완전히 전환 예정.

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

`com.elixircodex.backend.common.GlobalExceptionHandler`에서 전역 처리(USER_API_SPEC.md 1.4와 동일).

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 배치 목록 조회 | GET | `/api/room?ownerId={ownerId}` | 200 OK |
| 배치 목록 전체 저장(덮어쓰기) | PUT | `/api/room?ownerId={ownerId}` | 200 OK |

## 3. API 상세

### 3.1 배치 목록 조회 — `GET /api/room?ownerId={ownerId}`

**요청**: 바디 없음. `ownerId`(Long) 쿼리 파라미터 필수.

**성공 응답 (200)**
```json
[
  { "elixirCardId": 12, "x": 100, "y": 200, "rotation": 15, "zIndex": 1 },
  { "elixirCardId": 15, "x": 300, "y": 150, "rotation": -5, "zIndex": 2 }
]
```
해당 `ownerId`의 배치가 하나도 없으면 빈 배열 `[]`을 반환한다.

**오류 응답**

| 상태 | 조건 | 메시지 |
|---|---|---|
| 400 | `ownerId` 파라미터 누락 | 필수 파라미터가 누락되었습니다: ownerId |

---

### 3.2 배치 목록 전체 저장 — `PUT /api/room?ownerId={ownerId}`

요청받은 `placements` 배열로 해당 `ownerId`의 기존 배치를 **전부 삭제하고 통째로 교체**한다(부분 수정 API 없음). 캔버스에서 카드를 옮기다가 "저장" 버튼을 누른 시점의 전체 배열을 한 번에 보내는 방식을 전제로 한다.

**요청**
```json
{
  "placements": [
    { "elixirCardId": 12, "x": 100, "y": 200, "rotation": 15, "zIndex": 1 },
    { "elixirCardId": 15, "x": 300, "y": 150, "rotation": -5, "zIndex": 2 }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| placements | List | X (생략/빈 배열이면 전체 삭제로 처리) | 배치할 카드 목록 |
| placements[].elixirCardId | Long | O | `com.elixircodex.backend.alchemy.ElixirCard`의 id. FK 제약 없음, 존재 여부도 검증하지 않음(3.2 하단 참고) |
| placements[].x | int | O | 캔버스 x 좌표 |
| placements[].y | int | O | 캔버스 y 좌표 |
| placements[].rotation | int | O | 회전각 |
| placements[].zIndex | int | O | 쌓는 순서 |

**성공 응답 (200)**: 저장된 배치 목록 전체, 3.1과 동일한 형식

**오류 응답**

| 상태 | 조건 | 메시지 |
|---|---|---|
| 400 | `ownerId` 파라미터 누락 | 필수 파라미터가 누락되었습니다: ownerId |
| 400 | 요청 본문이 유효한 JSON이 아님 | 요청 본문을 읽을 수 없습니다 |

**검증하지 않는 것 (의도적 생략)**: `elixirCardId`가 실제로 존재하는 카드인지, 그 카드가 `ownerId` 소유가 맞는지는 검증하지 않는다. 프론트엔드가 유저 본인 도감에서만 카드를 골라 보내는 것을 전제로 한다.

## 4. 데이터 구조

### RoomCardPlacement

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| ownerId | Long | 소유 유저 ID |
| elixirCardId | Long | 참조하는 엘릭서 카드 ID (FK 제약 없음) |
| x | int | 캔버스 x 좌표 |
| y | int | 캔버스 y 좌표 |
| rotation | int | 회전각 |
| zIndex | int | 쌓는 순서 |

## 5. 현재 범위 밖 기능

- **`elixirCardId` 소유권/존재 검증**: `ElixirCardRepository`를 참조해 실제 존재하는 카드인지, `ownerId` 소유가 맞는지 확인하는 로직은 넣지 않았다. 필요해지면 `com.elixircodex.backend.alchemy.ElixirCardRepository`를 주입해서 추가하면 된다.
- **개별 카드 단위 이동 API**: 현재는 전체 배열을 통째로 덮어쓰는 방식만 있다. 카드 하나 옮길 때마다 API를 부르는 방식으로 바꾸려면 프론트 설계에 영향이 커서 별도 논의 후 결정하기로 함.
- **JWT 신원과 `ownerId` 일치 검증**: 토큰만 유효하면 통과되고, 토큰 주체와 요청한 `ownerId`가 같은 사람인지는 확인하지 않는다.
