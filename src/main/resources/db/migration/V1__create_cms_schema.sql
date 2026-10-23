CREATE TABLE `departments` (
  `department_id` int PRIMARY KEY AUTO_INCREMENT,
  `dept_code` varchar(20) UNIQUE NOT NULL,
  `dept_name` varchar(120) NOT NULL,
  `parent_dept_id` int,
  `is_active` boolean NOT NULL DEFAULT true,
  `created_at` datetime DEFAULT (now())
);

CREATE TABLE `users` (
  `user_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `employee_code` varchar(20) UNIQUE NOT NULL,
  `full_name` varchar(120) NOT NULL,
  `email` varchar(120) UNIQUE NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `department_id` int NOT NULL,
  `role_id` int NOT NULL,
  `is_active` boolean NOT NULL DEFAULT true,
  `last_login_at` datetime,
  `created_at` datetime DEFAULT (now()),
  `updated_at` datetime DEFAULT (now())
);

CREATE TABLE `roles` (
  `role_id` int PRIMARY KEY AUTO_INCREMENT,
  `role_code` varchar(30) UNIQUE NOT NULL COMMENT 'ADMIN | PMO | APPROVER | CONTRACT_MANAGER | FINANCE | PM',
  `role_name` varchar(80) NOT NULL,
  `description` varchar(255)
);

CREATE TABLE `master_agreement_for` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `code` varchar(20) UNIQUE NOT NULL COMMENT 'CUSTOMER | SUPPLIER | CORPORATE',
  `name` varchar(80) NOT NULL,
  `is_active` boolean DEFAULT true,
  `sort_order` int DEFAULT 0
);

CREATE TABLE `master_contract_category` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `code` varchar(20) UNIQUE NOT NULL COMMENT 'EXPENSE | REVENUE | CORPORATE',
  `name` varchar(80) NOT NULL,
  `is_active` boolean DEFAULT true,
  `sort_order` int DEFAULT 0
);

CREATE TABLE `master_contract_type` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `code` varchar(30) UNIQUE NOT NULL,
  `name` varchar(120) NOT NULL,
  `is_active` boolean DEFAULT true,
  `sort_order` int DEFAULT 0
);

CREATE TABLE `master_contract_status` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `code` varchar(30) UNIQUE NOT NULL COMMENT 'ACTIVE | COMPLETED | SUSPENDED | TERMINATED | WAITING_SIGN | RENEWED',
  `name` varchar(80) NOT NULL,
  `is_active` boolean DEFAULT true,
  `sort_order` int DEFAULT 0
);

CREATE TABLE `master_payment_type` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `code` varchar(30) UNIQUE NOT NULL COMMENT 'MONTHLY | QUARTERLY | LUMP_SUM | MILESTONE',
  `name` varchar(80) NOT NULL,
  `is_active` boolean DEFAULT true,
  `sort_order` int DEFAULT 0
);

CREATE TABLE `master_payment_status` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `code` varchar(30) UNIQUE NOT NULL COMMENT 'PENDING | PAID | OVERDUE | PARTIALLY PAID',
  `name` varchar(80) NOT NULL,
  `is_active` boolean DEFAULT true,
  `sort_order` int DEFAULT 0
);

CREATE TABLE `master_currency` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `code` varchar(10) UNIQUE NOT NULL COMMENT 'MMK | USD | JPY | CNY | EUR',
  `name` varchar(80) NOT NULL,
  `is_active` boolean DEFAULT true,
  `sort_order` int DEFAULT 0
);

CREATE TABLE `customers_suppliers` (
  `party_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `party_code` varchar(20) UNIQUE NOT NULL,
  `party_name` varchar(200) NOT NULL,
  `party_type` varchar(20) NOT NULL COMMENT 'CUSTOMER | SUPPLIER | CORPORATE',
  `tax_id` varchar(40),
  `address` text,
  `contact_person` varchar(120),
  `contact_email` varchar(120),
  `contact_phone` varchar(30),
  `is_active` boolean DEFAULT true,
  `created_at` datetime DEFAULT (now())
);

