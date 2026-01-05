# Stripe Integration Testing Guide (Postman)

This guide provides instructions for setting up and testing the Stripe payment integration.

## 1. Stripe Setup

To test the integration, you need to configure your Stripe API keys in the application.

### Prerequisites
1.  A Stripe account (Test mode).
2.  Stripe Secret Key.
3.  Stripe Webhook Secret (obtained by using Stripe CLI or setting up a webhook on Stripe Dashboard).

### Configuration
Update `backend/src/main/resources/application.yml` or set the following environment variables:

```yaml
payment:
  stripe:
    api-key: ${STRIPE_API_KEY:your_stripe_test_secret_key}
    webhook-secret: ${STRIPE_WEBHOOK_SECRET:your_stripe_webhook_secret}
  default-provider: STRIPE
```

---

## 2. API Testing with Postman

### A. Create Payment Link (Checkout Session)
This endpoint creates a Stripe Checkout Session and returns a URL for the user to complete the payment.

*   **URL:** `POST http://localhost:8080/api/payments/links`
*   **Headers:**
    *   `Content-Type: application/json`
    *   `Authorization: Bearer <your_jwt_token>` (If security is enabled)
*   **Body (JSON):**

```json
{
  "bookingId": 1,
  "patientId": 1,
  "amount": 500.00,
  "currency": "INR",
  "description": "Appointment Fee - Dr. Smith",
  "customerName": "John Doe",
  "customerEmail": "john.doe@example.com",
  "customerPhone": "9876543210",
  "callbackUrl": "http://localhost:3000/payment/callback",
  "redirectUrl": "http://localhost:3000/appointments"
}
```

*   **Expected Response:**
```json
{
  "paymentId": "pi_...",
  "paymentLink": "https://checkout.stripe.com/pay/...",
  "provider": "STRIPE"
}
```

---

### B. Simulate Stripe Webhook
To test the payment completion logic locally, you can simulate a Stripe webhook.

*   **URL:** `POST http://localhost:8080/api/webhooks/payments/stripe`
*   **Headers:**
    *   `Content-Type: application/json`
    *   `Stripe-Signature: <calculated_signature>` (Optional if signature verification is disabled or mocked)
*   **Body (JSON - Example `checkout.session.completed`):**

```json
{
  "id": "evt_123456789",
  "type": "checkout.session.completed",
  "data": {
    "object": {
      "id": "cs_test_...",
      "payment_intent": "pi_...",
      "amount_total": 50000,
      "currency": "inr",
      "payment_status": "paid",
      "metadata": {
        "bookingId": "1"
      }
    }
  }
}
```

---

## 3. Verification
1.  After calling the "Create Payment Link" API, open the `paymentLink` in a browser.
2.  Use Stripe test cards (e.g., `4242...`) to complete the payment.
3.  Verify that the `Payment` record status is updated to `PAID` in the database.
4.  Verify that an invoice is generated and linked to the booking.
