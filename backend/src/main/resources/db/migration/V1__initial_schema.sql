-- =====================================================================
-- StockManager Cloud - schéma initial
-- PostgreSQL 16
-- =====================================================================

CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    first_name    VARCHAR(80)  NOT NULL,
    last_name     VARCHAR(80)  NOT NULL,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE suppliers (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    company    VARCHAR(150),
    email      VARCHAR(160),
    phone      VARCHAR(40),
    address    TEXT,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE customers (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    company    VARCHAR(150),
    email      VARCHAR(160),
    phone      VARCHAR(40),
    address    TEXT,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE warehouses (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    address    TEXT,
    manager    VARCHAR(150),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    reference       VARCHAR(60)  NOT NULL UNIQUE,
    name            VARCHAR(180) NOT NULL,
    description     TEXT,
    purchase_price  NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (purchase_price >= 0),
    selling_price   NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (selling_price >= 0),
    alert_threshold INTEGER        NOT NULL DEFAULT 0 CHECK (alert_threshold >= 0),
    image_url       VARCHAR(500),
    category_id     BIGINT REFERENCES categories (id),
    supplier_id     BIGINT REFERENCES suppliers (id),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_name ON products (LOWER(name));
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_supplier ON products (supplier_id);

CREATE TABLE stocks (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT      NOT NULL REFERENCES products (id),
    warehouse_id      BIGINT      NOT NULL REFERENCES warehouses (id),
    quantity          INTEGER     NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INTEGER     NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_stock_product_warehouse UNIQUE (product_id, warehouse_id),
    CONSTRAINT chk_stock_reserved_lte_quantity CHECK (reserved_quantity <= quantity)
);

CREATE INDEX idx_stocks_warehouse ON stocks (warehouse_id);
CREATE INDEX idx_stocks_product ON stocks (product_id);

CREATE TABLE stock_movements (
    id                     BIGSERIAL PRIMARY KEY,
    type                   VARCHAR(20) NOT NULL CHECK (type IN ('IN', 'OUT', 'TRANSFER', 'ADJUSTMENT')),
    product_id             BIGINT      NOT NULL REFERENCES products (id),
    source_warehouse_id    BIGINT REFERENCES warehouses (id),
    destination_warehouse_id BIGINT REFERENCES warehouses (id),
    quantity               INTEGER     NOT NULL CHECK (quantity > 0),
    reason                 VARCHAR(255),
    created_by             VARCHAR(160),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_movements_product ON stock_movements (product_id);
CREATE INDEX idx_movements_created_at ON stock_movements (created_at DESC);

CREATE SEQUENCE order_reference_seq START 1;
CREATE SEQUENCE invoice_number_seq START 1;

CREATE TABLE customer_orders (
    id           BIGSERIAL PRIMARY KEY,
    reference    VARCHAR(30)  NOT NULL UNIQUE,
    customer_id  BIGINT       NOT NULL REFERENCES customers (id),
    status       VARCHAR(20)  NOT NULL CHECK (status IN ('DRAFT', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    total_ht     NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (total_ht >= 0),
    vat_rate     NUMERIC(5, 2)  NOT NULL DEFAULT 20 CHECK (vat_rate >= 0),
    total_vat    NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (total_vat >= 0),
    total_ttc    NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (total_ttc >= 0),
    notes        TEXT,
    created_by   VARCHAR(160),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMPTZ,
    processing_at TIMESTAMPTZ,
    shipped_at   TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ
);

CREATE INDEX idx_orders_status ON customer_orders (status);
CREATE INDEX idx_orders_customer ON customer_orders (customer_id);
CREATE INDEX idx_orders_created_at ON customer_orders (created_at DESC);

CREATE TABLE order_items (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT       NOT NULL REFERENCES customer_orders (id) ON DELETE CASCADE,
    product_id         BIGINT       NOT NULL REFERENCES products (id),
    warehouse_id       BIGINT       NOT NULL REFERENCES warehouses (id),
    product_reference  VARCHAR(60)  NOT NULL,
    product_name       VARCHAR(180) NOT NULL,
    unit_price         NUMERIC(14, 2) NOT NULL CHECK (unit_price >= 0),
    quantity           INTEGER      NOT NULL CHECK (quantity > 0),
    line_total         NUMERIC(14, 2) NOT NULL CHECK (line_total >= 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

CREATE TABLE invoices (
    id           BIGSERIAL PRIMARY KEY,
    number       VARCHAR(30)  NOT NULL UNIQUE,
    order_id     BIGINT       NOT NULL UNIQUE REFERENCES customer_orders (id),
    customer_id  BIGINT       NOT NULL REFERENCES customers (id),
    total_ht     NUMERIC(14, 2) NOT NULL CHECK (total_ht >= 0),
    vat_rate     NUMERIC(5, 2)  NOT NULL CHECK (vat_rate >= 0),
    total_vat    NUMERIC(14, 2) NOT NULL CHECK (total_vat >= 0),
    total_ttc    NUMERIC(14, 2) NOT NULL CHECK (total_ttc >= 0),
    document_key VARCHAR(300) NOT NULL,
    issued_at    TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoices_customer ON invoices (customer_id);
CREATE INDEX idx_invoices_issued_at ON invoices (issued_at DESC);
