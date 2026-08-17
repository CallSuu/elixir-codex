# 영양제 섭취 인증 API 명세서

- 문서 버전: 2.1
- 작성일: 2026-08-15
- 구현 기준 브랜치: develop
- 적용 범위: 영양제 섭취 인증(F-QST-04 / MAT-02)

> backend-1의 동일 기능(`com.elixircodex.backend.stack.SupplementController`)으로 통합됐다. 상세 요청/응답/오류 스펙은 `SUPPLEMENT_VERIFICATION_API_SPEC.md`가 더 자세히 다루며, 이 문서는 팀 기획 관점(F-QST-04/MAT-02)에서의 요약이다. 두 문서가 다르면 실제 코드 기준으로 `SUPPLEMENT_VERIFICATION_API_SPEC.md`를 신뢰할 것.

## 1. 공통 사항

### 1.1 기본 URL

```
http://localhost:8080
```

### 1.2 요청 및 응답 형식

`POST /api/supplements/verify`는 `multipart/form-data`, 그 외는 `application/json`.

### 1.3 인증 규칙

`Authorization: Bearer <JWT>` 헤더가 필수다. `ownerId`는 더 이상 요청에 포함하지 않으며, 토큰에서 서버가 직접 추출한다 (`AuthenticatedUserService`). 상세 오류 조건은 `SUPPLEMENT_VERIFICATION_API_SPEC.md` 참고.

### 1.4 공통 오류 응답 형식

```json
{ "message": "오류 메시지" }
```

`com.elixircodex.backend.common.GlobalExceptionHandler`에서 전역 처리한다.

## 2. API 목록

| 기능 | Method | URL | 성공 상태 |
|---|---|---|---|
| 영양제 섭취 인증 | POST | `/api/supplements/verify` | 200 OK |
| 인증 기록 조회 | GET | `/api/supplements` | 200 OK |

## 3. API 상세

### 3.1 영양제 섭취 인증 — `POST /api/supplements/verify`

`multipart/form-data` 요청 (`ownerId`는 JWT에서 추출하므로 폼 필드로 보내지 않음).

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `image` | file | O | 인증할 영양제 사진 |

GPT-4o Vision으로 사진 속 제품명을 인식하고, 신뢰도(0~100)를 산출해 운영 파라미터(`supplement.ocr.confidence-threshold`, `application.properties`, 기본 70)와 비교한다. 임계값 이상이면 `isVerified: true`. `isAffiliateProduct`는 (기획 당시와 달리) 클라이언트가 지정하는 게 아니라, 인식된 제품명에 `supplement.affiliate-products` 목록의 브랜드가 포함되는지로 서버가 자동 판별한다. 이미지 파일 자체는 서버에 저장하지 않고 인증 결과만 `SupplementLog`에 기록한다.

**성공 응답 (200)**
```json
{
  "supplementLogId": 1,
  "productName": "Vitamin C 1000mg Daily Immune Support",
  "confidenceScore": 90,
  "isVerified": true,
  "isAffiliateProduct": true
}
```
인식 실패(제품이 아니거나 라벨이 안 보임) 시 `productName`은 빈 문자열, `confidenceScore`는 낮게 나오며 `isVerified: false`.

**오류 응답**

| 상태 | 조건 | 메시지 |
|---|---|---|
| 400 | 이미지 미첨부(폼 파트 자체 누락) | 필수 파라미터 'image'이(가) 누락되었습니다 |
| 400 | 첨부된 이미지 내용이 비어 있음 | 이미지가 필요합니다 |
| 400 | 이미지 파일을 읽을 수 없음 | 이미지를 읽을 수 없습니다 |
| 502 | GPT-4o Vision 호출/응답 파싱 실패 | 영양제 인증 요청이 실패했습니다 / 영양제 인증 응답을 해석하지 못했습니다 등 |

---

### 3.2 인증 기록 조회 — `GET /api/supplements`

**요청**: 파라미터/바디 없음 (`ownerId`는 JWT에서 추출).

**성공 응답 (200)**: 3.1과 동일한 형태의 객체 배열, 최신순 정렬.

**오류 응답**: 인증 토큰 없음/무효 시 401 (그 외 없음).

## 4. 데이터 구조

### SupplementLog

`com.elixircodex.backend.stack.SupplementLog`. `POST /api/stack`(가마솥 투입, `STACK_API_SPEC.md` 참고)이 이 로그를 자동으로 조회해 소비한다.

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| ownerId | Long | 소유 유저 ID (`User.id`) |
| productName | String | GPT-4o Vision이 인식한 제품명 |
| confidenceScore | Integer (nullable) | 인식 신뢰도(0~100). 이 필드 도입 이전에 생성된 기록은 null |
| isAffiliateProduct | boolean | 제휴 제품 여부(서버 자동 판별) |
| isVerified | boolean | 신뢰도가 임계값 이상인지 여부 |
| consumedDate | LocalDate | 섭취(인증) 날짜 |
| createdAt | LocalDateTime | 생성 시각 |

## 5. 현재 범위 밖 기능

- **이미지 저장/재조회**: 원본 이미지를 저장하지 않으므로 인증 당시 사진을 다시 확인할 방법이 없다(스토리지 인프라 없어서 의도적으로 생략).
