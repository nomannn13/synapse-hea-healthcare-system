CREATE DATABASE IF NOT EXISTS hea_database
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE hea_database;

CREATE TABLE IF NOT EXISTS users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(20),
  national_id VARCHAR(50) UNIQUE,
  role ENUM('patient','doctor','nurse','admin') NOT NULL DEFAULT 'patient',
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS departments (
  department_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  floor_number INT,
  head_doctor_id INT NULL,
  phone VARCHAR(20),
  description TEXT
);

CREATE TABLE IF NOT EXISTS patients (
  patient_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE,
  date_of_birth DATE,
  gender ENUM('male','female','other'),
  blood_type VARCHAR(5),
  address TEXT,
  emergency_contact VARCHAR(100),
  emergency_phone VARCHAR(20),
  insurance_number VARCHAR(100),
  CONSTRAINT fk_patients_user
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS doctors (
  doctor_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE,
  department_id INT NULL,
  specialization VARCHAR(100) NOT NULL,
  license_number VARCHAR(100) UNIQUE,
  years_experience INT NOT NULL DEFAULT 0,
  is_available BOOLEAN NOT NULL DEFAULT TRUE,
  consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  CONSTRAINT fk_doctors_user
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_doctors_department
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE SET NULL
);


CREATE TABLE IF NOT EXISTS doctor_schedules (
  schedule_id INT AUTO_INCREMENT PRIMARY KEY,
  doctor_id INT NOT NULL,
  day_of_week ENUM('Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday') NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  is_available BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uq_doctor_schedule UNIQUE (doctor_id, day_of_week),
  CONSTRAINT chk_schedule_time CHECK (start_time < end_time),
  CONSTRAINT fk_schedules_doctor
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS resources (
  resource_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  type ENUM('bed','room','equipment') NOT NULL,
  department_id INT NULL,
  status ENUM('available','occupied','maintenance') NOT NULL DEFAULT 'available',
  location VARCHAR(100),
  description TEXT,
  last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_resources_department
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS appointments (
  appointment_id INT AUTO_INCREMENT PRIMARY KEY,
  patient_id INT NOT NULL,
  doctor_id INT NOT NULL,
  appointment_date DATE NOT NULL,
  appointment_time TIME NOT NULL,
  duration_minutes INT NOT NULL DEFAULT 30,
  status ENUM('pending','confirmed','rejected','completed','cancelled','rescheduled')
    NOT NULL DEFAULT 'pending',
  reason TEXT,
  notes TEXT,
  room_id INT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_appointments_patient
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
  CONSTRAINT fk_appointments_doctor
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
  CONSTRAINT fk_appointments_room
    FOREIGN KEY (room_id) REFERENCES resources(resource_id) ON DELETE SET NULL,
  INDEX idx_appointment_slot (doctor_id, appointment_date, appointment_time),
  INDEX idx_appointment_patient (patient_id, appointment_date)
);

CREATE TABLE IF NOT EXISTS medical_records (
  record_id INT AUTO_INCREMENT PRIMARY KEY,
  patient_id INT NOT NULL,
  doctor_id INT NOT NULL,
  appointment_id INT NULL,
  diagnosis TEXT NOT NULL,
  symptoms TEXT,
  treatment_plan TEXT,
  prescription TEXT,
  test_results TEXT,
  blood_pressure VARCHAR(20),
  heart_rate INT,
  temperature DECIMAL(4,1),
  weight DECIMAL(5,2),
  height DECIMAL(5,2),
  notes TEXT,
  triage_category ENUM('emergency','urgent','non-urgent') NOT NULL DEFAULT 'non-urgent',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_records_patient
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
  CONSTRAINT fk_records_doctor
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
  CONSTRAINT fk_records_appointment
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id) ON DELETE SET NULL,
  INDEX idx_records_patient (patient_id, created_at),
  INDEX idx_records_doctor (doctor_id, created_at)
);

CREATE TABLE IF NOT EXISTS staff_shifts (
  shift_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  department_id INT NULL,
  shift_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  shift_type ENUM('morning','afternoon','night') NOT NULL,
  status ENUM('scheduled','active','completed','cancelled') NOT NULL DEFAULT 'scheduled',
  created_by INT NULL,
  CONSTRAINT chk_shift_time CHECK (start_time < end_time),
  CONSTRAINT fk_shifts_user
    FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT fk_shifts_department
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE SET NULL,
  CONSTRAINT fk_shifts_creator
    FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS audit_logs (
  log_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NULL,
  action VARCHAR(100) NOT NULL,
  entity_type VARCHAR(50),
  entity_id INT,
  old_value JSON,
  new_value JSON,
  ip_address VARCHAR(45),
  user_agent TEXT,
  status ENUM('success','failed','suspicious') NOT NULL DEFAULT 'success',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_user
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
  INDEX idx_audit_user_time (user_id, created_at),
  INDEX idx_audit_status_time (status, created_at)
);

CREATE TABLE IF NOT EXISTS notifications (
  notification_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  title VARCHAR(200) NOT NULL,
  message TEXT NOT NULL,
  type ENUM('appointment','cancellation','urgent','schedule','system') NOT NULL DEFAULT 'system',
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notifications_user
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  INDEX idx_notifications_user_read (user_id, is_read, created_at)
);

CREATE TABLE IF NOT EXISTS billing (
  bill_id INT AUTO_INCREMENT PRIMARY KEY,
  patient_id INT NOT NULL,
  appointment_id INT NULL,
  amount DECIMAL(10,2) NOT NULL,
  description TEXT,
  status ENUM('pending','paid','cancelled') NOT NULL DEFAULT 'pending',
  payment_date TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_billing_patient
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
  CONSTRAINT fk_billing_appointment
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id) ON DELETE SET NULL
);

INSERT IGNORE INTO departments (name, floor_number, phone) VALUES
  ('Emergency', 1, '100'),
  ('Cardiology', 2, '101'),
  ('Neurology', 3, '102'),
  ('Orthopedics', 4, '103'),
  ('General Medicine', 1, '104'),
  ('Pediatrics', 2, '105');

INSERT INTO resources (name, type, status, location)
SELECT 'Room A', 'room', 'available', 'Floor 1'
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE name = 'Room A');

INSERT INTO resources (name, type, status, location)
SELECT 'Room B', 'room', 'available', 'Floor 2'
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE name = 'Room B');

INSERT INTO resources (name, type, status, location)
SELECT 'Bed 101', 'bed', 'available', 'Room A'
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE name = 'Bed 101');

INSERT INTO resources (name, type, status, location)
SELECT 'X-Ray Machine', 'equipment', 'available', 'Radiology'
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE name = 'X-Ray Machine');
