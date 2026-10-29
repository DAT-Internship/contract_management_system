-- Existing master rows may have null flags or sort values because V1 allowed them.
UPDATE master_agreement_for SET is_active = TRUE WHERE is_active IS NULL;
UPDATE master_contract_category SET is_active = TRUE WHERE is_active IS NULL;
UPDATE master_contract_type SET is_active = TRUE WHERE is_active IS NULL;
UPDATE master_contract_status SET is_active = TRUE WHERE is_active IS NULL;
UPDATE master_payment_type SET is_active = TRUE WHERE is_active IS NULL;
UPDATE master_payment_status SET is_active = TRUE WHERE is_active IS NULL;
UPDATE master_currency SET is_active = TRUE WHERE is_active IS NULL;

ALTER TABLE master_agreement_for
    MODIFY is_active boolean NOT NULL DEFAULT TRUE,
    MODIFY sort_order int NOT NULL DEFAULT 0;
ALTER TABLE master_contract_category
    MODIFY is_active boolean NOT NULL DEFAULT TRUE,
    MODIFY sort_order int NOT NULL DEFAULT 0;
ALTER TABLE master_contract_type
    MODIFY is_active boolean NOT NULL DEFAULT TRUE,
    MODIFY sort_order int NOT NULL DEFAULT 0;
ALTER TABLE master_contract_status
    MODIFY is_active boolean NOT NULL DEFAULT TRUE,
    MODIFY sort_order int NOT NULL DEFAULT 0;
ALTER TABLE master_payment_type
    MODIFY is_active boolean NOT NULL DEFAULT TRUE,
    MODIFY sort_order int NOT NULL DEFAULT 0;
ALTER TABLE master_payment_status
    MODIFY is_active boolean NOT NULL DEFAULT TRUE,
    MODIFY sort_order int NOT NULL DEFAULT 0;
ALTER TABLE master_currency
    MODIFY is_active boolean NOT NULL DEFAULT TRUE,
    MODIFY sort_order int NOT NULL DEFAULT 0;
