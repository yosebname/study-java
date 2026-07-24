# 📖 북로그(BookLog) — 도메인 설계 & ERD

> **1 서버 3 클라이언트** 프로젝트의 데이터 설계.
> `booklog-api` (Spring Boot) · `booklog-web` (Next.js) · `booklog-android` (Compose) 가 이 스키마 하나를 공유합니다.

---

## 1. 서비스 한 줄 정의

> **"읽은 책과 읽는 과정을 기록하고, 내 독서 습관을 숫자로 보여주는 앱"**

### MVP 핵심 기능 3개 (이것만 완성하면 성공)

| # | 기능 | 왜 이 기능인가 (학습 관점) |
|---|------|---------------------------|
| ① | **책 등록 + 독서 상태 관리** (읽고싶은 / 읽는 중 / 완독 / 중단) | 기본 CRUD + enum + 상태 전이 로직 |
| ② | **독서 세션 기록 + 메모/인용** (오늘 몇 페이지, 몇 분, 어떤 문장) | **1:N 연관관계** + 날짜 처리 + 페이징 |
| ③ | **통계 & 연간 목표** (월별 완독 수, 총 페이지, 목표 달성률) | **집계 쿼리(GROUP BY)** + 인덱스 + 성능 튜닝 재료 ⭐ |

### 확장 기능 (25주차 이후 여유 있으면)
- 태그 / 검색 / 정렬 · 책 표지 이미지 업로드(S3) · 공개 리뷰 피드 · 독서 캘린더 히트맵

> 💡 **③ 통계가 이 프로젝트의 진짜 무기입니다.** 게시판 CRUD는 누구나 냅니다. "월별 완독 수 집계 쿼리가 느려서 인덱스를 걸고 320ms → 12ms로 줄였다"는 면접에서 5분을 벌어줍니다.

---

## 2. ERD (텍스트 다이어그램)

```
┌────────────────┐
│    member      │
│────────────────│
│ id (PK)        │◄──────┐
│ email (UQ)     │       │
│ password       │       │
│ nickname       │       │
│ profile_img_url│       │
│ created_at     │       │
│ updated_at     │       │
└────────────────┘       │
        ▲                │
        │ 1              │ 1
        │                │
        │ N              │ N
┌────────────────┐   ┌──────────────────┐
│ refresh_token  │   │  reading_goal    │
│────────────────│   │──────────────────│
│ id (PK)        │   │ id (PK)          │
│ member_id (FK) │   │ member_id (FK)   │
│ token (UQ)     │   │ year             │
│ expires_at     │   │ target_count     │
└────────────────┘   │ (member,year) UQ │
                     └──────────────────┘

┌────────────────┐              ┌──────────────────────┐
│     book       │  1        N  │   reading_record     │  ← 핵심 테이블
│────────────────│◄─────────────│──────────────────────│
│ id (PK)        │              │ id (PK)              │
│ isbn13 (UQ)    │              │ member_id (FK) ──────┼──► member
│ title          │              │ book_id (FK)         │
│ author         │              │ status (enum)        │
│ publisher      │              │ start_date           │
│ published_date │              │ end_date             │
│ cover_url      │              │ current_page         │
│ page_count     │              │ rating (1~5, null)   │
│ created_at     │              │ review_text          │
└────────────────┘              │ is_public            │
                                │ created_at           │
                                │ updated_at           │
                                │ UQ(member_id,book_id)│
                                └──────────────────────┘
                                    ▲   ▲            ▲
                        ┌───────────┘   │            └────────────┐
                        │ N             │ N                       │ N
             ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
             │ reading_session  │  │  reading_note    │  │ reading_record_  │
             │──────────────────│  │──────────────────│  │      tag         │
             │ id (PK)          │  │ id (PK)          │  │──────────────────│
             │ record_id (FK)   │  │ record_id (FK)   │  │ record_id (FK)   │
             │ read_date        │  │ page             │  │ tag_id (FK) ─────┼──┐
             │ start_page       │  │ note_type (enum) │  │ PK(record,tag)   │  │
             │ end_page         │  │ content          │  └──────────────────┘  │
             │ minutes          │  │ created_at       │                        │
             │ created_at       │  │ updated_at       │              ┌─────────▼──────┐
             └──────────────────┘  └──────────────────┘              │      tag       │
                                                                     │────────────────│
                                                                     │ id (PK)        │
                                                                     │ name (UQ)      │
                                                                     └────────────────┘
```

### 관계 요약

