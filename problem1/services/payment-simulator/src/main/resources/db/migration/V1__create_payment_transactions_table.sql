CREATE SEQUENCE payment_reference_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(64) NOT NULL,
    payment_reference VARCHAR(64) NOT NULL UNIQUE,
    amount NUMERIC(12,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
