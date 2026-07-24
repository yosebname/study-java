# 🔌 북로그(BookLog) API 명세 초안 v1

> **웹(Next.js)과 앱(Android)이 동일하게 사용하는 계약서.**
> 이 문서가 확정되면 백엔드·프론트·안드로이드 3트랙이 **서로를 기다리지 않고** 병렬로 개발할 수 있습니다.
> 16주차에 Swagger UI를 붙이면 이 문서는 자동 생성본으로 대체됩니다.

- Base URL(로컬): `http://localhost:8080`
- 에뮬레이터에서: `http://10.0.2.2:8080` · 실기기에서: `http://<PC의 LAN IP>:8080`
- 모든 경로 접두사: **`/api/v1`**
- 요청/응답 형식: `application/json; charset=UTF-8`

---

## 1. 공통 규약

### 1-1. 인증

```
Authorization: Bearer {accessToken}
```
- 인증 불필요: `POST /auth/signup`, `POST /auth/login`, `POST /auth/reissue`
- 그 외 전부 필요

### 1-2. 성공 응답 — 데이터를 그대로 반환 (envelope 없음)

```json
// GET /api/v1/records/1
{ "id": 1, "status": "READING", "book": { "id": 7, "title": "클린 코드" } }
```

### 1-3. 에러 응답 — RFC 7807 ProblemDetail (Spring 6+ 내장) ⭐

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "독서 기록을 찾을 수 없습니다. id=99",
  "instance": "/api/v1/records/99",
  "code": "RECORD_NOT_FOUND",
  "errors": null
}
```

검증 실패(400)일 때:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "입력값이 올바르지 않습니다.",
  "code": "VALIDATION_FAILED",
  "errors": [
    { "field": "title", "reason": "제목은 필수입니다." },
    { "field": "rating", "reason": "1에서 5 사이여야 합니다." }
  ]
}
```

> 💡 Spring Framework 6+는 `ProblemDetail`을 표준으로 지원합니다. 옛날 강의의 `Map<String,Object>` 직접 만들기 대신 이걸 쓰는 게 2026년 방식입니다.

### 1-4. 에러 코드 표

| code | HTTP | 의미 |
|------|------|------|
| `VALIDATION_FAILED` | 400 | 입력값 검증 실패 |
| `INVALID_STATUS_TRANSITION` | 400 | 잘못된 상태 전이 (예: DONE → READING) |
| `UNAUTHORIZED` | 401 | 토큰 없음/만료 |
| `ACCESS_DENIED` | 403 | 남의 기록 접근 |
| `RECORD_NOT_FOUND` / `BOOK_NOT_FOUND` / `NOTE_NOT_FOUND` | 404 | |
| `DUPLICATE_RECORD` | 409 | 같은 책 중복 등록 |
| `DUPLICATE_EMAIL` | 409 | 이메일 중복 |
| `INTERNAL_ERROR` | 500 | |

### 1-5. 페이징 규약

요청: `?page=0&size=20&sort=createdAt,desc` (Spring Pageable 기본 형식)

```json
{
  "content": [ /* ... */ ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "first": true,
  "last": false
}
```

> ⚠️ Spring `Page`를 그대로 반환하면 응답 구조가 지저분하고 버전에 따라 바뀝니다. **위 형태의 `PageResponse<T>` record를 직접 만들어 감싸세요.** (프론트/안드로이드가 파싱하기 훨씬 쉬워집니다.)

### 1-6. 날짜 형식

- 날짜: `"2026-07-24"` (ISO-8601 `LocalDate`)
- 일시: `"2026-07-24T15:30:00"` (`LocalDateTime`)

---

## 2. 엔드포인트 전체 목록

