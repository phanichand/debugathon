CREATE SEQUENCE operator_booking_reference_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE operator_bookings (
    id BIGSERIAL PRIMARY KEY,
    operator_booking_reference VARCHAR(64) NOT NULL UNIQUE,
    source_booking_id VARCHAR(64) NOT NULL,
    trip_id VARCHAR(64) NOT NULL,
    passenger_data TEXT NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_operator_bookings_idempotency_key UNIQUE (idempotency_key)
);
