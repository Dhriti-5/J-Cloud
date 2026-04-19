@echo off
REM Legacy helper. Preferred approach is .env file in project root.
REM Use .env.example as template and keep secrets in .env (gitignored).
REM If you still want to use this script, copy to set-neon-env.bat and fill placeholders.

set "JCLOUD_DB_URL=jdbc:postgresql://your-neon-host.aws.neon.tech/neondb?sslmode=require&channel_binding=require"
set "JCLOUD_DB_HOST=your-neon-host.aws.neon.tech"
set "JCLOUD_DB_PORT=5432"
set "JCLOUD_DB_NAME=neondb"
set "JCLOUD_DB_USER=your_neon_user"
set "JCLOUD_DB_PASSWORD=your_neon_password"

echo Neon PostgreSQL environment variables set for this terminal session.
echo You can now run compile.bat, test-database.bat, run-master.bat, and the data node scripts.
