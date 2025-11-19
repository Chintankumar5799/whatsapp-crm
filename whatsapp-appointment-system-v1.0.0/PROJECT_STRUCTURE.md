# Project Structure

## Overview

This is a complete Spring Boot application for WhatsApp Appointment & Billing System.

## Directory Structure

```
whatsapp-appointment-system/
├── backend/                          # Spring Boot Backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/appointment/
│   │   │   │   ├── AppointmentApplication.java
│   │   │   │   ├── auth/            # Authentication module
│   │   │   │   │   ├── config/      # Security configuration
│   │   │   │   │   ├── controller/  # Auth controllers
│   │   │   │   │   ├── dto/         # Data transfer objects
│   │   │   │   │   ├── model/       # User entity
│   │   │   │   │   ├── repository/  # User repository
│   │   │   │   │   ├── security/    # JWT filter
│   │   │   │   │   └── service/     # JWT service
│   │   │   │   ├── booking/         # Booking module
│   │   │   │   │   ├── config/      # Redis configuration
│   │   │   │   │   ├── controller/  # Booking controllers
│   │   │   │   │   ├── dto/         # Booking DTOs
│   │   │   │   │   ├── model/       # Booking entities
│   │   │   │   │   ├── repository/  # Booking repositories
│   │   │   │   │   └── service/     # Booking services
│   │   │   │   ├── notification/    # Notification module
│   │   │   │   │   └── service/     # Notification services
│   │   │   │   ├── payment/         # Payment module
│   │   │   │   │   ├── config/      # Payment configuration
│   │   │   │   │   ├── controller/  # Payment controllers
│   │   │   │   │   ├── dto/         # Payment DTOs
│   │   │   │   │   ├── model/       # Payment entities
│   │   │   │   │   ├── repository/  # Payment repositories
│   │   │   │   │   └── service/     # Payment services
│   │   │   │   └── whatsapp/        # WhatsApp module
│   │   │   │       ├── config/      # WhatsApp configuration
│   │   │   │       ├── controller/  # WhatsApp controllers
│   │   │   │       ├── dto/         # WhatsApp DTOs
│   │   │   │       └── service/     # WhatsApp services
│   │   │   └── resources/
│   │   │       ├── db/migration/    # Flyway migrations
│   │   │       └── application.yml  # Application configuration
│   │   └── test/                    # Test files
│   ├── .mvn/                        # Maven wrapper
│   ├── mvnw                         # Maven wrapper (Unix)
│   ├── mvnw.cmd                     # Maven wrapper (Windows)
│   └── pom.xml                      # Maven dependencies
├── frontend/                        # React Frontend
│   ├── public/                      # Public assets
│   ├── src/
│   │   ├── components/              # React components
│   │   ├── pages/                   # Page components
│   │   ├── App.tsx                  # Main app component
│   │   └── index.tsx                # Entry point
│   ├── Dockerfile                   # Frontend Dockerfile
│   ├── nginx.conf                   # Nginx configuration
│   ├── package.json                 # NPM dependencies
│   └── tsconfig.json                # TypeScript config
├── docker-compose.yml               # Docker Compose setup
├── .gitignore                       # Git ignore rules
├── README.md                        # Main documentation
├── QUICK_START.md                   # Quick start guide
├── IMPLEMENTATION_SUMMARY.md        # Implementation details
├── PAYMENT_SYSTEM_IMPLEMENTATION.md # Payment system docs
├── PROJECT_STRUCTURE.md             # This file
├── build.sh                         # Build script (Unix)
└── build.bat                        # Build script (Windows)
```

## Key Files

### Backend

- **AppointmentApplication.java**: Main Spring Boot application class
- **application.yml**: Application configuration (database, Redis, payment, WhatsApp)
- **pom.xml**: Maven dependencies and build configuration
- **mvnw/mvnw.cmd**: Maven wrapper scripts (no Maven installation needed)

### Database Migrations

Located in `backend/src/main/resources/db/migration/`:
- **V1__create_base_tables.sql**: Core tables (doctors, patients, bookings, etc.)
- **V2__create_payments_and_invoices_tables.sql**: Payment and invoice tables
- **V3__seed_initial_data.sql**: Seed data for testing
- **V4__create_users_table.sql**: User authentication table

### Frontend

- **App.tsx**: Main React application component
- **pages/**: Page components (Login, Dashboard, Appointments, etc.)
- **components/**: Reusable React components
- **package.json**: NPM dependencies

## Building the Project

### Using Maven Wrapper (Recommended)

**Unix/Mac:**
```bash
cd backend
./mvnw clean package
```

**Windows:**
```bash
cd backend
mvnw.cmd clean package
```

### Using Installed Maven

```bash
cd backend
mvn clean package
```

### Using Build Scripts

**Unix/Mac:**
```bash
chmod +x build.sh
./build.sh
```

**Windows:**
```bash
build.bat
```

## Running the Project

### Using Docker Compose (Recommended)

```bash
docker-compose up -d
```

### Running Backend Only

```bash
cd backend
./mvnw spring-boot:run
```

### Running Frontend Only

```bash
cd frontend
npm install
npm start
```

## Dependencies

### Backend Dependencies (Key)
- Spring Boot 3.2.0
- Spring Data JPA
- Spring Security
- PostgreSQL Driver
- Redis (Lettuce)
- Flyway
- Razorpay SDK
- Twilio SDK
- iText7 (PDF)
- JWT
- Swagger/OpenAPI

### Frontend Dependencies (Key)
- React 18
- TypeScript
- Material UI
- React Router
- Axios

## Configuration

All configuration is in `backend/src/main/resources/application.yml`.

Key configuration sections:
- Database connection
- Redis connection
- Payment (Razorpay)
- WhatsApp (Twilio)
- JWT settings
- Slot hold settings

## Environment Variables

Create a `.env` file or set environment variables:
- Database credentials
- Redis credentials
- Razorpay keys
- Twilio credentials
- JWT secret

## Testing

### Backend Tests
```bash
cd backend
./mvnw test
```

### Integration Tests
Tests are located in `backend/src/test/java/`

## Deployment

See `QUICK_START.md` for deployment instructions.

## Support

For issues and questions, refer to:
- README.md - Main documentation
- QUICK_START.md - Quick start guide
- IMPLEMENTATION_SUMMARY.md - Implementation details

