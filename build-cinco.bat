@echo off
setlocal
cd /d "%~dp0"
title Cinco Client Build
color 0D
echo ========================================
echo          CINCO CLIENT BUILD
echo ========================================
echo.
where javac >nul 2>&1
if errorlevel 1 (
  echo JDK 21 was not found on PATH.
  echo Install Java JDK 21, then run this file again:
  echo https://adoptium.net/temurin/releases/?version=21
  echo.
  pause
  exit /b 1
)
for /f "tokens=2" %%V in ('javac -version 2^>^&1') do set JAVAC_VERSION=%%V
echo Detected Java compiler: %JAVAC_VERSION%
if not "%JAVAC_VERSION:~0,2%"=="21" (
  echo This project requires JDK 21, but another Java version is active.
  echo Install JDK 21 from https://adoptium.net/temurin/releases/?version=21
  echo Then reopen Command Prompt or restart Windows and run this file again.
  pause
  exit /b 1
)
echo Java 21 detected.
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-cinco.ps1"
set BUILD_RESULT=%ERRORLEVEL%
echo.
if "%BUILD_RESULT%"=="0" (
  echo BUILD SUCCESSFUL!
  echo Your mod is in: buildlibscinco-client-2.0.0.jar
) else (
  echo BUILD FAILED. Keep this window open and share the error text.
)
echo.
pause
exit /b %BUILD_RESULT%
