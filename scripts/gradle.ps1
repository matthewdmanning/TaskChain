$ErrorActionPreference = "Stop"

if (-not $env:GRADLE_USER_HOME -and (Test-Path -LiteralPath "$env:USERPROFILE\.gradle" -PathType Container)) {
    $env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"
}

$javaHome = $env:JAVA_HOME
if (-not (Test-Path -LiteralPath "$javaHome\bin\jlink.exe" -PathType Leaf)) {
    $javaHome = "$env:USERPROFILE\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
}
if (-not (Test-Path -LiteralPath "$javaHome\bin\jlink.exe" -PathType Leaf)) {
    $javaHome = "C:\Program Files\Android\Android Studio\jbr"
}
if (-not (Test-Path -LiteralPath "$javaHome\bin\jlink.exe" -PathType Leaf)) {
    throw "A complete JDK 21 was not found. Set JAVA_HOME to a JDK containing java, javac, and jlink."
}

$env:JAVA_HOME = $javaHome
& "$PSScriptRoot\..\gradlew.bat" @args
exit $LASTEXITCODE
