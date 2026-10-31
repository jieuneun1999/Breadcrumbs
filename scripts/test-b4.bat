@echo off
setlocal
pushd "%~dp0.." || exit /b 1
call scripts\build.bat
if errorlevel 1 goto failed
if not exist "build\test" mkdir "build\test"
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build\test -sourcepath "src;tests" tests\breadcrumbs\b4\B4Tests.java tests\breadcrumbs\b4\B4Scenarios.java
if errorlevel 1 goto failed
java -cp build\test breadcrumbs.b4.B4Tests
if errorlevel 1 goto failed
java -cp build\test breadcrumbs.b4.B4Scenarios
if errorlevel 1 goto failed
popd
exit /b 0
:failed
echo B4 verification failed. See the error and retained build fixtures above. 1>&2
popd
exit /b 1
