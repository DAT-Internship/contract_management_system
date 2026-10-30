INSERT INTO master_contract_status (code, name, is_active, sort_order)
SELECT 'DRAFT', 'Draft', TRUE, 0
WHERE NOT EXISTS (
    SELECT 1 FROM master_contract_status WHERE UPPER(code) = 'DRAFT'
);

ALTER TABLE contracts
    MODIFY fiscal_year VARCHAR(10) NULL,
    MODIFY contract_po_no VARCHAR(40) NULL,
    MODIFY agreement_for_id INT NULL,
    MODIFY category_id INT NULL,
    MODIFY contract_type_id INT NULL,
    MODIFY payment_type_id INT NULL,
    MODIFY department_id INT NULL,
    MODIFY party_id BIGINT NULL,
    MODIFY description TEXT NULL,
    MODIFY start_date DATE NULL,
    MODIFY end_date DATE NULL,
    MODIFY currency_id INT NULL;
