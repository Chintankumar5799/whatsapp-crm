# Master Project Prompt

Use this prompt to generate a similar Appointment Booking & CRM System with all the features currently implemented.

---

**Role:** Expert Full-Stack Developer & System Architect.
**Task:** Build a comprehensive "WhatsApp-integrated Appointment & Billing System" for Doctors and Patients.

## 1. Technology Stack
- **Backend:** Java 17+, Spring Boot 3.x (Web, JPA, Security, Validation, WebSocket, Mail, Redis).
- **Database:** PostgreSQL with Flyway for schema migrations.
- **Frontend:** React 18 (TypeScript), Material UI (MUI) v5, Recharts, Axios.
- **Tools:** Docker (optional), Stripe/Razorpay (Payments), Twilio/Meta API (WhatsApp).

## 2. Core Architecture & Database
- **Single User Table Strategy:** Refactor `Doctor` and `Patient` into a unified `users` table with a `role` enum (`DOCTOR`, `PATIENT`, `ADMIN`).
  - *Fields:* `id`, `username`, `password` (BCrypt), `role`, `whatsapp_number`, `specialization`, `consultation_fee`, `hospital_number`.
- **Address Management:**
  - Create an `addresses` table linked to `users`.
  - Support multiple addresses per user with an `is_primary` flag.
  - APIs: CRUD endpoints (`GET /addresses/user/{id}`, `POST`, `PUT`, `DELETE`).
- **Booking Schema:**
  - `pending_appointment_requests`: Stores initial requests (Status: PENDING, APPROVED, REJECTED). Must include `address_id` to link patient location.
  - `bookings`: Confirmed appointments. Links to `address_id`.
  - `slots`: (Optional) or Logic-based availability.

## 3. Key Features & Business Logic

### A. Authentication & Roles
- Implement JWT Authentication.
- Registration endpoints for both Patients (self-signup) and Doctors (admin-created or verify flow).
- Password hashing using BCrypt.

### B. Patient Booking Flow
- **Dashboard:** View doctors, view history.
- **Booking Wizard:**
  1.  Select Doctor & Specialization.
  2.  Select Date & Time Slot (Check availability/conflicts).
  3.  **Address Selection:** Fetch user's saved addresses or add a new one inline. Display addresses in a dropdown.
  4.  **Submit Request:** Create a `PendingAppointmentRequest` with user ID, doctor ID, time, and **Address ID**.

### C. Doctor Dashboard & Workflow
- **Metrics:** Show daily slots booked, revenue, patients attended (Bar/Line charts).
- **Real-Time Requests:** Use **WebSocket** (`/topic/pending-requests`) to notify doctor of new bookings instantly.
- **Request Management:**
  - View list of "Pending Requests".
  - **View Address:** Add a button to view the Patient's selected address details (Street, City, Zip) in a modal.
  - **Actions:** "Confirm" (Moves to Bookings table) or "Reject" (with remarks).
- **Payment Generation:** Button on confirmed bookings to "Generate Payment Link" (Stripe/Razorpay integration).

### D. Address Management Module
- Create a dedicated component (`AddressManager.tsx`) for reuse in Dashboards and Booking flows.
- Enforce validation: Fields like `addressLine1`, `city`, `state`, `postalCode`, `country` must be non-null in Backend.
- Logic: When adding the first address, auto-set `is_primary = true`.

### E. Payment Integration
- Integration with Stripe/Razorpay.
- API to generate payment links (`POST /payments/links`).
- Webhook listener to update booking status to `PAID` automatically upon success.

## 4. Implementation Guidelines
- **DTO Pattern:** Use Request/Response DTOs (e.g., `PatientBookingRequest` with `addressId`).
- **Error Handling:** Global Exception Handler. Ensure generic 500 errors return meaningful messages in debug mode.
- **Security:** method-level security (`@PreAuthorize`) and CORS configuration (`localhost:3000`).
- **Validation:** Use `@NotNull` on Entity fields to match DB constraints.

## 5. Specific Fixes & Lessons Learned (Reference)
- *Schema Validation:* Always ensure Flyway migrations match Entity definitions (e.g., `nullable=false`).
- *Migration Order:* Use `flyway.out-of-order=true` if inserting migrations (like `V8_1`) between existing ones.
- *Frontend:* Ensure Dialog/Modal components are strictly rendered in the JSX return block.