| # | Method | Path | 설명 | 인증 | 구현 주차 |
|---|--------|------|------|------|-----------|
| **인증** |
| 1 | POST | `/api/v1/auth/signup` | 회원가입 | ❌ | 21 |
| 2 | POST | `/api/v1/auth/login` | 로그인 (토큰 발급) | ❌ | 21 |
| 3 | POST | `/api/v1/auth/reissue` | 액세스 토큰 재발급 | ❌ | 22 |
| 4 | POST | `/api/v1/auth/logout` | 로그아웃 | ⭕ | 22 |
| 5 | GET | `/api/v1/members/me` | 내 정보 | ⭕ | 21 |
| **책** |
| 6 | GET | `/api/v1/books` | 책 검색 (title/author) | ⭕ | 16 |
| 7 | GET | `/api/v1/books/{id}` | 책 상세 | ⭕ | 16 |
| 8 | POST | `/api/v1/books` | 책 등록 (수동) | ⭕ | 16 |
| **독서 기록 (핵심)** |
| 9 | GET | `/api/v1/records` | 내 기록 목록 (status 필터·페이징) | ⭕ | 16 |
| 10 | GET | `/api/v1/records/{id}` | 기록 상세 (세션·메모 포함) | ⭕ | 17 |
| 11 | POST | `/api/v1/records` | 책을 내 서재에 추가 | ⭕ | 16 |
| 12 | PATCH | `/api/v1/records/{id}/status` | 상태 변경 | ⭕ | 17 |
| 13 | PATCH | `/api/v1/records/{id}/review` | 별점·리뷰 작성 | ⭕ | 18 |
| 14 | DELETE | `/api/v1/records/{id}` | 기록 삭제 | ⭕ | 16 |
| **독서 세션** |
| 15 | GET | `/api/v1/records/{id}/sessions` | 세션 목록 | ⭕ | 18 |
| 16 | POST | `/api/v1/records/{id}/sessions` | 오늘 읽은 기록 추가 | ⭕ | 18 |
| 17 | DELETE | `/api/v1/sessions/{sessionId}` | 세션 삭제 | ⭕ | 18 |
| **메모/인용** |
| 18 | GET | `/api/v1/records/{id}/notes` | 메모 목록 | ⭕ | 18 |
| 19 | POST | `/api/v1/records/{id}/notes` | 메모 작성 | ⭕ | 18 |
| 20 | PUT | `/api/v1/notes/{noteId}` | 메모 수정 | ⭕ | 19 |
| 21 | DELETE | `/api/v1/notes/{noteId}` | 메모 삭제 | ⭕ | 19 |
| **통계 & 목표** ⭐ |
| 22 | GET | `/api/v1/stats/summary` | 요약 (총 권수·페이지·시간) | ⭕ | 29 |
| 23 | GET | `/api/v1/stats/monthly?year=2026` | 월별 완독 수 | ⭕ | 29 |
| 24 | GET | `/api/v1/goals/{year}` | 연간 목표 조회 | ⭕ | 29 |
| 25 | PUT | `/api/v1/goals/{year}` | 연간 목표 설정 | ⭕ | 29 |

> 📌 **구현 주차 순서를 지키세요.** 9·11·14번(목록/추가/삭제)이 16주차에 먼저 돌아야 웹·앱이 붙을 수 있습니다.

---

## 3. 주요 엔드포인트 상세

### 🔐 POST `/api/v1/auth/signup`

```json
// Request
{ "email": "me@test.com", "password": "Password1!", "nickname": "책벌레" }

// 201 Created
{ "id": 1, "email": "me@test.com", "nickname": "책벌레" }
```
| 검증 | 규칙 |
|------|------|
| email | `@Email`, 필수, 중복 시 **409 `DUPLICATE_EMAIL`** |
| password | 8자 이상, 영문+숫자+특수문자 |
| nickname | 2~30자 |

---

### 🔐 POST `/api/v1/auth/login`

```json
// Request
{ "email": "me@test.com", "password": "Password1!" }

// 200 OK
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```
- 실패 시 **401 `UNAUTHORIZED`** — "이메일 또는 비밀번호가 올바르지 않습니다."
- ⚠️ **"이메일이 없습니다"와 "비밀번호가 틀렸습니다"를 구분하지 마세요.** 계정 존재 여부가 새어나갑니다. (보안 학습 포인트)

