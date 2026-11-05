-- Sample invoice seed for the payment workspace.
--
-- Requires:
--   - the master data seed (payment statuses, currencies, parties)
--   - the four sample contracts below
--
-- Change the contract numbers if your contracts use different numbers.
-- Safe to re-run: an invoice is inserted only when its invoice_no is missing.
--
-- Usage:
--   mariadb --skip-ssl -ucmsuser -proot cms_system < src/main/resources/db/seed/invoices.sql

START TRANSACTION;

SET @contract_1 = 'SAMPLE-2026-0001';
SET @contract_2 = 'SAMPLE-2026-0002';
SET @contract_3 = 'SAMPLE-2026-0003';
SET @contract_4 = 'SAMPLE-2026-0004';

SET @status_pending = (
    SELECT id FROM master_payment_status
    WHERE UPPER(TRIM(name)) = 'PENDING' LIMIT 1
);
SET @status_paid = (
    SELECT id FROM master_payment_status
    WHERE UPPER(TRIM(name)) = 'PAID' LIMIT 1
);
SET @status_overdue = (
    SELECT id FROM master_payment_status
    WHERE UPPER(TRIM(name)) = 'OVERDUE' LIMIT 1
);
SET @status_partial = (
    SELECT id FROM master_payment_status
    WHERE UPPER(TRIM(name)) = 'PARTIALLY PAID' LIMIT 1
);

SET @actor_id = COALESCE(
    (SELECT user_id FROM users WHERE employee_code = '26-00001' LIMIT 1),
    (SELECT user_id FROM users WHERE employee_code = '25-00001' LIMIT 1)
);

-- ---------------------------------------------------------------------------
-- Contract 1: Pending, Partially Paid, Paid, Overdue
-- ---------------------------------------------------------------------------
INSERT INTO invoices
(invoice_no, contract_id, party_id, description, invoice_date, due_date,
 amount, tax_amount, total_amount, currency_id, paid_amount, status_id,
 paid_date, updated_by, created_at, updated_at)
SELECT 'INV-2026-0001', c.contract_id, c.party_id, 'Milestone 1',
       DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY),
       DATE_ADD(CURRENT_DATE, INTERVAL 25 DAY),
       25000.00, 0.00, 25000.00, c.currency_id, 0.00, @status_pending,
       NULL, @actor_id, NOW(), NOW()
FROM contracts c
WHERE c.contract_no = @contract_1
  AND c.currency_id IS NOT NULL
  AND @status_pending IS NOT NULL
  AND @actor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_no = 'INV-2026-0001');

INSERT INTO invoices
(invoice_no, contract_id, party_id, description, invoice_date, due_date,
 amount, tax_amount, total_amount, currency_id, paid_amount, status_id,
 paid_date, updated_by, created_at, updated_at)
SELECT 'INV-2026-0002', c.contract_id, c.party_id, 'Milestone 2',
       DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY),
       DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY),
       25000.00, 0.00, 25000.00, c.currency_id, 10000.00, @status_partial,
       DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), @actor_id, NOW(), NOW()
FROM contracts c
WHERE c.contract_no = @contract_1
  AND c.currency_id IS NOT NULL
  AND @status_partial IS NOT NULL
  AND @actor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_no = 'INV-2026-0002');

INSERT INTO invoices
(invoice_no, contract_id, party_id, description, invoice_date, due_date,
 amount, tax_amount, total_amount, currency_id, paid_amount, status_id,
 paid_date, updated_by, created_at, updated_at)
SELECT 'INV-2026-0003', c.contract_id, c.party_id, 'Annual service fee',
       DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY),
       DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY),
       25000.00, 0.00, 25000.00, c.currency_id, 25000.00, @status_paid,
       DATE_SUB(CURRENT_DATE, INTERVAL 35 DAY), @actor_id, NOW(), NOW()
FROM contracts c
WHERE c.contract_no = @contract_1
  AND c.currency_id IS NOT NULL
  AND @status_paid IS NOT NULL
  AND @actor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_no = 'INV-2026-0003');

INSERT INTO invoices
(invoice_no, contract_id, party_id, description, invoice_date, due_date,
 amount, tax_amount, total_amount, currency_id, paid_amount, status_id,
 paid_date, updated_by, created_at, updated_at)
SELECT 'INV-2026-0004', c.contract_id, c.party_id, 'Support renewal',
       DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY),
       DATE_SUB(CURRENT_DATE, INTERVAL 40 DAY),
       25000.00, 0.00, 25000.00, c.currency_id, 0.00, @status_overdue,
       NULL, @actor_id, NOW(), NOW()
FROM contracts c
WHERE c.contract_no = @contract_1
  AND c.currency_id IS NOT NULL
  AND @status_overdue IS NOT NULL
  AND @actor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_no = 'INV-2026-0004');

-- ---------------------------------------------------------------------------
-- Contract 2: Pending, Partially Paid, Paid, Overdue
-- ---------------------------------------------------------------------------
INSERT INTO invoices
(invoice_no, contract_id, party_id, description, invoice_date, due_date,
 amount, tax_amount, total_amount, currency_id, paid_amount, status_id,
 paid_date, updated_by, created_at, updated_at)
SELECT 'INV-2026-0005', c.contract_id, c.party_id, 'Cloud subscription Q1',
       DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY),
       DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY),
       12000.00, 0.00, 12000.00, c.currency_id, 0.00, @status_pending,
       NULL, @actor_id, NOW(), NOW()
FROM contracts c
WHERE c.contract_no = @contract_2
  AND c.currency_id IS NOT NULL
  AND @status_pending IS NOT NULL
  AND @actor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_no = 'INV-2026-0005');

