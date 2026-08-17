# 유저 인증 API 명세서

- 문서 버전: 1.1
- 작성일: 2026-08-14
- 구현 기준 브랜치: develop
- 적용 범위: 회원가입, 로그인, 로그아웃, 내 정보 조회

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`Content-Type: application/json`

### 1.3 인증 규칙

로그인 API로 발급받은 JWT를 `Authorization: Bearer {token}` 헤더에 담아 요청한다. `/api/users/signup`, `/api/users/login`을 제외한 모든 API는 인증이 필요하다.

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

이 형식은 `com.elixircodex.backend.common.GlobalExceptionHandler`(`@RestControllerAdvice`, backend-1/backend-2 공용)에서 전역으로 일괄 처리된다. 처리되지 않은 예외는 `500 { "message": "서버 오류가 발생했습니다" }`로 응답한다. 인증/인가 실패(401/403)는 Spring Security의 `SecurityConfig`에서 같은 형식으로 별도 처리된다.

## 2. API 목록

| 기능 | Method | URL | 인증 필요 | 성공 상태 |
|---|---|---|---|---|
| 회원가입 | POST | `/api/users/signup` | X | 200 OK |
| 로그인 | POST | `/api/users/login` | X | 200 OK |
| 로그아웃 | POST | `/api/users/logout` | O | 200 OK |
| 내 정보 조회 | GET | `/api/users/me` | O | 200 OK |

## 3. API 상세

### 3.1 회원가입 — `POST /api/users/signup`

**요청**
```json
{
  "email": "user@example.com",
  "password": "password1234",
  "selectedCategory": "혈당/다이어트"
}
```

| 필드 | 타입 | 필수 | 제약 조건 |
|---|---|---|---|
| email | String | Y | 중복 불가 |
| password | String | Y | 저장 시 BCrypt로 암호화 |
| selectedCategory | String | Y | `피부/항산화` / `피로/에너지` / `혈당/다이어트` / `수면/휴식` 중 하나여야 주간 퀘스트 배정이 정상 동작함(QUEST_API_SPEC.md 참고). 가입 시점에는 형식 검증하지 않는 자유 문자열 |

**성공 응답 (200)**
```json
"회원가입이 완료되었습니다."
```

**오류 응답**

| 상태 | 조건 | 메시지 |
|---|---|---|
| 400 | 이미 가입된 이메일 | 이미 가입된 이메일입니다. |

---

### 3.2 로그인 — `POST /api/users/login`

**요청**
```json
{ "email": "user@example.com", "password": "password1234" }
```

**성공 응답 (200)**
```json
{ "token": "eyJhbGciOi...", "tokenType": "Bearer" }
```
토큰 유효기간: 24시간(HS256 서명)

**오류 응답**

| 상태 | 조건 | 메시지 |
|---|---|---|
| 400 | 가입되지 않은 이메일 | 가입되지 않은 이메일입니다. |
| 400 | 비밀번호 불일치 | 비밀번호가 일치하지 않습니다. |

---

### 3.3 로그아웃 — `POST /api/users/logout`

**요청**: 바디 없음. `Authorization: Bearer {token}` 헤더 필수.

**성공 응답 (200)**
```json
"로그아웃되었습니다."
```

JWT는 stateless라 토큰 자체를 서버에서 강제로 무효화할 수 없다. 로그아웃 시 해당 토큰을 `BlacklistedToken` 테이블에 원래 만료 시각까지 등록해두고, 이후 모든 요청에서 `JwtAuthenticationFilter`가 서명·만료뿐 아니라 블랙리스트 등재 여부까지 확인한다. 블랙리스트에 등재된 토큰으로는 인증되지 않은 요청으로 처리된다.

**오류 응답**

| 상태 | 조건 | 메시지 |
|---|---|---|
| 400 | `Authorization` 헤더가 `Bearer `로 시작하지 않음 | Authorization 헤더 형식이 올바르지 않습니다. |
| 400 | 유효하지 않거나 만료된 토큰 | 유효하지 않은 토큰입니다. |
| 401 | `Authorization` 헤더 자체가 없음 | 인증이 필요합니다. |

---

### 3.4 내 정보 조회 — `GET /api/users/me`

JWT 인증 필터 동작 확인용 API. 토큰의 subject(이메일)를 그대로 반환한다.

**성공 응답 (200)**
```json
"user@example.com"
```

**오류 응답**

| 상태 | 조건 | 메시지 |
|---|---|---|
| 401 | 토큰 없음/유효하지 않음/로그아웃된 토큰 | 인증이 필요합니다. |

## 4. 데이터 구조

### User

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| email | String | 이메일(로그인 ID) |
| password | String | BCrypt 암호화된 비밀번호 |
| selectedCategory | String | 주간 퀘스트 카테고리 선택값 |
| subscriptionStatus | String | 구독 상태 (가입 시 `FREE` 고정) |
| healthDataEnabled | boolean | Health 데이터 연동 권한 (기본 false) |

### BlacklistedToken

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| token | String | 로그아웃 처리된 JWT 원문 |
| expiresAt | LocalDateTime | 토큰 원래 만료 시각(이 시각까지만 블랙리스트에 유지할 필요가 있음) |

## 5. 현재 범위 밖 기능

- **비밀번호 재설정/이메일 인증**: 미구현.
- **회원 탈퇴**: 미구현.
- **`selectedCategory` 변경 API**: 가입 이후 카테고리를 바꿀 방법이 없음.
- **블랙리스트 정리(cleanup)**: 만료된 `BlacklistedToken` 로우를 지우는 배치/스케줄러 없음. 로그아웃이 누적될수록 테이블이 계속 커진다.
- **리프레시 토큰**: 액세스 토큰만 발급하며 만료 시 재로그인이 필요하다.
