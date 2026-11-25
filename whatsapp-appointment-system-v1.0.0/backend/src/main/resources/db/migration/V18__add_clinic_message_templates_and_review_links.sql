-- ==============================================================================
-- V18__add_clinic_message_templates_and_review_links.sql
-- Clinic-specific Editable Message Templates & Google Review Request Links
-- ==============================================================================

-- 1. Add google_review_url to clinics table
ALTER TABLE clinics
    ADD COLUMN IF NOT EXISTS google_review_url TEXT DEFAULT 'https://g.page/r/pms-clinic/review';

-- 2. Create message_templates table
CREATE TABLE IF NOT EXISTS message_templates (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL DEFAULT 1,
    template_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    template_body TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tpl_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE,
    CONSTRAINT uk_clinic_template_type UNIQUE (clinic_id, template_type)
);

CREATE INDEX IF NOT EXISTS idx_tpl_clinic ON message_templates(clinic_id);

-- 3. Seed default templates for clinic 1
INSERT INTO message_templates (clinic_id, template_type, title, template_body)
VALUES
(1, 'CONFIRMATION', 'Appointment Confirmation', 
 'Hello {patient_name}, your appointment at {clinic_name} with Dr. {doctor_name} is confirmed for {date} at {time}. Booking ID: {booking_id}. Helpline: {clinic_phone}.'),

(1, 'REMINDER_24H', '24-Hour Visit Reminder', 
 'Reminder: Dear {patient_name}, you have a consultation scheduled tomorrow ({date}) at {time} with Dr. {doctor_name} at {clinic_name}. Please arrive 10 minutes prior for vitals check.'),

(1, 'FOLLOW_UP', 'Doctor Revisit Recommendation', 
 'Dear {patient_name}, Dr. {doctor_name} recommended a follow-up consultation on {date}. Maintaining timely review ensures complete recovery. Reply YES to book your revisit.'),

(1, 'INQUIRY_RESPONSE', 'Patient Inquiry Acknowledgment', 
 'Hello {patient_name}, thank you for contacting {clinic_name}. We received your inquiry regarding {request_notes}. Our clinic desk will call you shortly, or call us at {clinic_phone}.'),

(1, 'PRESCRIPTION_SHARE', 'Digital Prescription Share', 
 'Dear {patient_name}, here is your digital prescription from Dr. {doctor_name} at {clinic_name}: {prescription_link}. Take medicines as advised. Get well soon!'),

(1, 'REVIEW_REQUEST', 'Google Review & Experience Feedback', 
 'Dear {patient_name}, thank you for visiting {clinic_name} today. We hope you had a comfortable consultation with Dr. {doctor_name}. Please take 30 seconds to share your feedback: {review_link}')
ON CONFLICT (clinic_id, template_type) DO NOTHING;

