CREATE TABLE product_prices (
    product_id varchar(80) PRIMARY KEY,
    amount numeric(10,2) NOT NULL CHECK (amount > 0),
    currency varchar(3) NOT NULL DEFAULT 'INR',
    version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
INSERT INTO product_prices(product_id, amount)
SELECT 'SKU-' || lpad(i::text, 5, '0'), 820.00 FROM generate_series(1, 20000) i;
INSERT INTO product_prices(product_id, amount) VALUES ('DEMO-001',820.00), ('SMOKE-001',820.00);

CREATE TABLE tax_rules (
    rule_id varchar(40) PRIMARY KEY,
    market varchar(4) NOT NULL,
    lower_amount numeric(10,2) NOT NULL,
    upper_amount numeric(10,2) NOT NULL,
    rate numeric(5,4) NOT NULL,
    priority integer NOT NULL
);
INSERT INTO tax_rules VALUES ('IN-BASE', 'IN', 0, 1000001, 0.05, 0);
INSERT INTO tax_rules
SELECT 'IN-BAND-' || i, 'IN', i * 100, (i + 1) * 100, 0.05, i
FROM generate_series(1, 12000) i;
