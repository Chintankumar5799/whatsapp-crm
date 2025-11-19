-- Seed initial data for testing

-- Insert sample doctor
INSERT INTO doctors (name, email, phone, whatsapp_number, specialization, qualification, consultation_fee, is_active)
VALUES ('Dr. John Smith', 'dr.smith@example.com', '+1234567890', 'whatsapp:+1234567890', 
        'General Medicine', 'MBBS, MD', 500.00, true)
ON CONFLICT (email) DO NOTHING;

-- Insert sample patient
INSERT INTO patients (name, email, phone, whatsapp_number, date_of_birth, gender)
VALUES ('Jane Doe', 'jane.doe@example.com', '+9876543210', 'whatsapp:+9876543210',
        '1990-01-15', 'FEMALE')
ON CONFLICT DO NOTHING;

-- Insert sample service
INSERT INTO services (name, description, duration_minutes, price, is_active)
VALUES ('General Consultation', 'Standard consultation with doctor', 30, 500.00, true)
ON CONFLICT DO NOTHING;

-- Insert sample availability (Monday to Friday, 9 AM to 5 PM)
INSERT INTO availability (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes, is_active)
SELECT 
    d.id,
    day_name,
    '09:00:00'::TIME,
    '17:00:00'::TIME,
    30,
    true
FROM doctors d
CROSS JOIN (VALUES 
    ('MONDAY'),
    ('TUESDAY'),
    ('WEDNESDAY'),
    ('THURSDAY'),
    ('FRIDAY')
) AS days(day_name)
WHERE d.email = 'dr.smith@example.com'
ON CONFLICT DO NOTHING;

