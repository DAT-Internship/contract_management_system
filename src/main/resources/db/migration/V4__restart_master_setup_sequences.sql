-- Reset empty Master Setup tables without lowering counters below existing IDs.
ALTER TABLE departments AUTO_INCREMENT = 1;
ALTER TABLE customers_suppliers AUTO_INCREMENT = 1;
ALTER TABLE master_agreement_for AUTO_INCREMENT = 1;
ALTER TABLE master_contract_category AUTO_INCREMENT = 1;
ALTER TABLE master_contract_type AUTO_INCREMENT = 1;
ALTER TABLE master_contract_status AUTO_INCREMENT = 1;
ALTER TABLE master_payment_type AUTO_INCREMENT = 1;
ALTER TABLE master_payment_status AUTO_INCREMENT = 1;
ALTER TABLE master_currency AUTO_INCREMENT = 1;
