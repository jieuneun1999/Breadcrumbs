@echo off
setlocal
call "%~dp0run-test-console.bat" --classroom "--group-name=The Bread" %*
exit /b %errorlevel%
