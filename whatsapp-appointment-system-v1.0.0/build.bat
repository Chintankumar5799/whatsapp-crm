@echo off
REM Build script for WhatsApp Appointment System (Windows)
REM This script builds the entire project

echo =========================================
echo Building WhatsApp Appointment System
echo =========================================

REM Build Backend
echo.
echo Building Backend...
cd backend
if exist "mvnw.cmd" (
    call mvnw.cmd clean package -DskipTests
) else (
    call mvn clean package -DskipTests
)

if errorlevel 1 (
    echo Backend build failed!
    exit /b 1
)

echo Backend build successful!
cd ..

REM Build Frontend
echo.
echo Building Frontend...
cd frontend
if exist "package.json" (
    call npm install
    call npm run build
    if errorlevel 1 (
        echo Frontend build failed!
        exit /b 1
    )
    echo Frontend build successful!
) else (
    echo Frontend package.json not found, skipping...
)
cd ..

echo.
echo =========================================
echo Build completed successfully!
echo =========================================
echo.
echo To run the application:
echo   docker-compose up -d
echo.
echo Or run backend manually:
echo   cd backend ^&^& mvnw.cmd spring-boot:run
echo.

pause

