-- Doctors table
CREATE TABLE IF NOT EXISTS doctors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(20) NOT NULL,
    whatsapp_number VARCHAR(20),
    specialization VARCHAR(100),
    qualification VARCHAR(200),
    consultation_fee DECIMAL(10, 2),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Patients table
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20) NOT NULL,
    whatsapp_number VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(10),
    address VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Services table
CREATE TABLE IF NOT EXISTS services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    duration_minutes INTEGER NOT NULL DEFAULT 30,
    price DECIMAL(10, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Availability table (regular weekly schedule)
CREATE TABLE IF NOT EXISTS availability (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER NOT NULL DEFAULT 30,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_availability_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- Availability exceptions (blocked dates, modified hours)
CREATE TABLE IF NOT EXISTS availability_exceptions (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    exception_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    exception_type VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exception_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    CONSTRAINT unique_doctor_exception_date UNIQUE (doctor_id, exception_date)
);

-- Slots table (generated slots for booking)
CREATE TABLE IF NOT EXISTS slots (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_slot_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- Bookings table
CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_number VARCHAR(50) UNIQUE NOT NULL,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    slot_id BIGINT,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes VARCHAR(1000),
    requires_payment BOOLEAN NOT NULL DEFAULT true,
    payment_status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    CONSTRAINT fk_booking_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES slots(id) ON DELETE SET NULL
);

-- Audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    action VARCHAR(50) NOT NULL,
    user_id BIGINT,
    user_type VARCHAR(20),
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for better query performance
CREATE INDEX idx_availability_doctor_id ON availability(doctor_id);
CREATE INDEX idx_availability_day ON availability(day_of_week);

CREATE INDEX idx_exceptions_doctor_date ON availability_exceptions(doctor_id, exception_date);

CREATE INDEX idx_slots_doctor_date ON slots(doctor_id, slot_date);
CREATE INDEX idx_slots_status ON slots(status);
CREATE INDEX idx_slots_date_status ON slots(slot_date, status);

CREATE INDEX idx_bookings_doctor_id ON bookings(doctor_id);
CREATE INDEX idx_bookings_patient_id ON bookings(patient_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_date ON bookings(booking_date);
CREATE INDEX idx_bookings_booking_number ON bookings(booking_number);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id, user_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

