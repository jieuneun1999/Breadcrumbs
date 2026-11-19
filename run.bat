@echo off
setlocal
pushd "%~dp0" || exit /b 1
set "BREAD_FULL_UI=1"
call scripts\build.bat
if errorlevel 1 goto failed
"build\demo\bread-keys.exe" --launch %*
if errorlevel 1 goto failed
popd
exit /b 0

:failed
set "breadcrumbs_exit=%errorlevel%"
echo.
echo Breadcrumbs could not start or ended with an error. Review the message above.
pause
popd
exit /b %breadcrumbs_exit%
