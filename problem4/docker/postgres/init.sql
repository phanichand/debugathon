CREATE TABLE source_events(event_id text PRIMARY KEY,run_id text NOT NULL,payload jsonb NOT NULL,created_at timestamptz NOT NULL DEFAULT clock_timestamp());
CREATE INDEX source_run ON source_events(run_id);
CREATE TABLE delivery_receipts(id bigserial PRIMARY KEY,event_id text NOT NULL REFERENCES source_events,topic text NOT NULL,partition_id int NOT NULL,record_offset bigint NOT NULL,created_at timestamptz NOT NULL DEFAULT clock_timestamp());
CREATE INDEX delivery_event ON delivery_receipts(event_id);
CREATE TABLE settlements(id bigserial PRIMARY KEY,execution_id uuid UNIQUE NOT NULL,event_id text NOT NULL,booking_id text NOT NULL,amount_paise bigint NOT NULL CHECK(amount_paise>0),currency text NOT NULL,partition_id int NOT NULL,record_offset bigint NOT NULL,instance_id text NOT NULL,run_id text NOT NULL,created_at timestamptz NOT NULL DEFAULT clock_timestamp());
CREATE INDEX settlement_event ON settlements(event_id);
CREATE INDEX settlement_run ON settlements(run_id);
