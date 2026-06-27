@echo off
setlocal
cd /d "%~dp0.."

if not exist "backend-api\.env" (
  copy "backend-api\.env.example" "backend-api\.env" >nul
  echo Created backend-api\.env from example. Edit it if needed.
)

docker compose up -d
if errorlevel 1 (
  echo Failed to start Docker MySQL. Make sure Docker Desktop is running.
  exit /b 1
)

echo Applying non-destructive schema...
docker exec smartgarden-mysql sh -c "mysql -uroot -p$MYSQL_ROOT_PASSWORD -e \"CREATE DATABASE IF NOT EXISTS $MYSQL_DATABASE; GRANT ALL PRIVILEGES ON $MYSQL_DATABASE.* TO '$MYSQL_USER'@'%%'; FLUSH PRIVILEGES;\""
if errorlevel 1 (
  echo Failed to prepare database and grants.
  exit /b 1
)
docker exec -i smartgarden-mysql sh -c "mysql -u$MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DATABASE" < "backend-api\schema.sql"
if errorlevel 1 (
  echo Failed to apply schema. Check backend-api\.env and Docker MySQL status.
  exit /b 1
)

cd backend-api
if not exist "node_modules" (
  npm install
  if errorlevel 1 exit /b 1
)

echo SmartGarden backend starting at http://0.0.0.0:3000
npm start
