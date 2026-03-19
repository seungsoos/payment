-- 포인트 정책 테이블
CREATE TABLE IF NOT EXISTS point_policy (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_key      VARCHAR(50)     NOT NULL UNIQUE,
    policy_value    BIGINT          NOT NULL,
    description     VARCHAR(200),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 회원 포인트 지갑 테이블
CREATE TABLE IF NOT EXISTS point_wallet (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT          NOT NULL UNIQUE,
    total_balance   BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 포인트 적립건 테이블
CREATE TABLE IF NOT EXISTS point_earn (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id           BIGINT          NOT NULL,
    point_key           VARCHAR(50)     NOT NULL UNIQUE,
    earned_amount       BIGINT          NOT NULL,
    remaining_amount    BIGINT          NOT NULL,
    earn_type           VARCHAR(20)     NOT NULL,
    expires_at          TIMESTAMP       NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_point_earn_wallet FOREIGN KEY (wallet_id) REFERENCES point_wallet(id)
);

-- 포인트 거래 이력 테이블
CREATE TABLE IF NOT EXISTS point_transaction (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id           BIGINT          NOT NULL,
    point_key           VARCHAR(50)     NOT NULL UNIQUE,
    type                VARCHAR(20)     NOT NULL,
    amount              BIGINT          NOT NULL,
    order_id            VARCHAR(100),
    related_point_key   VARCHAR(50),
    idempotency_key     VARCHAR(100)    UNIQUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_wallet FOREIGN KEY (wallet_id) REFERENCES point_wallet(id)
);

-- 포인트 사용-적립 매핑 테이블
CREATE TABLE IF NOT EXISTS point_usage (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id      BIGINT          NOT NULL,
    point_id            BIGINT          NOT NULL,
    amount              BIGINT          NOT NULL,
    restored_amount     BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usage_transaction FOREIGN KEY (transaction_id) REFERENCES point_transaction(id),
    CONSTRAINT fk_usage_point FOREIGN KEY (point_id) REFERENCES point_earn(id)
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_point_earn_wallet_id ON point_earn(wallet_id);
CREATE INDEX IF NOT EXISTS idx_point_earn_status_expires ON point_earn(status, expires_at);
CREATE INDEX IF NOT EXISTS idx_transaction_wallet_id ON point_transaction(wallet_id);
CREATE INDEX IF NOT EXISTS idx_transaction_related_key ON point_transaction(related_point_key);
CREATE INDEX IF NOT EXISTS idx_usage_transaction_id ON point_usage(transaction_id);
CREATE INDEX IF NOT EXISTS idx_usage_point_id ON point_usage(point_id);
