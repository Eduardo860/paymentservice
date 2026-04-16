-- =====================================================
-- PostgreSQL Init Script - Retry Jobs Database
-- =====================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- PRODUCTS RETRY JOBS
-- =====================================================
CREATE TABLE IF NOT EXISTS public.products_retry_jobs (
    id          uuid DEFAULT uuid_generate_v4() NOT NULL,
    product_id  varchar                          NOT NULL,
    request_data  text                           NULL,
    response_data text                           NULL,
    action      varchar                          NOT NULL,
    attempt     int4    NOT NULL DEFAULT 0,
    status      varchar NOT NULL DEFAULT 'SCHEDULED',
    next_run_at timestamptz DEFAULT now()        NOT NULL,
    created_at  timestamptz DEFAULT now()        NOT NULL,
    updated_at  timestamptz DEFAULT now()        NOT NULL,
    CONSTRAINT pk_products_retry_jobs PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_products_retry_product_id
    ON public.products_retry_jobs (product_id);

CREATE INDEX IF NOT EXISTS idx_products_retry_status
    ON public.products_retry_jobs (status);

CREATE INDEX IF NOT EXISTS idx_products_retry_next_run_status
    ON public.products_retry_jobs (next_run_at, status)
    WHERE status = 'SCHEDULED';

CREATE INDEX IF NOT EXISTS idx_products_retry_unique_action
    ON public.products_retry_jobs (product_id, action);

-- =====================================================
-- ORDER RETRY JOBS
-- =====================================================
CREATE TABLE IF NOT EXISTS public.order_retry_jobs (
    id         uuid DEFAULT uuid_generate_v4() NOT NULL,
    order_id   varchar                         NOT NULL,
    request_data  text                         NULL,
    response_data text                         NULL,
    action     varchar                         NOT NULL,
    attempt    int4    NOT NULL DEFAULT 0,
    status     varchar NOT NULL DEFAULT 'SCHEDULED',
    next_run_at timestamptz DEFAULT now()      NOT NULL,
    created_at  timestamptz DEFAULT now()      NOT NULL,
    updated_at  timestamptz DEFAULT now()      NOT NULL,
    CONSTRAINT pk_order_retry_jobs PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_order_retry_order_id
    ON public.order_retry_jobs (order_id);

CREATE INDEX IF NOT EXISTS idx_order_retry_status
    ON public.order_retry_jobs (status);

CREATE INDEX IF NOT EXISTS idx_order_retry_next_run_status
    ON public.order_retry_jobs (next_run_at, status)
    WHERE status = 'SCHEDULED';

CREATE INDEX IF NOT EXISTS idx_order_retry_unique_action
    ON public.order_retry_jobs (order_id, action);

-- =====================================================
-- PAYMENTS RETRY JOBS
-- =====================================================
CREATE TABLE IF NOT EXISTS public.payments_retry_jobs (
    id         uuid DEFAULT uuid_generate_v4() NOT NULL,
    payment_id varchar                         NOT NULL,
    request_data  text                         NULL,
    response_data text                         NULL,
    action     varchar                         NOT NULL,
    attempt    int4    NOT NULL DEFAULT 0,
    status     varchar NOT NULL DEFAULT 'SCHEDULED',
    next_run_at timestamptz DEFAULT now()      NOT NULL,
    created_at  timestamptz DEFAULT now()      NOT NULL,
    updated_at  timestamptz DEFAULT now()      NOT NULL,
    CONSTRAINT pk_payments_retry_jobs PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_payments_retry_payment_id
    ON public.payments_retry_jobs (payment_id);

CREATE INDEX IF NOT EXISTS idx_payments_retry_status
    ON public.payments_retry_jobs (status);

CREATE INDEX IF NOT EXISTS idx_payments_retry_next_run_status
    ON public.payments_retry_jobs (next_run_at, status)
    WHERE status = 'SCHEDULED';

CREATE INDEX IF NOT EXISTS idx_payments_retry_unique_action
    ON public.payments_retry_jobs (payment_id, action);
