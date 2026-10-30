@echo off
setlocal
pushd "%~dp0.." || exit /b 1
call scripts\build.bat
if errorlevel 1 (
    popd
    exit /b 1
)
if not exist "build\test" mkdir "build\test"
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build\test -sourcepath "src;tests" tests\breadcrumbs\AllTests.java
if errorlevel 1 (
    echo Test compilation failed. See compiler errors above. 1>&2
    popd
    exit /b 1
)
java -cp build\test breadcrumbs.AllTests
set "breadcrumbs_exit=%errorlevel%"
popd
exit /b %breadcrumbs_exit%
