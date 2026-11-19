@echo off
setlocal
call "%~dp0run.bat" %*
set "BREAD_DEMO_EXIT=%errorlevel%"
if not "%BREAD_DEMO_EXIT%"=="0" (
    echo.
    echo Breadcrumbs could not start or ended with an error. Review the message above.
    pause
)
exit /b %BREAD_DEMO_EXIT%
