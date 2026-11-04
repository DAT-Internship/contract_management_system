START TRANSACTION;

-- ---------------------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------------------
INSERT INTO roles (role_code, role_name, description)
SELECT 'ADMIN', 'Administrator', 'Full system access' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'ADMIN');

INSERT INTO roles (role_code, role_name, description)
SELECT 'FINANCE', 'Finance', 'View all contracts and manage payment records' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'FINANCE');

INSERT INTO roles (role_code, role_name, description)
SELECT 'APPROVER', 'Approver', 'Review and approve contracts in own department' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'APPROVER');

INSERT INTO roles (role_code, role_name, description)
SELECT 'OPERATOR', 'Operator', 'Create and view own contracts' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'OPERATOR');

-- ---------------------------------------------------------------------------
-- Departments (children of the ROOT department created by V5)
-- ---------------------------------------------------------------------------
SET @root_department_id = (
    SELECT department_id FROM departments WHERE dept_code = 'ROOT' LIMIT 1
);

INSERT INTO departments (dept_code, dept_name, parent_dept_id, is_active, created_at)
SELECT 'D001', 'PMO', @root_department_id, TRUE, NOW() FROM DUAL
WHERE @root_department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM departments WHERE dept_code = 'D001');

INSERT INTO departments (dept_code, dept_name, parent_dept_id, is_active, created_at)
SELECT 'D002', 'Digital Solutions', @root_department_id, TRUE, NOW() FROM DUAL
WHERE @root_department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM departments WHERE dept_code = 'D002');

INSERT INTO departments (dept_code, dept_name, parent_dept_id, is_active, created_at)
SELECT 'D003', 'Application Development', @root_department_id, TRUE, NOW() FROM DUAL
WHERE @root_department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM departments WHERE dept_code = 'D003');

INSERT INTO departments (dept_code, dept_name, parent_dept_id, is_active, created_at)
SELECT 'D004', 'Infrastructure', @root_department_id, TRUE, NOW() FROM DUAL
WHERE @root_department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM departments WHERE dept_code = 'D004');

INSERT INTO departments (dept_code, dept_name, parent_dept_id, is_active, created_at)
SELECT 'D005', 'Human Resources', @root_department_id, TRUE, NOW() FROM DUAL
WHERE @root_department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM departments WHERE dept_code = 'D005');

INSERT INTO departments (dept_code, dept_name, parent_dept_id, is_active, created_at)
SELECT 'D006', 'Finance', @root_department_id, TRUE, NOW() FROM DUAL
WHERE @root_department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM departments WHERE dept_code = 'D006');

-- ---------------------------------------------------------------------------
-- Customers / Suppliers
-- ---------------------------------------------------------------------------
INSERT INTO customers_suppliers
(party_code, party_name, party_type, tax_id, address, contact_person,
 contact_email, contact_phone, is_active, created_at)
SELECT 'CS001', 'ABC Bank', 'SUPPLIER', 'TAX001', 'Yangon', 'Aung Aung',
       'abc.bank@gmail.com', '09111111111', TRUE, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM customers_suppliers WHERE party_code = 'CS001');

INSERT INTO customers_suppliers
(party_code, party_name, party_type, tax_id, address, contact_person,
 contact_email, contact_phone, is_active, created_at)
SELECT 'CS002', 'XYZ Cloud Services', 'SUPPLIER', 'TAX002', 'Yangon', 'Kyaw Kyaw',
       'xyz.cloud@gmail.com', '09222222222', TRUE, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM customers_suppliers WHERE party_code = 'CS002');

INSERT INTO customers_suppliers
(party_code, party_name, party_type, tax_id, address, contact_person,
 contact_email, contact_phone, is_active, created_at)
SELECT 'CS003', 'DEF Insurance', 'SUPPLIER', 'TAX003', 'Mandalay', 'Min Min',
       'def.insurance@gmail.com', '09333333333', TRUE, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM customers_suppliers WHERE party_code = 'CS003');

INSERT INTO customers_suppliers
(party_code, party_name, party_type, tax_id, address, contact_person,
 contact_email, contact_phone, is_active, created_at)
SELECT 'CS004', 'Northstar Consulting', 'CUSTOMER', 'TAX004', 'Yangon', 'Su Su',
       'northstar@gmail.com', '09444444444', TRUE, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM customers_suppliers WHERE party_code = 'CS004');

INSERT INTO customers_suppliers
(party_code, party_name, party_type, tax_id, address, contact_person,
 contact_email, contact_phone, is_active, created_at)
SELECT 'CS005', 'Thiri Win', 'CUSTOMER', 'TAX005', 'Yangon', 'Thiri Win',
       'thiriwin@gmail.com', '09555555555', TRUE, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM customers_suppliers WHERE party_code = 'CS005');

