SET @now = UTC_TIMESTAMP(6);

INSERT INTO invoices (id,invoice_number,patient_id,appointment_id,status,subtotal,tax_amount,discount_amount,total_amount,currency,due_date,issued_at,paid_at,notes,created_at,updated_at,version) VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000401'),'HEA-2026-000001',UUID_TO_BIN('00000000-0000-0000-0000-000000000103'),NULL,'ISSUED',95.00,19.00,0.00,114.00,'EUR',DATE_ADD(UTC_DATE(), INTERVAL 14 DAY),@now,NULL,'Demo outpatient consultation invoice',@now,@now,0);

INSERT INTO invoice_items (id,invoice_id,description,quantity,unit_price,line_total,created_at,updated_at,version) VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000411'),UUID_TO_BIN('00000000-0000-0000-0000-000000000401'),'Cardiology consultation',1.00,65.00,65.00,@now,@now,0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000412'),UUID_TO_BIN('00000000-0000-0000-0000-000000000401'),'ECG assessment',1.00,30.00,30.00,@now,@now,0);

INSERT INTO prescriptions (id,prescription_number,patient_id,doctor_id,appointment_id,status,notes,issued_at,created_at,updated_at,version) VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000501'),'RX-2026-000001',UUID_TO_BIN('00000000-0000-0000-0000-000000000103'),UUID_TO_BIN('00000000-0000-0000-0000-000000000102'),NULL,'ISSUED','Demo prescription for the Phase 2 workspace',@now,@now,@now,0);

INSERT INTO prescription_items (id,prescription_id,medication_name,dosage,frequency,duration_days,instructions,created_at,updated_at,version) VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000511'),UUID_TO_BIN('00000000-0000-0000-0000-000000000501'),'Demo Medicine A','10 mg','Once daily',14,'Take after breakfast',@now,@now,0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000512'),UUID_TO_BIN('00000000-0000-0000-0000-000000000501'),'Demo Medicine B','5 mg','As needed',7,'Do not exceed two doses per day',@now,@now,0);
