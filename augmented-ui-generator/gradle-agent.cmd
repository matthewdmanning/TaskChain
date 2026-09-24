@echo off
setlocal
if not defined GRADLE_USER_HOME if exist "%USERPROFILE%\.gradle" set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jlink.exe" goto run
set "JAVA_HOME=%USERPROFILE%\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
if not exist "%JAVA_HOME%\bin\jlink.exe" (
    >&2 echo A complete JDK 21 was not found. Set JAVA_HOME to a JDK containing java, javac, and jlink.
    exit /b 1
)
set "TEMP=%~dp0.build-tmp"
set "TMP=%TEMP%"
if not exist "%TEMP%" mkdir "%TEMP%"
:run
call "%~dp0gradlew.bat" %*
