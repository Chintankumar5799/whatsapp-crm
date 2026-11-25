-- ==============================================================================
-- V14__add_clinic_and_pms_core.sql
-- Multi-Tenancy Baseline, Master Patient Registry & Clinical PMS Core
-- ==============================================================================

-- 1. Create clinics table
CREATE TABLE IF NOT EXISTS clinics (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    logo_url TEXT,
    phone VARCHAR(20),
    email VARCHAR(100),
    whatsapp_number VARCHAR(20),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'India',
    prescription_header_html TEXT,
    prescription_footer_html TEXT,
    follow_up_validity_days INTEGER NOT NULL DEFAULT 30,
    new_case_consultation_fee DECIMAL(10, 2) DEFAULT 500.00,
    old_case_consultation_fee DECIMAL(10, 2) DEFAULT 300.00,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_clinics_code ON clinics(code);

-- Seed default initial clinic for backward compatibility
INSERT INTO clinics (id, code, name, phone, email, follow_up_validity_days, new_case_consultation_fee, old_case_consultation_fee)
VALUES (1, 'PMS-MAIN', 'PMS Healthcare Clinic', '+919876543210', 'contact@pms.com', 30, 500.00, 300.00)
ON CONFLICT (id) DO NOTHING;

-- 2. Add clinic_id and doctor registration number to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS clinic_id BIGINT DEFAULT 1;
ALTER TABLE users ADD COLUMN IF NOT EXISTS registration_number VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS old_case_fee DECIMAL(10, 2);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_users_clinic') THEN
        ALTER TABLE users ADD CONSTRAINT fk_users_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 3. Add clinic_id and case_type to bookings
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS clinic_id BIGINT DEFAULT 1;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS case_type VARCHAR(20) DEFAULT 'NEW_CASE';
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS appointment_source VARCHAR(50) DEFAULT 'ONLINE';
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS fee_override BOOLEAN DEFAULT false;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS fee_override_reason VARCHAR(255);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_bookings_clinic') THEN
        ALTER TABLE bookings ADD CONSTRAINT fk_bookings_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 4. Re-create / Ensure master patients table
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL DEFAULT 1,
    patient_uid VARCHAR(50),
    name VARCHAR(120),
    phone VARCHAR(20),
    whatsapp_number VARCHAR(20),
    email VARCHAR(120),
    date_of_birth DATE,
    age INTEGER,
    gender VARCHAR(15),
    blood_group VARCHAR(10),
    address VARCHAR(500),
    allergies TEXT,
    chronic_diseases TEXT,
    last_visit_date TIMESTAMP,
    total_visits_count INTEGER NOT NULL DEFAULT 0,
    is_merged BOOLEAN NOT NULL DEFAULT false,
    merged_into_patient_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- If patients table already existed from earlier schema, ensure all PMS columns are present
ALTER TABLE patients ADD COLUMN IF NOT EXISTS clinic_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS patient_uid VARCHAR(50);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS age INTEGER;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS blood_group VARCHAR(10);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS address VARCHAR(500);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS allergies TEXT;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS chronic_diseases TEXT;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS last_visit_date TIMESTAMP;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS total_visits_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS is_merged BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS merged_into_patient_id BIGINT;

UPDATE patients 
SET patient_uid = 'PMS-MAIN-' || TO_CHAR(COALESCE(created_at, CURRENT_TIMESTAMP), 'YYYY') || '-' || LPAD(id::text, 5, '0')
WHERE patient_uid IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_patients_clinic') THEN
        ALTER TABLE patients ADD CONSTRAINT fk_patients_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'uk_clinic_patient_uid') THEN
        ALTER TABLE patients ADD CONSTRAINT uk_clinic_patient_uid UNIQUE (clinic_id, patient_uid);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_patients_clinic_phone ON patients(clinic_id, phone);
CREATE INDEX IF NOT EXISTS idx_patients_clinic_name ON patients(clinic_id, name);