INSERT INTO customers_suppliers
(party_code, party_name, party_type, tax_id, address, contact_person,
 contact_email, contact_phone, is_active, created_at)
SELECT 'CS006', 'MNO Finance', 'CUSTOMER', 'TAX006', 'Mandalay', 'MNO Contact',
       'mno.finance@gmail.com', '09666666666', TRUE, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM customers_suppliers WHERE party_code = 'CS006');

INSERT INTO customers_suppliers
(party_code, party_name, party_type, tax_id, address, contact_person,
 contact_email, contact_phone, is_active, created_at)
SELECT 'CS007', 'ABC Technology', 'BOTH', 'TAX007', 'Yangon', 'ABC Contact',
       'abc.technology@gmail.com', '09777777777', TRUE, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM customers_suppliers WHERE party_code = 'CS007');

-- ---------------------------------------------------------------------------
-- Contract types
-- ---------------------------------------------------------------------------
INSERT INTO master_contract_type (code, name, is_active, sort_order)
SELECT 'CT001', 'Service', TRUE, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_type WHERE code = 'CT001');

INSERT INTO master_contract_type (code, name, is_active, sort_order)
SELECT 'CT002', 'Procurement', TRUE, 2 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_type WHERE code = 'CT002');

INSERT INTO master_contract_type (code, name, is_active, sort_order)
SELECT 'CT003', 'Employment', TRUE, 3 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_type WHERE code = 'CT003');

INSERT INTO master_contract_type (code, name, is_active, sort_order)
SELECT 'CT004', 'Consultancy', TRUE, 4 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_type WHERE code = 'CT004');

INSERT INTO master_contract_type (code, name, is_active, sort_order)
SELECT 'CT005', 'Licence', TRUE, 5 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_type WHERE code = 'CT005');

INSERT INTO master_contract_type (code, name, is_active, sort_order)
SELECT 'CT006', 'Other', TRUE, 6 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_type WHERE code = 'CT006');

-- ---------------------------------------------------------------------------
-- Contract categories
-- ---------------------------------------------------------------------------
INSERT INTO master_contract_category (code, name, is_active, sort_order)
SELECT 'CC001', 'Expense', TRUE, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_category WHERE code = 'CC001');

INSERT INTO master_contract_category (code, name, is_active, sort_order)
SELECT 'CC002', 'Revenue', TRUE, 2 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_category WHERE code = 'CC002');

INSERT INTO master_contract_category (code, name, is_active, sort_order)
SELECT 'CC003', 'Corporate', TRUE, 3 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_category WHERE code = 'CC003');

-- ---------------------------------------------------------------------------
-- Payment types
-- ---------------------------------------------------------------------------
INSERT INTO master_payment_type (code, name, is_active, sort_order)
SELECT 'PT001', 'Monthly', TRUE, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_payment_type WHERE code = 'PT001');

INSERT INTO master_payment_type (code, name, is_active, sort_order)
SELECT 'PT002', 'Quarterly', TRUE, 2 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_payment_type WHERE code = 'PT002');

INSERT INTO master_payment_type (code, name, is_active, sort_order)
SELECT 'PT003', 'Lump Sum', TRUE, 3 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_payment_type WHERE code = 'PT003');

INSERT INTO master_payment_type (code, name, is_active, sort_order)
SELECT 'PT004', 'Milestone-based', TRUE, 4 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_payment_type WHERE code = 'PT004');

INSERT INTO master_payment_type (code, name, is_active, sort_order)
SELECT 'PT005', 'Semi-Annual', TRUE, 5 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_payment_type WHERE code = 'PT005');

INSERT INTO master_payment_type (code, name, is_active, sort_order)
SELECT 'PT006', 'Annual', TRUE, 6 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_payment_type WHERE code = 'PT006');

-- ---------------------------------------------------------------------------
-- Contract statuses
-- ---------------------------------------------------------------------------
INSERT INTO master_contract_status (code, name, is_active, sort_order)
SELECT 'CS001', 'Active', TRUE, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_status WHERE code = 'CS001');

INSERT INTO master_contract_status (code, name, is_active, sort_order)
SELECT 'CS002', 'Completed', TRUE, 2 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_status WHERE code = 'CS002');

INSERT INTO master_contract_status (code, name, is_active, sort_order)
SELECT 'CS003', 'Suspended', TRUE, 3 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_status WHERE code = 'CS003');

INSERT INTO master_contract_status (code, name, is_active, sort_order)
SELECT 'CS004', 'Terminated', TRUE, 4 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_status WHERE code = 'CS004');

INSERT INTO master_contract_status (code, name, is_active, sort_order)
SELECT 'CS005', 'Waiting Sign', TRUE, 5 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_status WHERE code = 'CS005');

