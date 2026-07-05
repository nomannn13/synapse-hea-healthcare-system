CREATE TABLE invoices (
  id BINARY(16) PRIMARY KEY,
  invoice_number VARCHAR(40) NOT NULL UNIQUE,
  patient_id BINARY(16) NOT NULL,
  appointment_id BINARY(16),
  status VARCHAR(20) NOT NULL,
  subtotal DECIMAL(12,2) NOT NULL,
  tax_amount DECIMAL(12,2) NOT NULL,
  discount_amount DECIMAL(12,2) NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  due_date DATE,
  issued_at TIMESTAMP(6),
  paid_at TIMESTAMP(6),
  notes VARCHAR(1000),
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_invoice_patient FOREIGN KEY (patient_id) REFERENCES patient_profiles(id),
  CONSTRAINT fk_invoice_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
  INDEX idx_invoice_patient_status (patient_id, status)
) ENGINE=InnoDB;

CREATE TABLE invoice_items (
  id BINARY(16) PRIMARY KEY,
  invoice_id BINARY(16) NOT NULL,
  description VARCHAR(180) NOT NULL,
  quantity DECIMAL(10,2) NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  line_total DECIMAL(12,2) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_invoice_item_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE prescriptions (
  id BINARY(16) PRIMARY KEY,
  prescription_number VARCHAR(40) NOT NULL UNIQUE,
  patient_id BINARY(16) NOT NULL,
  doctor_id BINARY(16) NOT NULL,
  appointment_id BINARY(16),
  status VARCHAR(20) NOT NULL,
  notes VARCHAR(1000),
  issued_at TIMESTAMP(6),
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_prescription_patient FOREIGN KEY (patient_id) REFERENCES patient_profiles(id),
  CONSTRAINT fk_prescription_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profiles(id),
  CONSTRAINT fk_prescription_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
  INDEX idx_prescription_patient_issued (patient_id, issued_at)
) ENGINE=InnoDB;

CREATE TABLE prescription_items (
  id BINARY(16) PRIMARY KEY,
  prescription_id BINARY(16) NOT NULL,
  medication_name VARCHAR(180) NOT NULL,
  dosage VARCHAR(120) NOT NULL,
  frequency VARCHAR(120) NOT NULL,
  duration_days INT,
  instructions VARCHAR(500),
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_prescription_item_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE clinical_documents (
  id BINARY(16) PRIMARY KEY,
  patient_id BINARY(16) NOT NULL,
  uploaded_by BINARY(16) NOT NULL,
  category VARCHAR(40) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(120) NOT NULL UNIQUE,
  content_type VARCHAR(120) NOT NULL,
  size_bytes BIGINT NOT NULL,
  notes VARCHAR(700),
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_document_patient FOREIGN KEY (patient_id) REFERENCES patient_profiles(id),
  CONSTRAINT fk_document_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id),
  INDEX idx_document_patient_created (patient_id, created_at)
) ENGINE=InnoDB;
