@echo off
setlocal
pushd "%~dp0" || exit /b 1
call scripts\build.bat
if errorlevel 1 (
    popd
    exit /b 1
)
java -cp build\app breadcrumbs.Main %*
set "breadcrumbs_exit=%errorlevel%"
popd
exit /b %breadcrumbs_exit%