INSERT INTO master_contract_status (code, name, is_active, sort_order)
SELECT 'CS006', 'Expired', TRUE, 6 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_contract_status WHERE code = 'CS006');

-- ---------------------------------------------------------------------------
-- Payment statuses (names are used by the payment service)
-- ---------------------------------------------------------------------------
INSERT INTO master_payment_status (code, name, is_active, sort_order)
SELECT 'PS001', 'Pending', TRUE, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_payment_status WHERE code = 'PS001');

INSERT INTO master_payment_status (code, name, is_active, sort_order)
SELECT 'PS002', 'Paid', TRUE, 2 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_payment_status WHERE code = 'PS002');

INSERT INTO master_payment_status (code, name, is_active, sort_order)
SELECT 'PS003', 'Overdue', TRUE, 3 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_payment_status WHERE code = 'PS003');

INSERT INTO master_payment_status (code, name, is_active, sort_order)
SELECT 'PS004', 'Partially Paid', TRUE, 4 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_payment_status WHERE code = 'PS004');

-- ---------------------------------------------------------------------------
-- Currencies
-- Normalize older MMK001/USD001/JPY001/CNY001 codes first, then insert the
-- correct codes. The code is what the UI displays next to amounts.
-- ---------------------------------------------------------------------------
UPDATE master_currency c_old
LEFT JOIN master_currency c_new ON c_new.code = 'MMK'
SET c_old.code = 'MMK'
WHERE c_old.code = 'MMK001' AND c_new.id IS NULL;

UPDATE master_currency c_old
LEFT JOIN master_currency c_new ON c_new.code = 'USD'
SET c_old.code = 'USD'
WHERE c_old.code = 'USD001' AND c_new.id IS NULL;

UPDATE master_currency c_old
LEFT JOIN master_currency c_new ON c_new.code = 'JPY'
SET c_old.code = 'JPY'
WHERE c_old.code = 'JPY001' AND c_new.id IS NULL;

UPDATE master_currency c_old
LEFT JOIN master_currency c_new ON c_new.code = 'CNY'
SET c_old.code = 'CNY'
WHERE c_old.code = 'CNY001' AND c_new.id IS NULL;

INSERT INTO master_currency (code, name, is_active, sort_order)
SELECT 'MMK', 'MMK', TRUE, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_currency WHERE code = 'MMK');

INSERT INTO master_currency (code, name, is_active, sort_order)
SELECT 'USD', 'USD', TRUE, 2 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_currency WHERE code = 'USD');

INSERT INTO master_currency (code, name, is_active, sort_order)
SELECT 'JPY', 'JPY', TRUE, 3 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_currency WHERE code = 'JPY');

INSERT INTO master_currency (code, name, is_active, sort_order)
SELECT 'CNY', 'CNY', TRUE, 4 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_currency WHERE code = 'CNY');

-- ---------------------------------------------------------------------------
-- Agreement for
-- ---------------------------------------------------------------------------
INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG001', 'Service Level Agreement (SLA)', TRUE, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG001');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG002', 'Individual Agreement (IDA)', TRUE, 2 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG002');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG003', 'Memorandum of Understanding (MOU)', TRUE, 3 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG003');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG004', 'IT Service Maintenance Agreement (ISMA)', TRUE, 4 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG004');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG005', 'IT Service Support Agreement (ISS)', TRUE, 5 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG005');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG006', 'Purchaser Agreement (PUA)', TRUE, 6 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG006');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG007', 'Project Management Service Agreement (PMS)', TRUE, 7 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG007');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG008', 'IT Consulting Service Agreement (ICSA)', TRUE, 8 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG008');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG009', 'Basic Offshore Agreement (BOA)', TRUE, 9 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG009');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG010', 'Outsourcing Service Agreement (OSA)', TRUE, 10 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG010');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG011', 'Lease (LEA)', TRUE, 11 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG011');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG012', 'Loan Agreement (LOA)', TRUE, 12 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG012');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG013', 'Individual Loan Agreement (ILA)', TRUE, 13 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG013');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG014', 'Partnership Agreement (PA)', TRUE, 14 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG014');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG015', 'Joint Venture Agreement (JV)', TRUE, 15 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG015');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG016', 'Purchase Order (PO)', TRUE, 16 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG016');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG017', 'Office Management and Usage Agreement (ADM)', TRUE, 17 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG017');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG018', 'Non-disclosure Agreement (NDA)', TRUE, 18 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG018');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG019', 'Employment Agreement (EMP)', TRUE, 19 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG019');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG020', 'Letter of Collaboration (LOC)', TRUE, 20 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG020');

INSERT INTO master_agreement_for (code, name, is_active, sort_order)
SELECT 'AG021', 'Service Order Form (SOF)', TRUE, 21 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM master_agreement_for WHERE code = 'AG021');

COMMIT;
