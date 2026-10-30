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
popd
exit /b 0
