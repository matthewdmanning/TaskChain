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
$updateDebug = $args -contains ":app:installDebug" -or $args -contains "installDebug"
$gradleArgs = @($args | ForEach-Object {
    if ($_ -eq ":app:installDebug" -or $_ -eq "installDebug") { ":app:assembleDebug" } else { $_ }
})
& "$PSScriptRoot\..\gradlew.bat" @gradleArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($updateDebug -and $args -notcontains "--dry-run" -and $args -notcontains "-m") {
    $sdkRoot = $env:ANDROID_HOME
    $localProperties = "$PSScriptRoot\..\local.properties"
    if (Test-Path -LiteralPath $localProperties) {
        $sdkLine = Get-Content -LiteralPath $localProperties | Where-Object { $_ -match "^sdk\.dir=" } | Select-Object -First 1
        if ($sdkLine) { $sdkRoot = $sdkLine.Substring(8).Replace("\:", ":").Replace("\\", "\") }
    }
    if (-not $sdkRoot) { throw "Set sdk.dir in local.properties or ANDROID_HOME to the Android SDK." }
    $adb = Join-Path $sdkRoot "platform-tools\adb.exe"
    if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) { throw "ADB was not found at $adb." }
    Write-Host "Updating the USB-connected app in place with adb install -r; existing app data is retained."
    & $adb -d install -r "$PSScriptRoot\..\app\build\outputs\apk\debug\app-debug.apk"
    exit $LASTEXITCODE
}
exit 0
