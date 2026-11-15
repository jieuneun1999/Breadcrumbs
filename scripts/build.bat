@echo off
setlocal
pushd "%~dp0.." || exit /b 1
where javac >nul 2>nul
if errorlevel 1 (
    echo Error: javac was not found. Add a Java 17+ JDK bin directory to PATH. 1>&2
    popd
    exit /b 1
)
if not exist "build\app" mkdir "build\app"
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build\app -sourcepath src src\breadcrumbs\Main.java
if errorlevel 1 (
    echo Compilation failed. A Java 17+ JDK is required. See compiler errors above. 1>&2
    popd
    exit /b 1
)
set "BREAD_CSC=%SystemRoot%\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if not exist "%BREAD_CSC%" set "BREAD_CSC=%SystemRoot%\Microsoft.NET\Framework\v4.0.30319\csc.exe"
if exist "%BREAD_CSC%" (
    "%BREAD_CSC%" /nologo /optimize+ /warnaserror+ /target:exe /out:build\app\bread-keys.exe tools\bread-keys.cs
    if errorlevel 1 (
        echo Optional Windows keyboard helper compilation failed. 1>&2
        popd
        exit /b 1
    )
) else (
    echo Optional Windows keyboard compiler unavailable. Portable static input remains available.
)
popd
exit /b 0
