-- Keep existing department rows valid before enforcing a parent relationship.
INSERT INTO departments (dept_code, dept_name, parent_dept_id, is_active, created_at)
SELECT 'ROOT', 'Root Department', NULL, TRUE, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM departments);

UPDATE departments
SET parent_dept_id = department_id
WHERE parent_dept_id IS NULL;

ALTER TABLE departments
    MODIFY parent_dept_id int NOT NULL;

-- Preserve existing party records while making the contact fields required.
UPDATE customers_suppliers
SET tax_id = COALESCE(tax_id, ''),
    address = COALESCE(address, ''),
    contact_person = COALESCE(contact_person, ''),
    contact_email = COALESCE(contact_email, ''),
    contact_phone = COALESCE(contact_phone, ''),
    is_active = COALESCE(is_active, TRUE);

ALTER TABLE customers_suppliers
    MODIFY tax_id varchar(40) NOT NULL,
    MODIFY address text NOT NULL,
    MODIFY contact_person varchar(120) NOT NULL,
    MODIFY contact_email varchar(120) NOT NULL,
    MODIFY contact_phone varchar(30) NOT NULL,
    MODIFY is_active boolean NOT NULL DEFAULT TRUE;

-- Existing nullable sort values should be first in the established order.
UPDATE master_agreement_for SET sort_order = 0 WHERE sort_order IS NULL;
UPDATE master_contract_category SET sort_order = 0 WHERE sort_order IS NULL;
UPDATE master_contract_type SET sort_order = 0 WHERE sort_order IS NULL;
UPDATE master_contract_status SET sort_order = 0 WHERE sort_order IS NULL;
UPDATE master_payment_type SET sort_order = 0 WHERE sort_order IS NULL;
UPDATE master_payment_status SET sort_order = 0 WHERE sort_order IS NULL;
UPDATE master_currency SET sort_order = 0 WHERE sort_order IS NULL;
