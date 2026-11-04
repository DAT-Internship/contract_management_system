ALTER TABLE approval_steps
    ADD COLUMN approver_id BIGINT NULL,
    ADD INDEX idx_approval_status_created (status, created_at),
    ADD CONSTRAINT fk_approval_approver FOREIGN KEY (approver_id) REFERENCES users(user_id);

-- Make old step rows visible as historical decisions when their status was already recorded.
