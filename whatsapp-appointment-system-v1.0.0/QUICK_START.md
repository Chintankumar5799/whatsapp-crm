# Quick Start Guide

## Prerequisites

- Docker and Docker Compose installed
- Razorpay account (for payments)
- Twilio account (for WhatsApp)

## Setup Steps

### 1. Configure Environment Variables

Create a `.env` file in the root directory:

```env
# Razorpay
RAZORPAY_KEY_ID=your_razorpay_key_id
RAZORPAY_KEY_SECRET=your_razorpay_key_secret
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret

# Twilio
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=whatsapp:+1234567890

# JWT (change in production)
JWT_SECRET=your-256-bit-secret-key-change-in-production-minimum-32-characters
```

### 2. Start Services

```bash
docker-compose up -d
```

This will start:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Backend API (port 8080)
- Frontend (port 3000)

### 3. Verify Services

Check if all services are running:
```bash
docker-compose ps
```

View logs:
```bash
docker-compose logs -f backend
```

### 4. Access the Application

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs**: http://localhost:8080/api/api-docs

### 5. Login

Default admin credentials:
- Username: `admin`
- Password: `admin123`

### 6. Configure Webhooks

#### Razorpay Webhook
1. Go to Razorpay Dashboard → Settings → Webhooks
2. Add webhook URL: `http://your-domain.com/api/webhooks/payments/razorpay`
3. Select events: `payment_link.paid`
4. Copy webhook secret to `.env` file

#### Twilio Webhook
1. Go to Twilio Console → Phone Numbers → WhatsApp
2. Set webhook URL: `http://your-domain.com/api/webhooks/whatsapp`
3. Save configuration

## Testing the System

### 1. Create a Booking

```bash
# Get available slots
curl http://localhost:8080/api/bookings/slots/available?doctorId=1&date=2024-01-15

# Create a hold
curl -X POST http://localhost:8080/api/bookings/slots/1/hold?patientId=1

# Create booking (use hold token from previous response)
curl -X POST http://localhost:8080/api/bookings/create \
  -H "Content-Type: application/json" \
  -d '{
    "doctorId": 1,
    "patientId": 1,
    "slotId": 1,
    "holdToken": "your-hold-token"
  }'
```

### 2. Create Payment Link

```bash
curl -X POST http://localhost:8080/api/payments/links \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": 1,
    "patientId": 1,
    "amount": 500.00,
    "currency": "INR",
    "description": "Consultation fee",
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "customerPhone": "+1234567890"
  }'
```

### 3. Test Webhooks

Use a tool like ngrok to expose your local server:
```bash
ngrok http 8080
```

Update webhook URLs in Razorpay/Twilio to use the ngrok URL.

## Troubleshooting

### Database Connection Issues
- Check if PostgreSQL is running: `docker-compose ps postgres`
- Check logs: `docker-compose logs postgres`
- Verify connection string in `application.yml`

### Redis Connection Issues
- Check if Redis is running: `docker-compose ps redis`
- Check logs: `docker-compose logs redis`
- Verify Redis configuration in `application.yml`

### Payment Issues
- Verify Razorpay credentials in `.env`
- Check webhook configuration in Razorpay dashboard
- Verify webhook signature verification

### WhatsApp Issues
- Verify Twilio credentials in `.env`
- Check Twilio phone number format (must start with `whatsapp:+`)
- Verify webhook URL in Twilio console

## Stopping Services

```bash
docker-compose down
```

To remove volumes (clears database):
```bash
docker-compose down -v
```

## Development Mode

### Backend Only
```bash
cd backend
mvn spring-boot:run
```

### Frontend Only
```bash
cd frontend
npm install
npm start
```

## Next Steps

1. Review the API documentation at `/swagger-ui.html`
2. Explore the codebase structure
3. Customize for your use case
4. Add additional features as needed

