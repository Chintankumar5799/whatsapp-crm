CREATE TABLE IF NOT EXISTS pending_appointment_requests (
    id BIGSERIAL PRIMARY KEY,
    doctor_phone VARCHAR(20),
    patient_phone VARCHAR(20),
    patient_name VARCHAR(100),
    requested_date DATE,
    requested_start_time VARCHAR(20),
    description TEXT,
    address_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pending_request_address FOREIGN KEY (address_id) REFERENCES addresses(id)
);
