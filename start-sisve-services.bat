@echo off
title SISVE - Inicio de Microservicios

echo ============================================================
echo            INICIANDO MICROSERVICIOS SISVE
echo ============================================================
echo.

REM ============================================================
REM Nos ubicamos automaticamente en la carpeta del .bat
REM ============================================================
cd /d "%~dp0"

echo [1/5] Iniciando AUTH SERVICE - Puerto 8081...
start "SISVE - AUTH SERVICE :8081" cmd /k call gradlew.bat :auth-service:quarkusDev "-Ddebug=false"

timeout /t 2 /nobreak >nul

echo [2/5] Iniciando ELECTION SERVICE - Puerto 8082...
start "SISVE - ELECTION SERVICE :8082" cmd /k call gradlew.bat :election-service:quarkusDev "-Ddebug=false"

timeout /t 2 /nobreak >nul

echo [3/5] Iniciando VOTE SERVICE - Puerto 8083...
start "SISVE - VOTE SERVICE :8083" cmd /k call gradlew.bat :vote-service:quarkusDev "-Ddebug=false"

timeout /t 2 /nobreak >nul

echo [4/5] Iniciando AUDIT SERVICE - Puerto 8084...
start "SISVE - AUDIT SERVICE :8084" cmd /k call gradlew.bat :audit-service:quarkusDev "-Ddebug=false"

timeout /t 2 /nobreak >nul

echo [5/5] Iniciando POLLING STATION SERVICE - Puerto 8085...
start "SISVE - POLLING STATION SERVICE :8085" cmd /k call gradlew.bat :polling-station-service:quarkusDev "-Ddebug=false"

echo.
echo ============================================================
echo Los 5 microservicios fueron lanzados.
echo ============================================================
echo.
echo AUTH SERVICE             http://localhost:8081
echo ELECTION SERVICE         http://localhost:8082
echo VOTE SERVICE             http://localhost:8083
echo AUDIT SERVICE            http://localhost:8084
echo POLLING STATION SERVICE  http://localhost:8085
echo.
echo Health checks:
echo http://localhost:8081/health/ready
echo http://localhost:8082/health/ready
echo http://localhost:8083/health/ready
echo http://localhost:8084/health/ready
echo http://localhost:8085/health/ready
echo.
pause