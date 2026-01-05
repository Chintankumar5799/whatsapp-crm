# Project Troubleshooting & Fixes Log

## 1. Database & Schema Issues

### ⚠️ Issue: Backend Startup Failed (`Schema-validation: missing column [address_id]`)
- **Cause**: The Java entity `PendingAppointmentRequest` logic was updated to use `addressId`, but the database table was missing the column. Hibernate validation failed.
- **Fix**: Created migration `V10__add_address_id_to_pending_requests.sql` to add columns and foreign keys.

### ⚠️ Issue: "Failed to save address" (500 Error / Table Missing)
- **Cause**: The `addresses` table might have been missing (if `V8` migration was skipped or failed), causing JPA queries to fail.
- **Fix**:
  1.  Created `V8_1__ensure_addresses_table.sql` to ensure the table exists (using `IF NOT EXISTS`).
  2.  Enabled `spring.flyway.out-of-order=true` in `application.yml` to allow this new migration to run even after newer versions (V9/V10) were detected.
  3.  Updated `V10` to be idempotent (safe to run multiple times).

### ⚠️ Issue: DB Constraint Violation (Nullability)
- **Cause**: The Java Entity `Address` allowed nulls for `state`, `postalCode`, `country`, but the Database (`V8` script) defined them as `NOT NULL`. This caused 500 errors when saving if fields were missing or deserialized defaults were null.
- **Fix**: Updated `Address.java` to add `@Column(nullable = false)` annotations, aligning Java with DB constraints.

---

## 2. Frontend & Integration Issues

### ⚠️ Issue: Doctor Dashboard "Manage Address" Not Opening
- **Cause**: The `<Dialog>` component for Address Manager was defined in `Dashboard.tsx` code logic but was **missing from the returned JSX**.
- **Fix**: Added the `<Dialog>` block inside the `return (...)` statement in `Dashboard.tsx`.

### ⚠️ Issue: Compilation Error "DialogActions is not defined"
- **Cause**: During the `Dashboard.tsx` fix, `DialogActions` was used but not imported.
- **Fix**: Added `DialogActions` to the import list in `Dashboard.tsx`.

### ⚠️ Issue: Address Save Failed (User ID Logic)
- **Cause**: Frontend code extracted `userId` as `parsedUser.userId`. In some cases, `parsedUser` had `id` instead, leading to `undefined` User ID sent to backend -> 500 Error/Bad Request.
- **Fix**: Updated logic to `const userId = parsedUser?.userId || parsedUser?.id;` in `BookingDashboard.tsx` and `Dashboard.tsx`.

### ⚠️ Issue: Address Display showing ", City" (Missing Street)
- **Cause**: `PatientBooking.tsx` tried to access `addr.street`, but the backend API returns `addr.addressLine1`.
- **Fix**: Updated `PatientBooking.tsx` to use `addr.addressLine1`.

---

## 3. Useful Commands & Scripts

### Restart Backend
Run this to apply migrations and reload code changes:
```bash
mvn spring-boot:run
```

### Manual Database Fix Script (PostgreSQL)
Run this if migrations fail or you want to force-sync the DB schema:
```sql
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

ALTER TABLE pending_appointment_requests ADD COLUMN IF NOT EXISTS address_id BIGINT;
ALTER TABLE pending_appointment_requests DROP CONSTRAINT IF EXISTS fk_pending_request_address;
ALTER TABLE pending_appointment_requests ADD CONSTRAINT fk_pending_request_address FOREIGN KEY (address_id) REFERENCES addresses(id);

ALTER TABLE bookings ADD COLUMN IF NOT EXISTS address_id BIGINT;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS fk_booking_address;
ALTER TABLE bookings ADD CONSTRAINT fk_booking_address FOREIGN KEY (address_id) REFERENCES addresses(id);
```