-- 5. Create consultations table
CREATE TABLE IF NOT EXISTS consultations (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL DEFAULT 1,
    booking_id BIGINT,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    case_type VARCHAR(20) NOT NULL DEFAULT 'NEW_CASE',
    consultation_fee DECIMAL(10, 2) NOT NULL,
    fee_override BOOLEAN NOT NULL DEFAULT false,
    fee_override_reason VARCHAR(255),
    chief_complaint TEXT NOT NULL,
    bp_systolic INTEGER,
    bp_diastolic INTEGER,
    pulse INTEGER,
    temperature DECIMAL(4, 1),
    weight DECIMAL(5, 2),
    height DECIMAL(5, 2),
    spo2 INTEGER,
    clinical_examination TEXT,
    diagnosis TEXT NOT NULL,
    doctor_private_notes TEXT,
    is_finalized BOOLEAN NOT NULL DEFAULT true,
    finalized_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_consultation_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE,
    CONSTRAINT fk_consultation_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL,
    CONSTRAINT fk_consultation_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_consultation_doctor FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_consultations_patient ON consultations(patient_id);
CREATE INDEX IF NOT EXISTS idx_consultations_doctor ON consultations(doctor_id);

-- 6. Create medicines catalogue table
CREATE TABLE IF NOT EXISTS medicines (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(150) NOT NULL,
    brand_name VARCHAR(150),
    dosage_form VARCHAR(50) NOT NULL DEFAULT 'TABLET',
    strength VARCHAR(50),
    composition VARCHAR(255),
    is_custom BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_medicines_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_medicines_search ON medicines(clinic_id, name);

-- 7. Create prescription_items table
CREATE TABLE IF NOT EXISTS prescription_items (
    id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL,
    medicine_id BIGINT,
    custom_medicine_name VARCHAR(150),
    dosage VARCHAR(50) NOT NULL,
    frequency VARCHAR(50),
    duration_days INTEGER NOT NULL DEFAULT 5,
    instructions VARCHAR(255),
    quantity INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prescription_consultation FOREIGN KEY (consultation_id) REFERENCES consultations(id) ON DELETE CASCADE,
    CONSTRAINT fk_prescription_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_prescription_items_consultation ON prescription_items(consultation_id);

-- 8. Create opd_queue table
CREATE TABLE IF NOT EXISTS opd_queue (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL DEFAULT 1,
    doctor_id BIGINT NOT NULL,
    booking_id BIGINT,
    patient_id BIGINT NOT NULL,
    queue_number INTEGER NOT NULL,
    queue_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    arrival_time TIMESTAMP,
    consultation_start_time TIMESTAMP,
    consultation_end_time TIMESTAMP,
    priority_flag BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_queue_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE,
    CONSTRAINT fk_queue_doctor FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_queue_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL,
    CONSTRAINT fk_queue_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT uk_doctor_queue_date_number UNIQUE (doctor_id, queue_date, queue_number)
);

CREATE INDEX IF NOT EXISTS idx_opd_queue_date_doctor ON opd_queue(doctor_id, queue_date, status);

-- 9. Create follow_ups table (Retention Engine)
CREATE TABLE IF NOT EXISTS follow_ups (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL DEFAULT 1,
    consultation_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    due_date DATE NOT NULL,
    interval_preset VARCHAR(20) DEFAULT '7D',
    reason VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    contact_status VARCHAR(30) NOT NULL DEFAULT 'NOT_CONTACTED',
    assigned_staff_id BIGINT,
    contact_attempt_count INTEGER NOT NULL DEFAULT 0,
    last_contact_attempt_at TIMESTAMP,
    resulting_booking_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_followup_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE,
    CONSTRAINT fk_followup_consultation FOREIGN KEY (consultation_id) REFERENCES consultations(id) ON DELETE CASCADE,
    CONSTRAINT fk_followup_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_followup_doctor FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_followup_staff FOREIGN KEY (assigned_staff_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_followup_booking FOREIGN KEY (resulting_booking_id) REFERENCES bookings(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_followups_due_date ON follow_ups(clinic_id, due_date, status);

-- 10. Create patient_inquiries table (CRM Engine)
CREATE TABLE IF NOT EXISTS patient_inquiries (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    request_description TEXT,
    source VARCHAR(50) NOT NULL DEFAULT 'PHONE_CALL',
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    assigned_staff_id BIGINT,
    next_follow_up_date DATE,
    converted_patient_id BIGINT,
    converted_booking_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inquiry_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE,
    CONSTRAINT fk_inquiry_staff FOREIGN KEY (assigned_staff_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_inquiry_patient FOREIGN KEY (converted_patient_id) REFERENCES patients(id) ON DELETE SET NULL,
    CONSTRAINT fk_inquiry_booking FOREIGN KEY (converted_booking_id) REFERENCES bookings(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_inquiries_clinic_status ON patient_inquiries(clinic_id, status);

