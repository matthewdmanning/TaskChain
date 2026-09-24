@echo off
setlocal

if not defined GRADLE_USER_HOME if exist "%USERPROFILE%\.gradle" set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jlink.exe" goto run
set "JAVA_HOME=%USERPROFILE%\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
if exist "%JAVA_HOME%\bin\jlink.exe" goto run
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if not exist "%JAVA_HOME%\bin\jlink.exe" (
    >&2 echo A complete JDK 21 was not found. Set JAVA_HOME to a JDK containing java, javac, and jlink.
    exit /b 1
)

:run
call "%~dp0..\gradlew.bat" %*
exit /b %ERRORLEVEL%
