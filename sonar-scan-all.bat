@echo off
REM ═══════════════════════════════════════════════════════════════════════════════
REM  ResumeAI — Per-Microservice SonarQube Scan Script
REM  Usage: sonar-scan-all.bat [SONAR_TOKEN]
REM  Example: sonar-scan-all.bat sqp_4682e3f9c7393c73b7ed046f6c60bbe626b550fb
REM ═══════════════════════════════════════════════════════════════════════════════

set SONAR_HOST=http://localhost:9000
set SONAR_TOKEN=%1

if "%SONAR_TOKEN%"=="" (
    echo ERROR: SonarQube token required.
    echo Usage: sonar-scan-all.bat YOUR_SONAR_TOKEN
    exit /b 1
)

set SERVICES=ai-service api-gateway auth-service eureka-server export-service jobmatch-service notification-service resume-service section-service template-service

echo.
echo ══════════════════════════════════════════════════════
echo   ResumeAI Enterprise SonarQube Per-Service Scanner
echo ══════════════════════════════════════════════════════
echo.

REM ── PHASE 1: Full build to generate all binaries ──
echo [PHASE 1] Building all services...
call mvnw.cmd clean verify -DskipTests=false
if %ERRORLEVEL% NEQ 0 (
    echo [FATAL] Build failed. Fix compilation errors before scanning.
    exit /b 1
)
echo [PHASE 1] Build complete.
echo.

REM ── PHASE 2: Per-service Sonar scans ──
echo [PHASE 2] Running per-service SonarQube scans...
echo.

set PASS_COUNT=0
set FAIL_COUNT=0

for %%S in (%SERVICES%) do (
    echo ────────────────────────────────────────────────
    echo   Scanning: %%S
    echo ────────────────────────────────────────────────
    pushd %%S
    call ..\mvnw.cmd sonar:sonar -Dsonar.host.url=%SONAR_HOST% -Dsonar.login=%SONAR_TOKEN%
    if %ERRORLEVEL% NEQ 0 (
        echo [FAIL] %%S scan failed.
        set /a FAIL_COUNT+=1
    ) else (
        echo [PASS] %%S scan complete.
        set /a PASS_COUNT+=1
    )
    popd
    echo.
)

REM ── PHASE 3: Frontend scan ──
echo ────────────────────────────────────────────────
echo   Scanning: FRONTEND (React)
echo ────────────────────────────────────────────────
pushd "..\Frontend React ResumeAI"
call npx sonarqube-scanner --define sonar.host.url=%SONAR_HOST% --define sonar.token=%SONAR_TOKEN%
if %ERRORLEVEL% NEQ 0 (
    echo [FAIL] Frontend scan failed.
    set /a FAIL_COUNT+=1
) else (
    echo [PASS] Frontend scan complete.
    set /a PASS_COUNT+=1
)
popd
echo.

REM ── PHASE 4: Aggregator scan ──
echo ────────────────────────────────────────────────
echo   Scanning: ROOT AGGREGATOR
echo ────────────────────────────────────────────────
call mvnw.cmd sonar:sonar -Dsonar.host.url=%SONAR_HOST% -Dsonar.login=%SONAR_TOKEN%
if %ERRORLEVEL% NEQ 0 (
    echo [FAIL] Aggregator scan failed.
    set /a FAIL_COUNT+=1
) else (
    echo [PASS] Aggregator scan complete.
    set /a PASS_COUNT+=1
)

echo.
echo ══════════════════════════════════════════════════════
echo   SCAN SUMMARY
echo ══════════════════════════════════════════════════════
echo   Passed: %PASS_COUNT%
echo   Failed: %FAIL_COUNT%
echo   Dashboard: %SONAR_HOST%
echo ══════════════════════════════════════════════════════
echo.
