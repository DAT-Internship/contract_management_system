-- Tracks when the recipient opened an email in Email Monitoring (drives the sidebar badge).
ALTER TABLE email_logs ADD COLUMN read_at DATETIME NULL;
UPDATE email_logs SET read_at = COALESCE(sent_at, created_at) WHERE status = 'SENT_READ';
CREATE INDEX idx_email_logs_recipient_read ON email_logs (recipient_email, read_at);
