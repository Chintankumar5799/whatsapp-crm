@echo off
title PMS PMS - Frontend Web App
echo ===================================================
echo Starting PMS React App on http://localhost:3000
echo ===================================================
cd /d "%~dp0frontend"
call npm start
pause