CREATE TABLE `projects` (
  `project_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `project_code` varchar(30) UNIQUE NOT NULL,
  `project_name` varchar(200) NOT NULL,
  `department_id` int,
  `project_manager` bigint,
  `project_rank` varchar(10),
  `start_date` date,
  `end_date` date,
  `status` varchar(20) DEFAULT 'ACTIVE',
  `created_at` datetime DEFAULT (now())
);

CREATE TABLE `contracts` (
  `contract_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `contract_no` varchar(30) UNIQUE NOT NULL,
  `renewed_from_id` bigint,
  `agreement_for_id` int NOT NULL,
  `category_id` int NOT NULL,
  `contract_type_id` int NOT NULL,
  `payment_type_id` int NOT NULL,
  `status_id` int NOT NULL,
  `department_id` int NOT NULL,
  `fiscal_year` varchar(10) NOT NULL,
  `contract_po_no` varchar(40) NOT NULL,
  `po_no_ref` varchar(40),
  `project_id` bigint,
  `party_id` bigint NOT NULL,
  `description` text NOT NULL,
  `signed_date` date,
  `register_date` date NOT NULL DEFAULT (curdate()),
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `period_months` int,
  `headcount_per_month` decimal(8,2),
  `total_man_months` decimal(10,2) NOT NULL DEFAULT 0,
  `currency_id` int NOT NULL,
  `contract_value` decimal(18,2) NOT NULL DEFAULT 0,
  `commercial_tax_rate` decimal(5,2) NOT NULL DEFAULT 5,
  `commercial_tax_amount` decimal(18,2),
  `missing_attachment` varchar(3) NOT NULL DEFAULT 'NO' COMMENT 'YES | NO',
  `payment_risk` varchar(10) NOT NULL DEFAULT 'LOW' COMMENT 'LOW | MEDIUM | HIGH',
  `renewal_risk` varchar(10) NOT NULL DEFAULT 'LOW' COMMENT 'LOW | MEDIUM | HIGH',
  `project_incharge_id` bigint,
  `contract_manager_id` bigint,
  `client_incharge` varchar(120),
  `created_by` bigint NOT NULL,
  `updated_by` bigint,
  `created_at` datetime DEFAULT (now()),
  `updated_at` datetime DEFAULT (now())
);

CREATE TABLE `contract_attachments` (
  `attachment_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `contract_id` bigint NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_path` varchar(500) NOT NULL,
  `file_size` bigint NOT NULL,
  `mime_type` varchar(80) NOT NULL,
  `uploaded_by` bigint NOT NULL,
  `uploaded_at` datetime DEFAULT (now())
);

CREATE TABLE `invoices` (
  `invoice_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `invoice_no` varchar(40) UNIQUE,
  `contract_id` bigint,
  `party_id` bigint,
  `description` text,
  `invoice_date` date NOT NULL,
  `due_date` date NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `tax_amount` decimal(18,2) DEFAULT 0,
  `total_amount` decimal(18,2) NOT NULL,
  `currency_id` int NOT NULL,
  `paid_amount` decimal(18,2) DEFAULT 0,
  `status_id` int,
  `paid_date` date,
  `updated_by` bigint,
  `created_at` datetime DEFAULT (now()),
  `updated_at` datetime DEFAULT (now())
);

CREATE TABLE `approval_steps` (
  `step_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `contract_id` bigint NOT NULL,
  `party_id` bigint NOT NULL,
  `submitted_by` bigint NOT NULL,
  `current_approval` varchar(50),
  `status` varchar(10),
  `comment` text,
  `created_at` datetime,
  `updated_at` datetime
);

CREATE TABLE `audit_logs` (
  `log_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entity_type` varchar(40) NOT NULL,
  `entity_id` bigint NOT NULL,
  `action` varchar(40) NOT NULL,
  `actor_id` bigint,
  `old_value` json,
  `new_value` json,
  `ip_address` varchar(45),
  `created_at` datetime DEFAULT (now())
);