INSERT INTO invoices
(invoice_no, contract_id, party_id, description, invoice_date, due_date,
 amount, tax_amount, total_amount, currency_id, paid_amount, status_id,
 paid_date, updated_by, created_at, updated_at)
SELECT 'INV-2026-0006', c.contract_id, c.party_id, 'Cloud subscription Q2',
       DATE_SUB(CURRENT_DATE, INTERVAL 25 DAY),
       DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY),
       12000.00, 0.00, 12000.00, c.currency_id, 5000.00, @status_partial,
       DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), @actor_id, NOW(), NOW()
FROM contracts c
WHERE c.contract_no = @contract_2
  AND c.currency_id IS NOT NULL
  AND @status_partial IS NOT NULL
  AND @actor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_no = 'INV-2026-0006');

INSERT INTO invoices
(invoice_no, contract_id, party_id, description, invoice_date, due_date,
 amount, tax_amount, total_amount, currency_id, paid_amount, status_id,
 paid_date, updated_by, created_at, updated_at)
SELECT 'INV-2026-0007', c.contract_id, c.party_id, 'Cloud setup fee',
       DATE_SUB(CURRENT_DATE, INTERVAL 50 DAY),
       DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY),
       12000.00, 0.00, 12000.00, c.currency_id, 12000.00, @status_paid,
       DATE_SUB(CURRENT_DATE, INTERVAL 25 DAY), @actor_id, NOW(), NOW()
FROM contracts c
WHERE c.contract_no = @contract_2
  AND c.currency_id IS NOT NULL
  AND @status_paid IS NOT NULL
  AND @actor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_no = 'INV-2026-0007');

INSERT INTO invoices
(invoice_no, contract_id, party_id, description, invoice_date, due_date,
 amount, tax_amount, total_amount, currency_id, paid_amount, status_id,
 paid_date, updated_by, created_at, updated_at)
SELECT 'INV-2026-0008', c.contract_id, c.party_id, 'Cloud overage',
       DATE_SUB(CURRENT_DATE, INTERVAL 70 DAY),
       DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY),
       12000.00, 0.00, 12000.00, c.currency_id, 0.00, @status_overdue,
       NULL, @actor_id, NOW(), NOW()
FROM contracts c
WHERE c.contract_no = @contract_2
  AND c.currency_id IS NOT NULL
  AND @status_overdue IS NOT NULL
  AND @actor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_no = 'INV-2026-0008');

-- ---------------------------------------------------------------------------
-- Contract 3: Pending and Overdue with a partial payment
-- ---------------------------------------------------------------------------
INSERT INTO invoices
(invoice_no, contract_id, party_id, description, invoice_date, due_date,
 amount, tax_amount, total_amount, currency_id, paid_amount, status_id,
 paid_date, updated_by, created_at, updated_at)
SELECT 'INV-2026-0009', c.contract_id, c.party_id, 'Insurance premium 1',
       DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY),
       DATE_ADD(CURRENT_DATE, INTERVAL 45 DAY),
       50000.00, 0.00, 50000.00, c.currency_id, 0.00, @status_pending,
       NULL, @actor_id, NOW(), NOW()
FROM contracts c
WHERE c.contract_no = @contract_3
  AND c.currency_id IS NOT NULL
  AND @status_pending IS NOT NULL
  AND @actor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_no = 'INV-2026-0009');

INSERT INTO invoices
(invoice_no, contract_id, party_id, description, invoice_date, due_date,
 amount, tax_amount, total_amount, currency_id, paid_amount, status_id,
 paid_date, updated_by, created_at, updated_at)
SELECT 'INV-2026-0010', c.contract_id, c.party_id, 'Insurance premium 2',
       DATE_SUB(CURRENT_DATE, INTERVAL 80 DAY),
       DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY),
       50000.00, 0.00, 50000.00, c.currency_id, 15000.00, @status_overdue,
       DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), @actor_id, NOW(), NOW()
FROM contracts c
WHERE c.contract_no = @contract_3
  AND c.currency_id IS NOT NULL
  AND @status_overdue IS NOT NULL
  AND @actor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_no = 'INV-2026-0010');

-- ---------------------------------------------------------------------------
-- Contract 4: Partially Paid and Paid
-- ---------------------------------------------------------------------------
INSERT INTO invoices
(invoice_no, contract_id, party_id, description, invoice_date, due_date,
 amount, tax_amount, total_amount, currency_id, paid_amount, status_id,
 paid_date, updated_by, created_at, updated_at)
SELECT 'INV-2026-0011', c.contract_id, c.party_id, 'Consulting phase 1',
       DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY),
       DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY),
       20000.00, 0.00, 20000.00, c.currency_id, 8000.00, @status_partial,
       DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), @actor_id, NOW(), NOW()
FROM contracts c
WHERE c.contract_no = @contract_4
  AND c.currency_id IS NOT NULL
  AND @status_partial IS NOT NULL
  AND @actor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_no = 'INV-2026-0011');

INSERT INTO invoices
(invoice_no, contract_id, party_id, description, invoice_date, due_date,
 amount, tax_amount, total_amount, currency_id, paid_amount, status_id,
 paid_date, updated_by, created_at, updated_at)
SELECT 'INV-2026-0012', c.contract_id, c.party_id, 'Consulting phase 2',
       DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY),
       DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY),
       20000.00, 0.00, 20000.00, c.currency_id, 20000.00, @status_paid,
       DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), @actor_id, NOW(), NOW()
FROM contracts c
WHERE c.contract_no = @contract_4
  AND c.currency_id IS NOT NULL
  AND @status_paid IS NOT NULL
  AND @actor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_no = 'INV-2026-0012');

COMMIT;
