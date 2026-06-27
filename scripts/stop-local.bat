@echo off
setlocal
cd /d "%~dp0.."

if not exist "backend-api\.env" (
  copy "backend-api\.env.example" "backend-api\.env" >nul
)

docker compose stop
echo SmartGarden local services stopped. Database data is preserved.
