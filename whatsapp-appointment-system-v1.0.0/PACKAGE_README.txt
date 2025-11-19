================================================================================
  WhatsApp Appointment & Billing System - Spring Boot Project
================================================================================

This is a complete, production-ready Spring Boot application for managing
appointments via WhatsApp with integrated payment processing and billing.

PROJECT CONTENTS:
-----------------
- backend/          : Spring Boot application (Java 17+)
- frontend/         : React dashboard (TypeScript)
- docker-compose.yml: Full stack deployment configuration
- Documentation     : README.md, QUICK_START.md, etc.

QUICK START:
------------
1. Ensure Java 17+ is installed
2. Configure environment variables (see .env.example or DOWNLOAD_INSTRUCTIONS.md)
3. Build: ./build.sh (Unix) or build.bat (Windows)
4. Run: docker-compose up -d

OR run backend only:
  cd backend
  ./mvnw spring-boot:run    (Unix/Mac)
  mvnw.cmd spring-boot:run  (Windows)

MAVEN WRAPPER:
--------------
This project includes Maven Wrapper - no Maven installation needed!
- Unix/Mac: Use ./mvnw
- Windows: Use mvnw.cmd

DOCUMENTATION:
--------------
- README.md                 : Main documentation
- QUICK_START.md            : Quick setup guide
- DOWNLOAD_INSTRUCTIONS.md  : Detailed download and setup
- PROJECT_STRUCTURE.md      : Project structure overview
- IMPLEMENTATION_SUMMARY.md : Architecture and implementation details

REQUIREMENTS:
-------------
- Java 17 or higher
- Docker & Docker Compose (optional, for containerized deployment)
- Node.js 18+ (optional, for frontend development)

DEFAULT CREDENTIALS:
--------------------
Admin Login:
  Username: admin
  Password: admin123

ACCESS POINTS:
--------------
- Frontend:     http://localhost:3000
- Backend API:  http://localhost:8080/api
- Swagger UI:   http://localhost:8080/api/swagger-ui.html

CONFIGURATION:
--------------
All configuration is in:
- backend/src/main/resources/application.yml
- Environment variables (see .env file)

For detailed setup instructions, see DOWNLOAD_INSTRUCTIONS.md

================================================================================
For support and questions, refer to the documentation files.
================================================================================