| 관계 | 카디널리티 | JPA 매핑 |
|------|-----------|----------|
| member → reading_record | 1 : N | `@OneToMany(mappedBy="member")` / `@ManyToOne` |
| book → reading_record | 1 : N | `@ManyToOne(fetch = LAZY)` |
| reading_record → reading_session | 1 : N | `@OneToMany(cascade = ALL, orphanRemoval = true)` |
| reading_record → reading_note | 1 : N | 동일 |
| reading_record ↔ tag | N : M | **중간 엔티티 `reading_record_tag`로 풀어서** ⭐ |
| member → reading_goal | 1 : N (연도별) | `@ManyToOne` + UQ(member, year) |

> ⚠️ **N:M을 `@ManyToMany`로 매핑하지 마세요.** 실무에서 쓰지 않습니다. 중간 엔티티를 직접 만들어 `@ManyToOne` 두 개로 푸는 게 정답이고, 면접에서도 이걸 묻습니다.

---

## 3. 테이블 상세 명세

### `member` — 회원

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| email | VARCHAR(255) | **UNIQUE**, NOT NULL | 로그인 ID |
| password | VARCHAR(255) | NOT NULL | **BCrypt 해시** (평문 저장 절대 금지) |
| nickname | VARCHAR(30) | NOT NULL | |
| profile_image_url | VARCHAR(500) | NULL | |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | USER / ADMIN |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

### `book` — 책 마스터 (전 회원 공유)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK | |
| isbn13 | VARCHAR(13) | **UNIQUE**, NULL | 수동 등록 시 NULL 허용 |
| title | VARCHAR(255) | NOT NULL | |
| author | VARCHAR(255) | NOT NULL | |
| publisher | VARCHAR(255) | NULL | |
| published_date | DATE | NULL | |
| cover_url | VARCHAR(500) | NULL | |
| page_count | INT | NULL | 진행률 계산용 |
| created_at | DATETIME | NOT NULL | |

> 💡 **왜 book을 회원별로 안 두고 마스터로 분리했나** — 같은 책을 10명이 읽으면 행이 10개 생기는 낭비를 막습니다. 정규화 학습의 산 교재이자 면접 답변거리입니다.

### `reading_record` — 독서 기록 (핵심) ⭐

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK | |
| member_id | BIGINT | FK → member, NOT NULL | |
| book_id | BIGINT | FK → book, NOT NULL | |
| status | VARCHAR(20) | NOT NULL | `WANT_TO_READ` / `READING` / `DONE` / `PAUSED` |
| start_date | DATE | NULL | READING 진입 시 세팅 |
| end_date | DATE | NULL | DONE 진입 시 세팅 |
| current_page | INT | NOT NULL, DEFAULT 0 | |
| rating | TINYINT | NULL | 1~5, DONE일 때만 |
| review_text | TEXT | NULL | |
| is_public | BOOLEAN | NOT NULL, DEFAULT false | |
| created_at / updated_at | DATETIME | NOT NULL | |

- **UNIQUE (member_id, book_id)** — 같은 회원이 같은 책을 두 번 등록 못 함 (409 Conflict 학습 재료)
- **INDEX (member_id, status)** — 목록 조회용
- **INDEX (member_id, end_date)** — 월별 통계용 ⭐

### `reading_session` — 읽은 기록 (통계의 원천)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| record_id | BIGINT | FK → reading_record, NOT NULL |
| read_date | DATE | NOT NULL |
| start_page / end_page | INT | NOT NULL |
| minutes | INT | NULL |
| created_at | DATETIME | NOT NULL |

- **INDEX (record_id, read_date)**

### `reading_note` — 메모 / 인용

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| record_id | BIGINT | FK → reading_record, NOT NULL |
| page | INT | NULL |
| note_type | VARCHAR(20) | NOT NULL — `QUOTE`(인용) / `THOUGHT`(생각) |
| content | TEXT | NOT NULL |
| created_at / updated_at | DATETIME | NOT NULL |

### `reading_goal` — 연간 목표

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| member_id | BIGINT | FK, NOT NULL |
| year | INT | NOT NULL |
| target_count | INT | NOT NULL |

- **UNIQUE (member_id, year)**

### `tag` / `reading_record_tag`

| tag | | | `reading_record_tag` | |
|---|---|---|---|---|
| id BIGINT PK | | | record_id BIGINT FK | |
| name VARCHAR(30) UNIQUE | | | tag_id BIGINT FK | **PK(record_id, tag_id)** |

### `refresh_token`

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| member_id | BIGINT | FK, NOT NULL |
| token | VARCHAR(500) | UNIQUE, NOT NULL |
| expires_at | DATETIME | NOT NULL |

---

## 4. dbdiagram.io DSL (복붙해서 바로 그림 뽑기)

