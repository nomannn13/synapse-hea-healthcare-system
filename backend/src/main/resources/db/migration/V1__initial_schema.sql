CREATE TABLE users (
  id BINARY(16) PRIMARY KEY,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  phone VARCHAR(30),
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  INDEX idx_users_role_status (role, status)
) ENGINE=InnoDB;

CREATE TABLE departments (
  id BINARY(16) PRIMARY KEY,
  code VARCHAR(30) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL UNIQUE,
  description VARCHAR(500),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE patient_profiles (
  id BINARY(16) PRIMARY KEY,
  user_id BINARY(16) NOT NULL UNIQUE,
  date_of_birth DATE,
  blood_group VARCHAR(10),
  allergies VARCHAR(1000),
  emergency_contact VARCHAR(120),
  address VARCHAR(500),
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE doctor_profiles (
  id BINARY(16) PRIMARY KEY,
  user_id BINARY(16) NOT NULL UNIQUE,
  license_number VARCHAR(80) NOT NULL UNIQUE,
  specialization VARCHAR(120) NOT NULL,
  department_id BINARY(16) NOT NULL,
  biography VARCHAR(1500),
  consultation_minutes INT NOT NULL DEFAULT 30,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_doctor_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB;

CREATE TABLE availability_slots (
  id BINARY(16) PRIMARY KEY,
  doctor_id BINARY(16) NOT NULL,
  start_at TIMESTAMP(6) NOT NULL,
  end_at TIMESTAMP(6) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_slot_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profiles(id),
  INDEX idx_slot_doctor_time (doctor_id, start_at, end_at)
) ENGINE=InnoDB;

CREATE TABLE appointments (
  id BINARY(16) PRIMARY KEY,
  patient_id BINARY(16) NOT NULL,
  doctor_id BINARY(16) NOT NULL,
  department_id BINARY(16) NOT NULL,
  start_at TIMESTAMP(6) NOT NULL,
  end_at TIMESTAMP(6) NOT NULL,
  status VARCHAR(20) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  notes VARCHAR(1000),
  cancellation_reason VARCHAR(500),
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patient_profiles(id),
  CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profiles(id),
  CONSTRAINT fk_appointment_department FOREIGN KEY (department_id) REFERENCES departments(id),
  INDEX idx_appointment_doctor_time (doctor_id, start_at, end_at, status),
  INDEX idx_appointment_patient_time (patient_id, start_at)
) ENGINE=InnoDB;

CREATE TABLE medical_records (
  id BINARY(16) PRIMARY KEY,
  patient_id BINARY(16) NOT NULL,
  doctor_id BINARY(16) NOT NULL,
  appointment_id BINARY(16),
  diagnosis VARCHAR(500) NOT NULL,
  treatment VARCHAR(1500),
  prescription VARCHAR(1500),
  clinical_notes VARCHAR(3000),
  recorded_at TIMESTAMP(6) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_record_patient FOREIGN KEY (patient_id) REFERENCES patient_profiles(id),
  CONSTRAINT fk_record_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profiles(id),
  CONSTRAINT fk_record_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
  INDEX idx_medical_patient_time (patient_id, recorded_at)
) ENGINE=InnoDB;

CREATE TABLE notifications (
  id BINARY(16) PRIMARY KEY,
  user_id BINARY(16) NOT NULL,
  title VARCHAR(140) NOT NULL,
  message VARCHAR(1000) NOT NULL,
  type VARCHAR(40) NOT NULL,
  read_at TIMESTAMP(6),
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_notification_user_read (user_id, read_at)
) ENGINE=InnoDB;

CREATE TABLE urgent_cases (
  id BINARY(16) PRIMARY KEY,
  patient_id BINARY(16) NOT NULL,
  created_by BINARY(16) NOT NULL,
  priority VARCHAR(20) NOT NULL,
  heart_rate INT,
  oxygen_saturation INT,
  systolic_pressure INT,
  temperature DOUBLE,
  symptom_severity INT,
  reason VARCHAR(1000) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_urgent_patient FOREIGN KEY (patient_id) REFERENCES patient_profiles(id),
  CONSTRAINT fk_urgent_creator FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE hospital_resources (
  id BINARY(16) PRIMARY KEY,
  department_id BINARY(16) NOT NULL,
  type VARCHAR(30) NOT NULL,
  code VARCHAR(60) NOT NULL UNIQUE,
  name VARCHAR(140) NOT NULL,
  status VARCHAR(30) NOT NULL,
  notes VARCHAR(1000),
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_resource_department FOREIGN KEY (department_id) REFERENCES departments(id),
  INDEX idx_resource_dept_type_status (department_id, type, status)
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
  id BINARY(16) PRIMARY KEY,
  actor_id BINARY(16),
  action VARCHAR(30) NOT NULL,
  entity_type VARCHAR(80) NOT NULL,
  entity_id BINARY(16),
  details VARCHAR(1000),
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  INDEX idx_audit_actor_time (actor_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
  id BINARY(16) PRIMARY KEY,
  user_id BINARY(16) NOT NULL,
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP(6) NOT NULL,
  revoked_at TIMESTAMP(6),
  replaced_by_hash VARCHAR(64),
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_refresh_user (user_id)
) ENGINE=InnoDB;
