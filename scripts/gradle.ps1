$ErrorActionPreference = "Stop"

if (-not $env:GRADLE_USER_HOME -and (Test-Path -LiteralPath "$env:USERPROFILE\.gradle" -PathType Container)) {
    $env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"
}

$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    throw "Set JAVA_HOME to a complete JDK 17. Only JDK 17 is supported."
}
foreach ($tool in @("java", "javac", "jlink")) {
    if (-not (Test-Path -LiteralPath "$javaHome\bin\$tool.exe" -PathType Leaf)) {
        throw "Set JAVA_HOME to a complete JDK 17 containing java, javac, and jlink."
    }
}
$release = Get-Content -LiteralPath "$javaHome\release" -Raw
if ($release -notmatch '(?m)^JAVA_VERSION="17\.') {
    throw "Only JDK 17 is supported. Correct JAVA_HOME before running Gradle."
}

$env:JAVA_HOME = $javaHome
& "$PSScriptRoot\..\gradlew.bat" @args
exit $LASTEXITCODE
