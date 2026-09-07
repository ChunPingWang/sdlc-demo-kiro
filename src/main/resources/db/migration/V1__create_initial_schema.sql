-- V1: 壽險保費試算系統初始 Schema
-- 對應 SD-LIFE-v1.0 §7.2 資料表設計

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ────────────────────────────────────────────
-- products（保險商品）
-- ────────────────────────────────────────────
CREATE TABLE products (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    product_code VARCHAR(20)  NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_product_code UNIQUE (product_code),
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

-- ────────────────────────────────────────────
-- rate_table_versions（費率表版本）
-- ────────────────────────────────────────────
CREATE TABLE rate_table_versions (
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    product_id     UUID        NOT NULL,
    product_code   VARCHAR(20) NOT NULL,
    version_number INT         NOT NULL,
    effective_date DATE        NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    entry_count    INT         NOT NULL DEFAULT 0,
    file_path      VARCHAR(500) NOT NULL,
    uploaded_by    UUID        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_rate_table_versions PRIMARY KEY (id),
    CONSTRAINT fk_rtv_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uq_rate_versions_product_effective UNIQUE (product_code, effective_date),
    CONSTRAINT chk_rtv_status CHECK (status IN ('PENDING','ACTIVE','SUPERSEDED'))
);

CREATE INDEX idx_rate_versions_product_code ON rate_table_versions (product_code);

-- ────────────────────────────────────────────
-- rate_entries（費率明細）
-- ────────────────────────────────────────────
CREATE TABLE rate_entries (
    id             UUID           NOT NULL DEFAULT gen_random_uuid(),
    version_id     UUID           NOT NULL,
    age            SMALLINT       NOT NULL,
    gender         CHAR(1)        NOT NULL,
    payment_period SMALLINT       NOT NULL,
    rate           NUMERIC(10,4)  NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_rate_entries PRIMARY KEY (id),
    CONSTRAINT fk_re_version FOREIGN KEY (version_id) REFERENCES rate_table_versions(id),
    CONSTRAINT chk_re_age    CHECK (age >= 0 AND age <= 120),
    CONSTRAINT chk_re_gender CHECK (gender IN ('M','F')),
    CONSTRAINT chk_re_period CHECK (payment_period IN (10,20,30,99)),
    CONSTRAINT chk_re_rate   CHECK (rate > 0)
);

CREATE INDEX idx_rate_entries_lookup ON rate_entries (version_id, age, gender, payment_period);

-- ────────────────────────────────────────────
-- calculation_records（試算紀錄）
-- ────────────────────────────────────────────
CREATE TABLE calculation_records (
    id              UUID           NOT NULL DEFAULT gen_random_uuid(),
    agent_id        UUID,
    product_code    VARCHAR(20)    NOT NULL,
    insured_age     SMALLINT       NOT NULL,
    insured_gender  CHAR(1)        NOT NULL,
    insured_amount  INT            NOT NULL,
    payment_period  SMALLINT       NOT NULL,
    rate_used       NUMERIC(10,4),
    annual_premium  INT,
    monthly_premium INT,
    status          VARCHAR(20)    NOT NULL,
    failure_reason  VARCHAR(50),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_calculation_records PRIMARY KEY (id),
    CONSTRAINT chk_cr_status CHECK (status IN ('SUCCESS','FAILED'))
);

CREATE INDEX idx_calc_records_agent_id   ON calculation_records (agent_id, created_at DESC);
CREATE INDEX idx_calc_records_created_at ON calculation_records (created_at DESC);
