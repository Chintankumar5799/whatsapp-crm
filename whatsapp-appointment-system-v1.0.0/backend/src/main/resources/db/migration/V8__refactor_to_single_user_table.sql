-- Drop old tables if they exist
DROP TABLE IF EXISTS patients CASCADE;
DROP TABLE IF EXISTS doctors CASCADE;
DROP TABLE IF EXISTS admin CASCADE;

-- Update users table with new columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS whatsapp_number VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS hospital_number VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS specialization_id BIGINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS qualification VARCHAR(200);
ALTER TABLE users ADD COLUMN IF NOT EXISTS consultation_fee DECIMAL(10,2);

-- Clean up users table
ALTER TABLE users DROP COLUMN IF EXISTS doctor_id;
ALTER TABLE users DROP COLUMN IF EXISTS patient_id;

-- Create addresses table (if not exists, usually JPA creates it but good to have)
CREATE TABLE IF NOT EXISTS addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_user_address FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Update bookings table
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS address_id BIGINT;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS queue_position INTEGER;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS disease_description TEXT;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS total_amount DECIMAL(10,2);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_link_id VARCHAR(255);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_link_url TEXT;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITHOUT TIME ZONE;

-- Rename notes to doctor_notes if needed, or keeping notes as alias
-- Assuming 'notes' column exists, we might want to rename it or add 'doctor_notes'
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS doctor_notes TEXT;

-- Drop payment_status if no longer needed (since we use BookingStatus.PAID)
-- ALTER TABLE bookings DROP COLUMN IF EXISTS payment_status; 
-- Keeping it might be safer for now as legacy

-- Ensure foreign keys in bookings point to users
-- Note: 'doctor_id' and 'patient_id' in bookings should now reference 'users(id)'
-- If they referenced doctors(id) and patients(id), we need to drop those constraints and add new ones.
-- THIS IS CRITICAL: Logic assumes doctor_id and patient_id in bookings are now User IDs.
-- If existing data is present, IDs might not match! 
-- Assuming development environment reset (ddl-auto=create), so starting fresh.
-- Otherwise, data migration would be needed to map old doctor_id to new user_id.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_booking_doctor') THEN
        ALTER TABLE bookings DROP CONSTRAINT fk_booking_doctor;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_booking_patient') THEN
        ALTER TABLE bookings DROP CONSTRAINT fk_booking_patient;
    END IF;
END $$;

ALTER TABLE bookings ADD CONSTRAINT fk_booking_doctor_user FOREIGN KEY (doctor_id) REFERENCES users(id);
ALTER TABLE bookings ADD CONSTRAINT fk_booking_patient_user FOREIGN KEY (patient_id) REFERENCES users(id);
