-- ==============================================================================
-- V15__add_communication_logs_and_consents.sql
-- Communication Logs, Message Delivery Audit & Patient Privacy Consents
-- ==============================================================================

-- 1. Create communication_logs table
CREATE TABLE IF NOT EXISTS communication_logs (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL DEFAULT 1,
    patient_id BIGINT,
    booking_id BIGINT,
    recipient_phone VARCHAR(30) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP',
    message_type VARCHAR(50) NOT NULL,
    message_body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    external_message_id VARCHAR(100),
    error_message TEXT,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comm_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comm_clinic_date ON communication_logs(clinic_id, sent_at);
CREATE INDEX IF NOT EXISTS idx_comm_recipient ON communication_logs(recipient_phone);

-- 2. Create patient_consents table
CREATE TABLE IF NOT EXISTS patient_consents (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL DEFAULT 1,
    patient_id BIGINT NOT NULL,
    consent_type VARCHAR(50) NOT NULL, -- REMINDERS, AUTOMATED_CALLS, PROMOTIONAL
    is_granted BOOLEAN NOT NULL DEFAULT true,
    consent_method VARCHAR(50) DEFAULT 'IN_PERSON_REGISTRATION', -- IN_PERSON, SMS_OPTIN, WEB
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    withdrawn_at TIMESTAMP,
    CONSTRAINT fk_consent_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE,
    CONSTRAINT fk_consent_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT uk_patient_consent_type UNIQUE (patient_id, consent_type)
);

CREATE INDEX IF NOT EXISTS idx_consents_patient ON patient_consents(patient_id, consent_type);

