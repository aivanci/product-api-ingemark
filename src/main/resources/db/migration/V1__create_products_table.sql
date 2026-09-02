CREATE TABLE products
(
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(10)    NOT NULL,
    name         VARCHAR(255)   NOT NULL,
    price_eur    NUMERIC(12, 2) NOT NULL,
    price_usd    NUMERIC(12, 2) NOT NULL,
    is_available BOOLEAN        NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_products_code UNIQUE (code),
    CONSTRAINT chk_products_code_length CHECK (char_length(code) = 10),
    CONSTRAINT chk_products_price_eur_non_negative CHECK (price_eur >= 0),
    CONSTRAINT chk_products_price_usd_non_negative CHECK (price_usd >= 0)
);
