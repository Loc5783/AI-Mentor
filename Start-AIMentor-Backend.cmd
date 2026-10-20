@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0backend\start-local.ps1" -Demo
pause
