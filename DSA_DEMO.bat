@echo off
setlocal
call "%~dp0DEMO.bat" --classroom "--group-name=The Bread" %*
exit /b %errorlevel%
