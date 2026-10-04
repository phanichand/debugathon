CREATE SEQUENCE booking_reference_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(64) NOT NULL UNIQUE,
    customer_id VARCHAR(64) NOT NULL,
    trip_id VARCHAR(64) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payment_status VARCHAR(32),
    payment_reference VARCHAR(64),
    operator_booking_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE booking_attempts (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    attempt_number INT NOT NULL,
    attempt_type VARCHAR(16) NOT NULL,
    request_timestamp TIMESTAMPTZ NOT NULL,
    response_timestamp TIMESTAMPTZ,
    idempotency_key VARCHAR(128),
    correlation_id VARCHAR(64),
    http_status INT,
    outcome VARCHAR(32) NOT NULL,
    error_type VARCHAR(128),
    duration_ms BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
