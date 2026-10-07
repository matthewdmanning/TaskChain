@echo off
setlocal

if not defined GRADLE_USER_HOME if exist "%USERPROFILE%\.gradle" set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
if not defined JAVA_HOME goto invalidJdk
for %%T in (java javac jlink) do if not exist "%JAVA_HOME%\bin\%%T.exe" goto invalidJdk
if not exist "%JAVA_HOME%\release" goto invalidJdk
set "JDK_VERSION="
for /f "usebackq tokens=1,2 delims==" %%A in ("%JAVA_HOME%\release") do if "%%A"=="JAVA_VERSION" set "JDK_VERSION=%%~B"
if not "%JDK_VERSION:~0,3%"=="17." goto invalidJdk

:run
call "%~dp0..\gradlew.bat" %*
exit /b %ERRORLEVEL%

:invalidJdk
>&2 echo Set JAVA_HOME to a complete JDK 17 containing java, javac, and jlink. Only JDK 17 is supported.
exit /b 1
