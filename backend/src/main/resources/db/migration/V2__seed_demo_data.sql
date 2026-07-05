SET @now = UTC_TIMESTAMP(6);
SET @password = '$2y$12$GL/4.akh1dttb/wW2Y43D.INweRyHnUrY3P5h8EeW8SjHwrxfbXPq';

INSERT INTO departments (id, code, name, description, active, created_at, updated_at, version) VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000201'), 'CARD', 'Cardiology', 'Heart and cardiovascular care', TRUE, @now, @now, 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000202'), 'GEN', 'General Medicine', 'Primary and internal medicine', TRUE, @now, @now, 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000203'), 'NEUR', 'Neurology', 'Nervous system care', TRUE, @now, @now, 0);

INSERT INTO users (id,email,password_hash,first_name,last_name,phone,role,status,created_at,updated_at,version) VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000001'),'admin@synapse.local',@password,'System','Administrator','+39000000001','ADMIN','ACTIVE',@now,@now,0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000002'),'doctor@synapse.local',@password,'Elena','Rossi','+39000000002','DOCTOR','ACTIVE',@now,@now,0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000003'),'patient@synapse.local',@password,'Marco','Bianchi','+39000000003','PATIENT','ACTIVE',@now,@now,0);

INSERT INTO doctor_profiles (id,user_id,license_number,specialization,department_id,biography,consultation_minutes,active,created_at,updated_at,version) VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000102'),UUID_TO_BIN('00000000-0000-0000-0000-000000000002'),'IT-MED-2026-001','Cardiologist',UUID_TO_BIN('00000000-0000-0000-0000-000000000201'),'Cardiologist focused on preventive care and clear patient communication.',30,TRUE,@now,@now,0);

INSERT INTO patient_profiles (id,user_id,date_of_birth,blood_group,allergies,emergency_contact,address,created_at,updated_at,version) VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000103'),UUID_TO_BIN('00000000-0000-0000-0000-000000000003'),'1998-04-12','O+','None recorded','Giulia Bianchi +39000000999','Trento, Italy',@now,@now,0);

INSERT INTO hospital_resources (id,department_id,type,code,name,status,notes,created_at,updated_at,version) VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000301'),UUID_TO_BIN('00000000-0000-0000-0000-000000000201'),'ROOM','CARD-R01','Cardiology Consultation Room 1','AVAILABLE','Demo resource',@now,@now,0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000302'),UUID_TO_BIN('00000000-0000-0000-0000-000000000202'),'BED','GEN-B01','General Ward Bed 1','AVAILABLE','Demo resource',@now,@now,0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000303'),UUID_TO_BIN('00000000-0000-0000-0000-000000000201'),'EQUIPMENT','CARD-ECG-01','ECG Unit 1','AVAILABLE','Demo resource',@now,@now,0);
