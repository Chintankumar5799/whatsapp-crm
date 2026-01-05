ALTER TABLE pending_appointment_requests DROP COLUMN IF EXISTS patient_id;
ALTER TABLE pending_appointment_requests DROP COLUMN IF EXISTS status;
ALTER TABLE pending_appointment_requests DROP COLUMN IF EXISTS updated_at;
