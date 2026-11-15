@echo off
setlocal
pushd "%~dp0.." || exit /b 1
call scripts\build.bat
if errorlevel 1 goto failed
if not exist "build\test" mkdir "build\test"
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build\test -sourcepath "src;tests" tests\breadcrumbs\AllTests.java tests\breadcrumbs\storage\B7RecoveryProcessTest.java tests\breadcrumbs\b7\ProcessScenarios.java tests\breadcrumbs\b7\ProjectMapProcessTest.java tests\breadcrumbs\b6\ProcessScenarios.java tests\breadcrumbs\b5\ProcessScenarios.java tests\breadcrumbs\b4\B4Scenarios.java
if errorlevel 1 goto failed
java -cp build\test breadcrumbs.AllTests
if errorlevel 1 goto failed
java -cp build\test breadcrumbs.storage.B7RecoveryProcessTest
if errorlevel 1 goto failed
java -cp build\test breadcrumbs.b7.ProcessScenarios
if errorlevel 1 goto failed
java -cp build\test breadcrumbs.b7.ProjectMapProcessTest
if errorlevel 1 goto failed
java -cp build\test breadcrumbs.b6.ProcessScenarios
if errorlevel 1 goto failed
java -cp build\test breadcrumbs.b5.ProcessScenarios
if errorlevel 1 goto failed
java -cp build\test breadcrumbs.b4.B4Scenarios
if errorlevel 1 goto failed
popd
exit /b 0
:failed
echo B7 verification failed. Inspect retained build fixtures. 1>&2
popd
exit /b 1