---

### 📚 GET `/api/v1/records`

**Query Parameters**

| 이름 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `status` | enum | (전체) | `WANT_TO_READ` / `READING` / `DONE` / `PAUSED` |
| `keyword` | string | — | 책 제목·저자 검색 |
| `page` | int | 0 | |
| `size` | int | 20 | |
| `sort` | string | `createdAt,desc` | |

```json
// 200 OK
{
  "content": [
    {
      "id": 12,
      "status": "READING",
      "currentPage": 137,
      "progressRate": 42.3,
      "startDate": "2026-07-10",
      "endDate": null,
      "rating": null,
      "book": {
        "id": 7,
        "title": "클린 코드",
        "author": "로버트 C. 마틴",
        "coverUrl": "https://.../clean-code.jpg",
        "pageCount": 324
      }
    }
  ],
  "page": 0, "size": 20, "totalElements": 37, "totalPages": 2,
  "first": true, "last": false
}
```

> 💡 `progressRate`는 **DB 컬럼이 아니라 응답 DTO에서 계산**합니다 (`currentPage / pageCount * 100`). 파생 데이터를 저장하지 않는 것도 설계 학습 포인트입니다.
> ⚠️ 이 API가 **N+1 문제의 주 무대**입니다. `record`를 20건 조회하면 `book`을 20번 더 조회합니다. 18주차에 `fetch join`으로 잡고 **쿼리 수를 21 → 1로 줄인 기록을 반드시 남기세요.**

---

### 📚 POST `/api/v1/records` — 서재에 추가

```json
// Request
{ "bookId": 7, "status": "WANT_TO_READ" }

// 201 Created
{ "id": 12, "status": "WANT_TO_READ", "currentPage": 0, "book": { "id": 7, "title": "클린 코드" } }
```
- 이미 등록한 책이면 **409 `DUPLICATE_RECORD`** (UNIQUE 제약을 잡아서 변환)

---

### 📚 PATCH `/api/v1/records/{id}/status`

```json
// Request
{ "status": "READING" }

// 200 OK
{ "id": 12, "status": "READING", "startDate": "2026-07-24", "endDate": null }
```
- 잘못된 전이 → **400 `INVALID_STATUS_TRANSITION`** ("완독한 책은 다시 읽는 중으로 바꿀 수 없습니다.")
- 부수 효과는 `07_ERD_독서기록.md` 6장 표 참조

---

### ⏱ POST `/api/v1/records/{id}/sessions` — 오늘 읽은 기록

```json
// Request
{ "readDate": "2026-07-24", "startPage": 120, "endPage": 137, "minutes": 35 }

// 201 Created
{
  "id": 88, "readDate": "2026-07-24",
  "startPage": 120, "endPage": 137, "pagesRead": 17, "minutes": 35,
  "recordCurrentPage": 137
}
```
| 검증 | 규칙 |
|------|------|
| endPage ≥ startPage | 아니면 400 |
| endPage ≤ book.pageCount | 초과 시 400 |
| readDate | 미래 날짜 금지 |

---

### 📝 POST `/api/v1/records/{id}/notes`

```json
// Request
{ "page": 55, "noteType": "QUOTE", "content": "깨끗한 코드는 한 가지에 집중한다." }

// 201 Created
{ "id": 41, "page": 55, "noteType": "QUOTE", "content": "...", "createdAt": "2026-07-24T15:30:00" }
```

---

### 📊 GET `/api/v1/stats/summary` ⭐

```json
// 200 OK
{
  "totalBooks": 37,
  "doneBooks": 21,
  "readingBooks": 3,
  "totalPagesRead": 6842,
  "totalMinutes": 9130,
  "currentStreakDays": 12,
  "thisYear": { "year": 2026, "doneCount": 14, "targetCount": 24, "achievementRate": 58.3 }
}
```

