@echo off
setlocal
pushd "%~dp0.." || exit /b 1
where java >nul 2>nul
if errorlevel 1 goto missing_java
where javac >nul 2>nul
if errorlevel 1 goto missing_java
java --version >nul 2>nul
if errorlevel 1 goto incompatible_java
set "BREAD_JAVA_VERSION="
for /f "tokens=2" %%V in ('java --version 2^>nul') do if not defined BREAD_JAVA_VERSION set "BREAD_JAVA_VERSION=%%V"
for /f "tokens=1 delims=.-+" %%V in ("%BREAD_JAVA_VERSION%") do set "BREAD_JAVA_MAJOR=%%V"
if not defined BREAD_JAVA_MAJOR goto incompatible_java
if %BREAD_JAVA_MAJOR% LSS 17 goto incompatible_java
if not exist "build\app" mkdir "build\app"
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build\app -sourcepath src src\breadcrumbs\Main.java
if errorlevel 1 (
    echo Compilation failed. Breadcrumbs was not started; old classes will not be run. 1>&2
    echo A Java 17+ JDK is required. See compiler errors above. 1>&2
    popd
    exit /b 1
)
rem A previous helper must never survive a skipped or failed optional rebuild.
if exist "build\app\bread-keys.exe" del /q "build\app\bread-keys.exe"
if "%BREAD_SKIP_NATIVE%"=="1" goto portable
set "BREAD_CSC=%SystemRoot%\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if not exist "%BREAD_CSC%" set "BREAD_CSC=%SystemRoot%\Microsoft.NET\Framework\v4.0.30319\csc.exe"
if not exist "%BREAD_CSC%" goto portable
"%BREAD_CSC%" /nologo /optimize+ /warnaserror+ /target:exe /out:build\app\bread-keys.exe tools\bread-keys.cs
if errorlevel 1 (
    if exist "build\app\bread-keys.exe" del /q "build\app\bread-keys.exe"
    echo Optional keyboard helper could not be built. Using portable numbered input. 1>&2
)
popd
exit /b 0
:portable
echo Optional keyboard helper unavailable or skipped. Portable numbered input remains available.
popd
exit /b 0
:missing_java
echo Java tools were not found. Install a JDK 17 or newer and add its bin folder to PATH. 1>&2
echo Both java and javac are required; a JRE alone is not enough. 1>&2
popd
exit /b 1
:incompatible_java
echo Java 17 or newer is required. Check java --version and javac --version on PATH. 1>&2
popd
exit /b 1
