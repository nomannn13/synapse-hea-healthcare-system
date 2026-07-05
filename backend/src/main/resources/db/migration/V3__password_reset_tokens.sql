CREATE TABLE password_reset_tokens (
  id BINARY(16) PRIMARY KEY,
  user_id BINARY(16) NOT NULL,
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP(6) NOT NULL,
  consumed_at TIMESTAMP(6),
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_password_reset_hash (token_hash)
) ENGINE=InnoDB;
