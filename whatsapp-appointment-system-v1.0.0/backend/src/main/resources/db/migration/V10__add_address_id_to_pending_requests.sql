ALTER TABLE pending_appointment_requests ADD COLUMN IF NOT EXISTS address_id BIGINT;

ALTER TABLE pending_appointment_requests DROP CONSTRAINT IF EXISTS fk_pending_request_address;
ALTER TABLE pending_appointment_requests ADD CONSTRAINT fk_pending_request_address FOREIGN KEY (address_id) REFERENCES addresses(id);

ALTER TABLE bookings ADD COLUMN IF NOT EXISTS address_id BIGINT;

ALTER TABLE bookings DROP CONSTRAINT IF EXISTS fk_booking_address;
ALTER TABLE bookings ADD CONSTRAINT fk_booking_address FOREIGN KEY (address_id) REFERENCES addresses(id);
