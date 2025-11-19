#!/bin/bash

# Build script for WhatsApp Appointment System
# This script builds the entire project

echo "========================================="
echo "Building WhatsApp Appointment System"
echo "========================================="

# Build Backend
echo ""
echo "Building Backend..."
cd backend
if [ -f "./mvnw" ]; then
    chmod +x ./mvnw
    ./mvnw clean package -DskipTests
else
    mvn clean package -DskipTests
fi

if [ $? -ne 0 ]; then
    echo "Backend build failed!"
    exit 1
fi

echo "Backend build successful!"
cd ..

# Build Frontend
echo ""
echo "Building Frontend..."
cd frontend
if [ -f "package.json" ]; then
    npm install
    npm run build
    if [ $? -ne 0 ]; then
        echo "Frontend build failed!"
        exit 1
    fi
    echo "Frontend build successful!"
else
    echo "Frontend package.json not found, skipping..."
fi
cd ..

echo ""
echo "========================================="
echo "Build completed successfully!"
echo "========================================="
echo ""
echo "To run the application:"
echo "  docker-compose up -d"
echo ""
echo "Or run backend manually:"
echo "  cd backend && ./mvnw spring-boot:run"
echo ""

