# 무료 포인트 시스템 API

무료 포인트의 적립, 적립취소, 사용, 사용취소 기능을 제공하는 REST API 서버입니다.

## 기술 스택

- Java 21
- Spring Boot 3.4.4
- Spring Data JPA
- H2 Database (In-Memory)
- Gradle

## 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

실행 후 H2 콘솔: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:payment`
- Username: `sa`
- Password: (비워두기)

## API 명세

### 1. 포인트 적립

`POST /api/points/earn`

```json
{
  "memberId": 1,
  "amount": 1000,
  "earnType": "NORMAL",
  "expireDays": 365,
  "idempotencyKey": "uuid"
}
```

| 필드 | 필수 | 기본값 | 설명 |
|------|------|--------|------|
| memberId | O | - | 회원 ID |
| amount | O | - | 적립 금액 (1 이상, 정책 최대값 이하) |
| earnType | X | NORMAL | 적립 유형 (NORMAL: 일반, MANUAL: 수기지급) |
| expireDays | X | 365 | 만료일 (1일 이상 5년 미만) |
| idempotencyKey | O | - | 멱등성 키 |

### 2. 포인트 적립취소

`POST /api/points/earn/cancel`

```json
{
  "memberId": 1,
  "pointKey": "PT-a3f8b2c1"
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| memberId | O | 회원 ID |
| pointKey | O | 취소할 적립건의 고유 식별자 (멱등성 보장) |

- 적립한 금액 중 일부라도 사용된 경우 취소 불가
- 동일 pointKey로 재요청 시 기존 결과 반환 (멱등성)

### 3. 포인트 사용

`POST /api/points/use`

```json
{
  "memberId": 1,
  "amount": 1200,
  "orderId": "A1234",
  "idempotencyKey": "uuid"
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| memberId | O | 회원 ID |
| amount | O | 사용 금액 (1 이상) |
| orderId | O | 주문번호 |
| idempotencyKey | O | 멱등성 키 |

- 수기지급(MANUAL) 포인트 우선 사용
- 만료일이 짧게 남은 순서로 사용
- 적립건별 1원 단위 사용 추적

### 4. 포인트 사용취소

`POST /api/points/use/cancel`

```json
{
  "memberId": 1,
  "pointKey": "PT-cccc1234",
  "cancelAmount": 1100,
  "idempotencyKey": "uuid"
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| memberId | O | 회원 ID |
| pointKey | O | 취소할 사용 거래의 고유 식별자 |
| cancelAmount | O | 취소 금액 (전체 또는 부분) |
| idempotencyKey | O | 멱등성 키 |

- 전체 또는 부분 사용취소 가능
- 이미 만료된 적립건의 사용취소 시 신규적립 처리

## 응답 형식

모든 API는 `CommonResponse`로 래핑됩니다.

```json
{
  "meta": {
    "code": "SUCCESS",
    "message": "성공"
  },
  "data": { ... }
}
```

## DB 설계

### 테이블 구조

| 테이블 | 설명 |
|--------|------|
| point_policy | 포인트 정책 (1회 최대 적립금, 최대 보유금 등 런타임 변경 가능) |
| point_wallet | 회원별 포인트 지갑 (총 잔액 캐시, 동시성 제어 락 대상) |
| point_earn | 포인트 적립건 (개별 잔액 관리, 만료일, 수기지급 구분) |
| point_transaction | 거래 이력 (적립/적립취소/사용/사용취소 기록, 멱등성 키) |
| point_usage | 사용-적립 매핑 (어떤 적립건에서 얼마를 차감했는지 1원 단위 추적) |

ERD는 `src/main/resources/erd.png`에서 확인할 수 있습니다.

### 정책 관리

`point_policy` 테이블로 하드코딩 없이 정책값을 관리합니다.

| 정책 키 | 기본값 | 설명 |
|---------|--------|------|
| MAX_EARN_AMOUNT | 100,000 | 1회 최대 적립 가능 금액 |
| MAX_HOLD_AMOUNT | 5,000,000 | 개인별 최대 보유 가능 금액 |

## 동시성 제어

- **비관적 락**: `point_wallet`에 `SELECT FOR UPDATE`를 적용하여 같은 회원의 포인트 변경을 순차 처리
- **멱등성 이중 체크**: AOP 기반 1차 체크(트랜잭션 전 빠른 차단) + 락 획득 후 2차 재체크(동시 요청 방어)
- **지갑 동시 생성 방지**: `member_id` UNIQUE 제약으로 DB 레벨 방어

## 프로젝트 구조

```
src/main/java/com/musinsa/payment/
├── common/
│   ├── config/          JPA 설정
│   ├── dto/             공통 응답 (CommonResponse, SuccessResponseAdvice)
│   ├── entity/          BaseEntity (createdAt, updatedAt)
│   ├── exception/       예외 처리 (Result, BusinessException, GlobalExceptionHandler)
│   └── idempotency/     멱등성 AOP (@Idempotent, IdempotentAspect)
├── point/
│   ├── common/          포인트 상수 (PointConstants)
│   ├── controller/      API 엔드포인트
│   ├── dto/             요청/응답 DTO
│   ├── entity/          도메인 엔티티
│   ├── repository/      데이터 접근
│   └── service/         비즈니스 로직 (PointService, PointValidator)
└── PaymentApplication.java

src/main/resources/
├── db/
│   ├── ddl.sql          테이블 생성
│   └── dml.sql          초기 정책 데이터
├── application.yml
└── erd.png              ERD 이미지
```

## 테스트

```
src/test/java/com/musinsa/payment/point/
├── entity/
│   └── PointEarnTest.java              엔티티 단위 테스트
├── service/
│   ├── PointServiceIntegrationTest.java 통합 테스트 (과제 시나리오, 경계값, 엣지 케이스)
│   └── PointConcurrencyTest.java        동시성 테스트
└── controller/
    └── PointControllerTest.java         API 엔드포인트 테스트
```