CREATE TABLE `notifications` (
  `notification_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` varchar(40) NOT NULL COMMENT 'CONTRACT_EXPIRY_90D | PAYMENT_DUE | APPROVAL_REQ | CONTRACT_RETURNED',
  `severity` varchar(20) NOT NULL COMMENT 'INFO | WARNING | CRITICAL',
  `title` varchar(200) NOT NULL,
  `message` text NOT NULL,
  `entity_type` varchar(40),
  `entity_id` bigint,
  `action_url` varchar(255),
  `is_read` boolean NOT NULL DEFAULT false,
  `read_at` datetime,
  `created_at` datetime DEFAULT (now())
);

CREATE TABLE `email_logs` (
  `email_log_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `notification_id` bigint,
  `recipient_email` varchar(255) NOT NULL,
  `subject` varchar(255) NOT NULL,
  `email_type` varchar(40) NOT NULL COMMENT 'CONTRACT_EXPIRY | PAYMENT_DUE | APPROVAL | CONTRACT_RETURNED',
  `contract_id` bigint,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | SENT | FAILED',
  `attempt_count` int NOT NULL DEFAULT 0,
  `last_attempt_at` datetime,
  `sent_at` datetime,
  `error_message` text,
  `created_at` datetime DEFAULT (now())
);

CREATE TABLE `contract_renewals` (
  `renewal_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `original_contract_id` bigint NOT NULL,
  `renewed_contract_id` bigint,
  `renewal_due_date` date NOT NULL,
  `renewal_status` varchar(30) NOT NULL DEFAULT 'UPCOMING' COMMENT 'UPCOMING | IN_PROGRESS | RENEWED | NOT_RENEWED | CANCELLED',
  `renewal_reason` text,
  `remarks` text,
  `initiated_by` bigint,
  `reviewed_by` bigint,
  `created_at` datetime DEFAULT (now()),
  `updated_at` datetime DEFAULT (now())
);

CREATE TABLE `import_batches` (
  `batch_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `batch_type` varchar(30) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_path` varchar(500) NOT NULL,
  `total_rows` int,
  `success_rows` int,
  `status` varchar(20),
  `uploaded_by` bigint,
  `created_at` datetime DEFAULT (now())
);

CREATE INDEX `idx_users_dept` ON `users` (`department_id`);

CREATE INDEX `idx_party_name` ON `customers_suppliers` (`party_name`);

CREATE INDEX `idx_party_type` ON `customers_suppliers` (`party_type`);

CREATE INDEX `idx_contracts_status_end_date` ON `contracts` (`status_id`, `end_date`);

CREATE INDEX `idx_contracts_party` ON `contracts` (`party_id`);

CREATE INDEX `idx_contracts_dept` ON `contracts` (`department_id`);

CREATE INDEX `idx_contracts_fy` ON `contracts` (`fiscal_year`);

CREATE INDEX `idx_contracts_pm` ON `contracts` (`project_incharge_id`);

CREATE INDEX `idx_contracts_creator` ON `contracts` (`created_by`);

CREATE INDEX `idx_attachments_contract` ON `contract_attachments` (`contract_id`);

CREATE INDEX `idx_invoice_no` ON `invoices` (`invoice_no`);

CREATE INDEX `idx_invoice_status_due` ON `invoices` (`status_id`, `due_date`);

CREATE INDEX `idx_invoice_contract` ON `invoices` (`contract_id`);

CREATE INDEX `idx_audit_entity` ON `audit_logs` (`entity_type`, `entity_id`);

CREATE INDEX `idx_audit_actor` ON `audit_logs` (`actor_id`);

CREATE INDEX `idx_audit_created` ON `audit_logs` (`created_at`);

CREATE INDEX `idx_notifications_user_read` ON `notifications` (`user_id`, `is_read`);