### 📊 GET `/api/v1/stats/monthly?year=2026`

```json
{
  "year": 2026,
  "months": [
    { "month": 1, "doneCount": 2, "pagesRead": 610 },
    { "month": 2, "doneCount": 1, "pagesRead": 320 },
    { "month": 7, "doneCount": 3, "pagesRead": 894 }
  ]
}
```

> 🔥 **이 API가 포트폴리오의 하이라이트입니다.**
> ① 먼저 순진하게 구현 → ② `EXPLAIN`으로 풀스캔 확인 → ③ `INDEX(member_id, end_date)` 추가 → ④ 전/후 실행 시간 기록.
> 이 4단계를 블로그 글 한 편으로 쓰면 면접 질문의 절반이 여기로 들어옵니다.

---

## 4. 클라이언트별 소비 계획

| 화면 | 사용 API | 웹 | 앱 |
|------|----------|-----|-----|
| 로그인 | 2 | ⭕ | ⭕ |
| 내 서재 목록 | 9 | ⭕ | ⭕ |
| 책 검색·추가 | 6, 11 | ⭕ | ⭕ |
| 기록 상세 | 10, 15, 18 | ⭕ | ⭕ |
| 오늘 읽기 기록 | 16 | ⭕ | ⭕ **(앱이 더 자연스러운 기능)** |
| 메모 작성 | 19 | ⭕ | ⭕ |
| 통계 대시보드 | 22, 23 | ⭕ **(웹이 더 잘 어울림)** | 요약만 |
| 완독 처리·별점 | 12, 13 | ⭕ | ⭕ |

> 💡 **웹과 앱의 역할을 살짝 다르게** 가져가세요. 웹은 "통계·리뷰를 넓게 보는 곳", 앱은 "읽으면서 바로 기록하는 곳".
> 면접에서 "왜 둘 다 만들었나요?"에 대한 답이 여기서 나옵니다.

---

## 5. 3트랙 타입 동기화 (같은 모델, 3개 언어)

```java
// 🟦 Java — Spring Boot DTO
public record BookSummary(Long id, String title, String author,
                          String coverUrl, Integer pageCount) {}
```
```typescript
// 🟨 TypeScript — Next.js
export type BookSummary = {
  id: number; title: string; author: string;
  coverUrl: string | null; pageCount: number | null;
};
```
```kotlin
// 🟩 Kotlin — Android
@Serializable
data class BookSummaryDto(
    val id: Long, val title: String, val author: String,
    val coverUrl: String? = null, val pageCount: Int? = null,
)
```

> ⭐ **이 3개를 나란히 놓고 보는 것이 이 플랜의 핵심 학습 장치입니다.**
> `record` / `type` / `data class`, `Integer` / `number | null` / `Int?` — 세 언어의 null 표현을 동시에 익히게 됩니다.
> **백엔드에서 필드명을 바꾸면 3곳을 다 고쳐야 한다**는 감각이 곧 API 설계 감각입니다.

---

## 6. Mock 데이터로 병렬 개발하기 (백엔드를 기다리지 않는 법)

백엔드가 16주차에 완성되기 전에도 프론트·앱은 개발할 수 있습니다.

| 트랙 | 방법 |
|------|------|
| 🟨 프론트 | **MSW(Mock Service Worker)** 또는 `json-server`로 위 응답 JSON을 그대로 서빙 |
| 🟩 안드로이드 | Repository 인터페이스의 **Fake 구현체**를 만들어 더미 데이터 반환 |

```kotlin
// 🟩 14주차 이전에는 이걸로 화면을 완성해두세요
class FakeRecordRepository : RecordRepository {
    override suspend fun getRecords() = listOf(
        ReadingRecord(1, "클린 코드", "로버트 C. 마틴", ReadingStatus.READING, 137, 324)
    )
}
```

> 💡 Fake 구현체를 만들어두면 **23주차 테스트 작성 때 그대로 재사용**됩니다. 두 번 이득입니다.
