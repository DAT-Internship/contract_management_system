CREATE TABLE import_batch_errors (
  error_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  source_row INT NOT NULL,
  field_name VARCHAR(80),
  error_message TEXT NOT NULL,
  CONSTRAINT fk_import_batch_errors_batch
    FOREIGN KEY (batch_id) REFERENCES import_batches(batch_id) ON DELETE CASCADE
);
