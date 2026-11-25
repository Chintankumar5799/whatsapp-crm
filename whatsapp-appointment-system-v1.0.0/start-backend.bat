@echo off
title PMS PMS - Backend Server
echo ===================================================
echo Starting PMS Backend on http://localhost:8080/api
echo ===================================================
cd /d "%~dp0backend"
set DB_PASSWORD=4321
echo Starting Spring Boot (Hibernate ddl-auto: create)...
call .\mvnw.cmd spring-boot:run
pause

