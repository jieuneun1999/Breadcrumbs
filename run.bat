@echo off
setlocal
pushd "%~dp0" || exit /b 1
set "BREAD_BUILD=build\app"
if "%BREAD_DEMO_WIDE%"=="1" set "BREAD_BUILD=build\demo"
call scripts\build.bat
if errorlevel 1 (
    popd
    exit /b 1
)
if exist "%BREAD_BUILD%\bread-keys.exe" (
    "%BREAD_BUILD%\bread-keys.exe" --launch %*
) else (
    java -cp "%BREAD_BUILD%" breadcrumbs.Main %*
)
set "breadcrumbs_exit=%errorlevel%"
popd
exit /b %breadcrumbs_exit%
