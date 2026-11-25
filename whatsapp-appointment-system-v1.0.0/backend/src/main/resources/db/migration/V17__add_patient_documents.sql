-- ==============================================================================
-- V17__add_patient_documents.sql
-- Patient Document Attachments (Lab Reports, Scans, IDs)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS patient_documents (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL DEFAULT 1,
    patient_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL DEFAULT 'LAB_REPORT', -- LAB_REPORT, SCAN, PRESCRIPTION_SCAN, ID_PROOF, OTHER
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    file_data BYTEA,
    notes TEXT,
    uploaded_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_doc_patient ON patient_documents(patient_id);
CREATE INDEX IF NOT EXISTS idx_doc_clinic ON patient_documents(clinic_id);

