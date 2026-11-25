-- ==============================================================================
-- V20__seed_demo_clinic_staff_and_sample_patients.sql
-- Seed ready-to-test staff accounts, sample patients, OPD queue tokens and CRM leads
-- ==============================================================================

-- Ensure users table has timestamps default
ALTER TABLE users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

-- 1. Seed doctor and receptionist users (Password for all: admin123)
INSERT INTO users (username, email, password, phone, role, clinic_id, name, registration_number, qualification, consultation_fee, old_case_fee, is_active, created_at, updated_at)
VALUES ('dr.smith', 'dr.smith@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ5a', '+919876543210', 'DOCTOR', 1, 'Dr. John Smith', 'MCI-2021-99881', 'MBBS, MD (Medicine)', 500.00, 300.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, email, password, phone, role, clinic_id, name, is_active, created_at, updated_at)
VALUES ('reception', 'reception@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ5a', '+919876543211', 'RECEPTIONIST', 1, 'Sarah Jenkins', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Ensure admin has full doctor & staff privileges and clinic_id = 1
UPDATE users 
SET clinic_id = 1, name = 'Clinic Administrator', consultation_fee = 500.00, old_case_fee = 300.00 
WHERE username = 'admin';

-- 2. Seed sample patients for instant testing
INSERT INTO patients (clinic_id, patient_uid, name, phone, email, age, gender, blood_group, allergies, chronic_diseases, total_visits_count, last_visit_date)
VALUES 
(1, 'PMS-MAIN-2026-00001', 'Rahul Sharma', '+919876543210', 'rahul.sharma@example.com', 38, 'MALE', 'B+', 'Penicillin', 'Hypertension', 3, CURRENT_TIMESTAMP - INTERVAL '10 days'),
(1, 'PMS-MAIN-2026-00002', 'Priya Patel', '+919876543211', 'priya.patel@example.com', 29, 'FEMALE', 'O+', 'None', 'None', 1, CURRENT_TIMESTAMP - INTERVAL '40 days'),
(1, 'PMS-MAIN-2026-00003', 'Amit Verma', '+919876543212', 'amit.verma@example.com', 45, 'MALE', 'A+', 'Dust, Sulfa', 'Type 2 Diabetes', 2, CURRENT_TIMESTAMP - INTERVAL '5 days')
ON CONFLICT (clinic_id, patient_uid) DO NOTHING;

-- 3. Seed sample OPD queue tokens for today
INSERT INTO opd_queue (doctor_id, booking_id, patient_id, queue_date, queue_number, status, arrival_time)
VALUES 
(1, NULL, 1, CURRENT_DATE, 1, 'WAITING', CURRENT_TIMESTAMP - INTERVAL '20 minutes'),
(1, NULL, 3, CURRENT_DATE, 2, 'WAITING', CURRENT_TIMESTAMP - INTERVAL '10 minutes')
ON CONFLICT (doctor_id, queue_date, queue_number) DO NOTHING;

-- 4. Seed sample CRM leads
INSERT INTO patient_inquiries (clinic_id, name, phone, request_description, source, status)
VALUES 
(1, 'Ananya Sen', '+919876543220', 'Inquiring about consultation timings for diabetic neuropathy and second opinion.', 'GOOGLE_BUSINESS', 'NEW'),
(1, 'Vikas Gupta', '+919876543221', 'Wants to schedule an annual preventive health checkup and blood profile.', 'WHATSAPP', 'CONTACTED')
ON CONFLICT DO NOTHING;

