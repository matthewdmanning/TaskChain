# Android SDK Access Diagnosis

## Input

Both the repository and standalone UI-library `local.properties` files use:

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

Java properties decoding resolves that value to:

```text
C:\Users\<you>\AppData\Local\Android\Sdk
```

The expected tools are:

```text
C:\Users\<you>\AppData\Local\Android\Sdk\platform-tools\adb.exe
C:\Users\<you>\AppData\Local\Android\Sdk\emulator\emulator.exe
```

## Reproduced symptom

The deterministic probe is:

```powershell
$sdkRoot = 'C:\Users\<you>\AppData\Local\Android\Sdk'
$adb = Join-Path $sdkRoot 'platform-tools\adb.exe'
Test-Path -LiteralPath $adb -PathType Leaf
& $adb version
```

Inside the workspace sandbox, `Test-Path` raises `UnauthorizedAccessException`. The subsequent invocation can therefore appear as `CommandNotFoundException` or “not recognized,” even though the executable exists.

The identical read-only probe succeeds when rerun with sandbox escalation and reports:

```text
Android Debug Bridge version 1.0.41
Version 37.0.0-14910828
Installed as C:\Users\<you>\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

## Root cause

The configured SDK path is correct. The failure is the agent filesystem sandbox denying direct reads under `%LOCALAPPDATA%`. PowerShell's follow-on errors make that access denial look like a bad or missing SDK path.

The following were ruled out:

- malformed `sdk.dir` escaping;
- missing `platform-tools`;
- missing Android Emulator installation;
- conflicting `ANDROID_HOME` or `ANDROID_SDK_ROOT` values.

## Fix

1. Read `sdk.dir` from `local.properties` and decode the Java-properties escaping.
2. Probe the exact executable once in the sandbox.
3. If the result includes `UnauthorizedAccessException`, rerun that same read-only command with escalation. Do not edit `local.properties`.
4. Run ADB and emulator commands with their absolute SDK paths in the escalated context.

Do not infer “SDK missing” from `Test-Path = false`, `CommandNotFoundException`, or “not recognized” when the same output also contains an access-denied error.

## Verified state

Verified on 2026-09-17:

- SDK root exists at the decoded path.
- `adb.exe` and `emulator.exe` both exist.
- ADB starts successfully.
- Available AVDs are `Pixel_6_-_API_35` and `Pixel_8_APIs`.
- No device or emulator was connected during the probe.
