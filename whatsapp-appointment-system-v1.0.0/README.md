# WhatsApp Appointment & Billing System

A production-ready WhatsApp-based appointment booking and billing system built with Java Spring Boot, PostgreSQL, Redis, and React.

## Features

- **WhatsApp Integration**: Interactive booking via WhatsApp with button-based UI
- **Appointment Management**: Doctor approval workflow, slot management, Redis-based holds
- **Payment Processing**: Razorpay integration with payment links and webhooks
- **Invoice Generation**: Automatic PDF invoice generation on successful payment
- **Notifications**: Automated reminders (24h, 1h before appointment)
- **Doctor Dashboard**: React-based dashboard for managing appointments and availability
- **Security**: JWT authentication, role-based access control, audit logging

## Tech Stack

### Backend
- Java 17+
- Spring Boot 3.2.0
- Spring Data JPA
- PostgreSQL
- Redis (Lettuce)
- Flyway (Database Migrations)
- Razorpay SDK
- Twilio SDK (WhatsApp)
- iText7 (PDF Generation)
- Swagger/OpenAPI

### Frontend
- React with TypeScript
- Material UI
- Axios

### Infrastructure
- Docker & Docker Compose
- Maven

## Project Structure

```
.
├── backend/                 # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/appointment/
│   │   │   │   ├── payment/        # Payment module
│   │   │   │   ├── booking/        # Booking module
│   │   │   │   ├── whatsapp/       # WhatsApp integration
│   │   │   │   ├── notification/   # Notification service
│   │   │   │   └── auth/           # Authentication
│   │   │   └── resources/
│   │   │       ├── db/migration/   # Flyway migrations
│   │   │       └── application.yml
│   │   └── test/
│   └── pom.xml
├── frontend/               # React dashboard
├── docker-compose.yml      # Local development setup
└── README.md
```

## Payment System

The payment system is fully abstracted with a provider interface, making it easy to switch between payment providers (Razorpay, Stripe, etc.).

### Key Components

1. **PaymentProvider Interface**: Abstract interface for payment providers
2. **RazorpayPaymentProvider**: Razorpay implementation
3. **PaymentService**: Business logic for payment processing
4. **InvoiceService**: Invoice generation and management
5. **PdfInvoiceGenerator**: PDF invoice generation using iText7

### Payment Flow

1. Create payment link via `/api/payments/links` endpoint
2. Patient pays via Razorpay payment link
3. Razorpay sends webhook to `/api/webhooks/payments/razorpay`
4. System verifies webhook signature
5. Payment status updated in database
6. PDF invoice automatically generated
7. Invoice sent to patient via WhatsApp

### API Endpoints

#### Payment Management
- `POST /api/payments/links` - Create payment link
- `GET /api/payments/{paymentId}` - Get payment details
- `GET /api/payments/booking/{bookingId}` - Get payments for booking
- `GET /api/payments/patient/{patientId}` - Get payments for patient

#### Invoice Management
- `GET /api/payments/invoices/booking/{bookingId}` - Get invoices for booking
- `GET /api/payments/invoices/{invoiceId}/pdf` - Download invoice PDF

#### Webhooks
- `POST /api/webhooks/payments/razorpay` - Razorpay payment webhook

## Setup & Installation

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Node.js 18+ (for frontend)
- PostgreSQL 14+ (or use Docker)
- Redis 7+ (or use Docker)

### Environment Variables

Create a `.env` file in the root directory:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=appointment_db
DB_USER=appointment_user
DB_PASSWORD=appointment_pass

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Payment (Razorpay)
RAZORPAY_KEY_ID=your_razorpay_key_id
RAZORPAY_KEY_SECRET=your_razorpay_key_secret
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret

# WhatsApp (Twilio)
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=whatsapp:+1234567890

# JWT
JWT_SECRET=your-256-bit-secret-key-change-in-production-minimum-32-characters

# Server
SERVER_PORT=8080
```

### Running with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Manual Setup

#### Backend

```bash
cd backend

# Build
mvn clean install

# Run
mvn spring-boot:run
```

#### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Run development server
npm start
```

## Database Migrations

Flyway automatically runs migrations on application startup. Migrations are located in `backend/src/main/resources/db/migration/`.

## Testing

### Payment System Tests

```bash
cd backend
mvn test
```

### Integration Tests

Integration tests use Testcontainers for PostgreSQL and Redis.

## API Documentation

Once the application is running, access Swagger UI at:
- http://localhost:8080/api/swagger-ui.html
- API Docs: http://localhost:8080/api/api-docs

## Deployment

### Production Considerations

1. **Security**:
   - Change default JWT secret
   - Use environment variables for all secrets
   - Enable TLS/HTTPS
   - Configure proper CORS

2. **Database**:
   - Use connection pooling
   - Enable SSL for database connections
   - Regular backups

3. **Redis**:
   - Configure persistence
   - Set up Redis Sentinel/Cluster for HA

4. **Payment**:
   - Use production Razorpay keys
   - Configure webhook URL in Razorpay dashboard
   - Enable webhook signature verification

5. **WhatsApp**:
   - Use production Twilio credentials
   - Configure webhook URL in Twilio console

## Quick Start

1. **Clone the repository**
2. **Set up environment variables** (create `.env` file or export variables)
3. **Start services**: `docker-compose up -d`
4. **Access the application**:
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080/api
   - Swagger UI: http://localhost:8080/api/swagger-ui.html
5. **Login**: Use default admin credentials (username: `admin`, password: `admin123`)

## Known Limitations

1. Multi-doctor/multi-clinic mode requires additional configuration
2. Email fallback for notifications is optional (not implemented)
3. LLM integration for WhatsApp is placeholder only
4. Some hardcoded values (e.g., doctor ID in invoice) need to be fetched from booking
5. Frontend pages (Appointments, Availability, Billing) are basic placeholders and need full implementation

## Extending the System

### Adding a New Payment Provider

1. Implement the `PaymentProvider` interface
2. Add provider-specific configuration
3. Update `PaymentConfig` to register the new provider
4. Update `application.yml` with provider settings

### Adding Multi-Doctor Support

1. Update booking entity to include doctor relationship
2. Modify availability service to be doctor-specific
3. Update dashboard to filter by doctor
4. Add doctor selection in booking flow

## License

MIT License

## Support

For issues and questions, please open an issue on GitHub.

