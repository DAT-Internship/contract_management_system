-- Keep one hidden top-level parent when the department table is empty.
-- This is needed because parent_dept_id is mandatory for every department.
INSERT INTO departments (department_id, dept_code, dept_name, parent_dept_id, is_active, created_at)
SELECT 1, 'ROOT', 'Root Department', 1, FALSE, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM departments);

-- MySQL keeps the next value above existing IDs, and resets empty tables to 1.
ALTER TABLE departments AUTO_INCREMENT = 1;
ALTER TABLE customers_suppliers AUTO_INCREMENT = 1;
ALTER TABLE master_agreement_for AUTO_INCREMENT = 1;
ALTER TABLE master_contract_category AUTO_INCREMENT = 1;
ALTER TABLE master_contract_type AUTO_INCREMENT = 1;
ALTER TABLE master_contract_status AUTO_INCREMENT = 1;
ALTER TABLE master_payment_type AUTO_INCREMENT = 1;
ALTER TABLE master_payment_status AUTO_INCREMENT = 1;
ALTER TABLE master_currency AUTO_INCREMENT = 1;
