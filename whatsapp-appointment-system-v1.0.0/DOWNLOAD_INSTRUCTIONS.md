# Download and Setup Instructions

## Prerequisites

Before downloading and running this project, ensure you have:

1. **Java 17 or higher** - [Download Java](https://www.oracle.com/java/technologies/downloads/)
2. **Docker and Docker Compose** (optional, for containerized deployment)
   - [Download Docker Desktop](https://www.docker.com/products/docker-desktop)
3. **Node.js 18+** (for frontend development, optional)
   - [Download Node.js](https://nodejs.org/)

## Download Options

### Option 1: Download as ZIP

1. Download the entire project as a ZIP file
2. Extract to your desired location
3. Follow the setup instructions below

### Option 2: Clone from Git (if available)

```bash
git clone <repository-url>
cd whatsapp-appointment-system
```

## Project Structure

After downloading, you should see:

```
whatsapp-appointment-system/
├── backend/          # Spring Boot application
├── frontend/         # React application
├── docker-compose.yml
├── README.md
└── ... (other files)
```

## Quick Setup

### Step 1: Configure Environment Variables

Create a `.env` file in the root directory:

```env
# Razorpay Configuration
RAZORPAY_KEY_ID=your_razorpay_key_id
RAZORPAY_KEY_SECRET=your_razorpay_key_secret
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret

# Twilio Configuration
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=whatsapp:+1234567890

# JWT Secret (change in production)
JWT_SECRET=your-256-bit-secret-key-change-in-production-minimum-32-characters
```

### Step 2: Build the Project

#### Using Build Scripts (Recommended)

**On Windows:**
```bash
build.bat
```

**On Unix/Mac:**
```bash
chmod +x build.sh
./build.sh
```

#### Manual Build

**Backend:**
```bash
cd backend

# Using Maven Wrapper (no Maven installation needed)
./mvnw clean package    # Unix/Mac
mvnw.cmd clean package  # Windows

# Or using installed Maven
mvn clean package
```

**Frontend:**
```bash
cd frontend
npm install
npm run build
```

### Step 3: Run the Application

#### Option A: Using Docker Compose (Recommended)

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database
- Redis cache
- Backend API (port 8080)
- Frontend (port 3000)

#### Option B: Run Backend Only

```bash
cd backend
./mvnw spring-boot:run    # Unix/Mac
mvnw.cmd spring-boot:run  # Windows
```

#### Option C: Run Frontend Only

```bash
cd frontend
npm install
npm start
```

### Step 4: Access the Application

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs**: http://localhost:8080/api/api-docs

### Step 5: Login

Default admin credentials:
- **Username**: `admin`
- **Password**: `admin123`

## Maven Wrapper

This project includes Maven Wrapper, so you don't need to install Maven separately.

- **Unix/Mac**: Use `./mvnw` or `./mvnw.cmd`
- **Windows**: Use `mvnw.cmd`

The wrapper will automatically download Maven if needed.

## Troubleshooting

### Java Version Issues

Verify Java version:
```bash
java -version
```

Should show Java 17 or higher.

### Port Already in Use

If port 8080 or 3000 is already in use:

1. **Backend**: Change port in `backend/src/main/resources/application.yml`:
   ```yaml
   server:
     port: 8081  # Change to available port
   ```

2. **Frontend**: Change port in `frontend/package.json` or Docker Compose

### Database Connection Issues

1. Ensure PostgreSQL is running (if using Docker Compose, it starts automatically)
2. Check database credentials in `application.yml`
3. Verify database is accessible

### Build Failures

1. **Maven Issues**: Use the Maven wrapper (`./mvnw` or `mvnw.cmd`)
2. **Dependency Issues**: Delete `backend/target` and rebuild
3. **Frontend Issues**: Delete `frontend/node_modules` and run `npm install` again

### Docker Issues

1. Ensure Docker Desktop is running
2. Check Docker Compose version: `docker-compose --version`
3. View logs: `docker-compose logs -f`

## Next Steps

1. Read `README.md` for detailed documentation
2. Check `QUICK_START.md` for quick start guide
3. Review `IMPLEMENTATION_SUMMARY.md` for architecture details
4. Explore the API at `/swagger-ui.html`

## Support

For issues:
1. Check the documentation files
2. Review error logs
3. Verify environment variables
4. Check database and Redis connections

## Production Deployment

Before deploying to production:

1. Change default passwords
2. Update JWT secret
3. Use production database
4. Configure proper CORS
5. Enable HTTPS/TLS
6. Set up monitoring
7. Configure backups

See `README.md` for production deployment guidelines.