> https://dbdiagram.io 에 붙여넣으면 ERD 이미지가 나옵니다. **이 이미지를 README에 넣으세요.**

```
Table member {
  id bigint [pk, increment]
  email varchar(255) [unique, not null]
  password varchar(255) [not null]
  nickname varchar(30) [not null]
  profile_image_url varchar(500)
  role varchar(20) [not null, default: 'USER']
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table book {
  id bigint [pk, increment]
  isbn13 varchar(13) [unique]
  title varchar(255) [not null]
  author varchar(255) [not null]
  publisher varchar(255)
  published_date date
  cover_url varchar(500)
  page_count int
  created_at datetime [not null]
}

Table reading_record {
  id bigint [pk, increment]
  member_id bigint [not null]
  book_id bigint [not null]
  status varchar(20) [not null, note: 'WANT_TO_READ/READING/DONE/PAUSED']
  start_date date
  end_date date
  current_page int [not null, default: 0]
  rating tinyint
  review_text text
  is_public boolean [not null, default: false]
  created_at datetime [not null]
  updated_at datetime [not null]

  indexes {
    (member_id, book_id) [unique]
    (member_id, status)
    (member_id, end_date)
  }
}

Table reading_session {
  id bigint [pk, increment]
  record_id bigint [not null]
  read_date date [not null]
  start_page int [not null]
  end_page int [not null]
  minutes int
  created_at datetime [not null]

  indexes { (record_id, read_date) }
}

Table reading_note {
  id bigint [pk, increment]
  record_id bigint [not null]
  page int
  note_type varchar(20) [not null, note: 'QUOTE/THOUGHT']
  content text [not null]
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table reading_goal {
  id bigint [pk, increment]
  member_id bigint [not null]
  year int [not null]
  target_count int [not null]
  indexes { (member_id, year) [unique] }
}

Table tag {
  id bigint [pk, increment]
  name varchar(30) [unique, not null]
}

Table reading_record_tag {
  record_id bigint
  tag_id bigint
  indexes { (record_id, tag_id) [pk] }
}

Table refresh_token {
  id bigint [pk, increment]
  member_id bigint [not null]
  token varchar(500) [unique, not null]
  expires_at datetime [not null]
}

Ref: reading_record.member_id > member.id
Ref: reading_record.book_id > book.id
Ref: reading_session.record_id > reading_record.id
Ref: reading_note.record_id > reading_record.id
Ref: reading_goal.member_id > member.id
Ref: reading_record_tag.record_id > reading_record.id
Ref: reading_record_tag.tag_id > tag.id
Ref: refresh_token.member_id > member.id
```

---

## 5. 단계별 도입 시점 (한꺼번에 다 만들지 않습니다) ⭐

| 주차 | 도입 테이블 | 이유 |
|------|-------------|------|
| **12주** | `book`, `reading_record` | SQL 실습용. **JOIN 연습이 되는 최소 2개** |
| **17주** | + `reading_session`, `reading_note` | JPA **1:N 연관관계**를 배우는 시점 |
| **18주** | + `reading_goal` | 집계 쿼리·페이징 실습 |
| **21주** | + `member`, `refresh_token` | Security/JWT 시점. **그전엔 memberId를 1로 하드코딩** |
| **28주** | + `tag`, `reading_record_tag` | N:M을 배울 여유가 생겼을 때 |

> ⚠️ **1주차에 테이블 9개를 다 만들면 100% 무너집니다.** 배운 만큼만 추가하세요.
> `member`를 21주차에 넣는 이유: 인증을 모르는 상태에서 회원 개념을 넣으면 모든 쿼리에 `WHERE member_id = ?`를 붙이며 헤매게 됩니다. **`memberId = 1L` 상수로 시작**하고, Security를 배운 뒤 한 번에 치환하세요.

---

## 6. 상태 전이 규칙 (Service 계층 비즈니스 로직)

```
       등록
        ↓
  WANT_TO_READ ──읽기 시작──► READING ──완독──► DONE
        │                        │  ▲
        │                        │  │ 재개
        └────────바로 완독───────┘  ▼
                              PAUSED
```

| 전이 | 부수 효과 | 검증 |
|------|-----------|------|
| → READING | `start_date = today` | 이미 READING이면 400 |
| → DONE | `end_date = today`, `current_page = book.page_count` | rating 없으면 허용(나중에 입력) |
| → PAUSED | 없음 | READING 상태에서만 가능 |
| 세션 추가 | `current_page = max(current_page, end_page)` | `end_page ≥ start_page` 아니면 400 |

> 💡 **이 표가 곧 Service 단위 테스트 케이스 목록입니다.** 23주차에 그대로 옮겨 쓰세요.
