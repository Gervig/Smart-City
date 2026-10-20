@echo off
rem Starts the Smart City API. Usage: run   (or: run -NoDb to skip the database container)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run.ps1" %*
exit /b %ERRORLEVEL%