CREATE INDEX `idx_notifications_entity` ON `notifications` (`entity_type`, `entity_id`);

CREATE INDEX `idx_email_logs_status_created` ON `email_logs` (`status`, `created_at`);

CREATE INDEX `idx_email_logs_contract` ON `email_logs` (`contract_id`);

CREATE INDEX `idx_email_logs_notification` ON `email_logs` (`notification_id`);

CREATE INDEX `idx_renewal_original` ON `contract_renewals` (`original_contract_id`);

CREATE INDEX `idx_renewal_due_status` ON `contract_renewals` (`renewal_due_date`, `renewal_status`);

CREATE INDEX `idx_renewal_new_contract` ON `contract_renewals` (`renewed_contract_id`);

ALTER TABLE `departments` ADD FOREIGN KEY (`parent_dept_id`) REFERENCES `departments` (`department_id`);

ALTER TABLE `users` ADD FOREIGN KEY (`department_id`) REFERENCES `departments` (`department_id`);

ALTER TABLE `users` ADD FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`);

ALTER TABLE `projects` ADD FOREIGN KEY (`department_id`) REFERENCES `departments` (`department_id`);

ALTER TABLE `projects` ADD FOREIGN KEY (`project_manager`) REFERENCES `users` (`user_id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`renewed_from_id`) REFERENCES `contracts` (`contract_id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`agreement_for_id`) REFERENCES `master_agreement_for` (`id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`category_id`) REFERENCES `master_contract_category` (`id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`contract_type_id`) REFERENCES `master_contract_type` (`id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`payment_type_id`) REFERENCES `master_payment_type` (`id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`status_id`) REFERENCES `master_contract_status` (`id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`department_id`) REFERENCES `departments` (`department_id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`project_id`) REFERENCES `projects` (`project_id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`party_id`) REFERENCES `customers_suppliers` (`party_id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`currency_id`) REFERENCES `master_currency` (`id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`project_incharge_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`contract_manager_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `contracts` ADD FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `contract_attachments` ADD FOREIGN KEY (`contract_id`) REFERENCES `contracts` (`contract_id`);

ALTER TABLE `contract_attachments` ADD FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `invoices` ADD FOREIGN KEY (`contract_id`) REFERENCES `contracts` (`contract_id`);

ALTER TABLE `invoices` ADD FOREIGN KEY (`party_id`) REFERENCES `customers_suppliers` (`party_id`);

ALTER TABLE `invoices` ADD FOREIGN KEY (`currency_id`) REFERENCES `master_currency` (`id`);

ALTER TABLE `invoices` ADD FOREIGN KEY (`status_id`) REFERENCES `master_payment_status` (`id`);

ALTER TABLE `invoices` ADD FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `approval_steps` ADD FOREIGN KEY (`contract_id`) REFERENCES `contracts` (`contract_id`);

ALTER TABLE `approval_steps` ADD FOREIGN KEY (`party_id`) REFERENCES `customers_suppliers` (`party_id`);

ALTER TABLE `approval_steps` ADD FOREIGN KEY (`submitted_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `audit_logs` ADD FOREIGN KEY (`actor_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `notifications` ADD FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `email_logs` ADD FOREIGN KEY (`notification_id`) REFERENCES `notifications` (`notification_id`);

ALTER TABLE `email_logs` ADD FOREIGN KEY (`contract_id`) REFERENCES `contracts` (`contract_id`);

ALTER TABLE `contract_renewals` ADD FOREIGN KEY (`original_contract_id`) REFERENCES `contracts` (`contract_id`);

ALTER TABLE `contract_renewals` ADD FOREIGN KEY (`renewed_contract_id`) REFERENCES `contracts` (`contract_id`);

ALTER TABLE `contract_renewals` ADD FOREIGN KEY (`initiated_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `contract_renewals` ADD FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`user_id`);

ALTER TABLE `import_batches` ADD FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`user_id`);
