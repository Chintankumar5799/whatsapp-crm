-- ==============================================================================
-- V19__enhance_clinic_billing_and_itemized_receipts.sql
-- In-Clinic POS Cash Desk, Itemized Procedures Billing, and Printable Receipts
-- ==============================================================================

-- 1. Modify payments table for clinic POS flexibility
ALTER TABLE payments
    ALTER COLUMN booking_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS clinic_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS consultation_id BIGINT,
    ADD COLUMN IF NOT EXISTS payment_mode VARCHAR(30) DEFAULT 'CASH',
    ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10, 2) DEFAULT 0.00;

-- 2. Modify invoices table for itemized clinic receipts
ALTER TABLE invoices
    ALTER COLUMN booking_id DROP NOT NULL,
    ALTER COLUMN payment_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS clinic_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS consultation_id BIGINT,
    ADD COLUMN IF NOT EXISTS payment_mode VARCHAR(30) DEFAULT 'CASH',
    ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10, 2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS patient_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS doctor_name VARCHAR(255);

-- 3. Create invoice_items table for procedure & consultation line items
CREATE TABLE IF NOT EXISTS invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_item_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_inv_item_invoice ON invoice_items(invoice_id);

-- 4. Add upi_id to clinics for dynamic on-screen QR generation
ALTER TABLE clinics
    ADD COLUMN IF NOT EXISTS upi_id VARCHAR(100) DEFAULT 'pms.clinic@upi';

