<#
Use this script before development operations on valued app data to create a verified local snapshot.
Inputs: AdbPath is the SDK adb executable; DeviceSerial selects one device; PackageName identifies
the debuggable app; DestinationDirectory holds a new snapshot folder. Dependencies: adb run-as, tar.
The app is force-stopped for consistency; it is not uninstalled, cleared, or automatically restarted.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$AdbPath,
    [Parameter(Mandatory = $true)][ValidatePattern('^[A-Za-z0-9._:-]+$')][string]$DeviceSerial,
    [ValidatePattern('^[A-Za-z][A-Za-z0-9_.]+$')][string]$PackageName = 'com.taskchain',
    [string]$DestinationDirectory = (Join-Path $PSScriptRoot '../.scratch/device-backups')
)

$ErrorActionPreference = 'Stop'
$adbExecutable = (Resolve-Path -LiteralPath $AdbPath).Path
$tarExecutable = (Get-Command tar.exe -ErrorAction Stop).Source
& $adbExecutable -s $DeviceSerial shell run-as $PackageName pwd
if ($LASTEXITCODE -ne 0) { throw 'The selected package must be installed and debuggable.' }
& $adbExecutable -s $DeviceSerial shell am force-stop $PackageName
if ($LASTEXITCODE -ne 0) { throw 'Could not stop the app for a consistent snapshot.' }

$snapshotName = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N')
$snapshotDirectory = Join-Path ([IO.Path]::GetFullPath($DestinationDirectory)) $snapshotName
New-Item -ItemType Directory -Path $snapshotDirectory | Out-Null
$archivePath = Join-Path $snapshotDirectory 'files.tar'
$processInfo = New-Object Diagnostics.ProcessStartInfo
$processInfo.FileName = $adbExecutable
$processInfo.Arguments = "-s $DeviceSerial exec-out run-as $PackageName tar -cf - files"
$processInfo.UseShellExecute = $false
$processInfo.CreateNoWindow = $true
$processInfo.RedirectStandardOutput = $true
$processInfo.RedirectStandardError = $true
$process = New-Object Diagnostics.Process
$process.StartInfo = $processInfo
$archiveStream = $null
$processStarted = $false
try {
    if (-not $process.Start()) { throw 'Could not start the archive command.' }
    $processStarted = $true
    $errorOutput = $process.StandardError.ReadToEndAsync()
    $archiveStream = [IO.File]::Open($archivePath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
    $process.StandardOutput.BaseStream.CopyTo($archiveStream)
    $archiveStream.Dispose()
    $archiveStream = $null
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "Snapshot failed: $($errorOutput.GetAwaiter().GetResult())" }
    if ((Get-Item -LiteralPath $archivePath).Length -eq 0) { throw 'Snapshot is empty.' }
    & $tarExecutable -tf $archivePath | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Snapshot archive validation failed.' }
    $manifest = [ordered]@{
        packageName = $PackageName
        deviceSerial = $DeviceSerial
        createdAtUtc = [DateTime]::UtcNow.ToString('o')
        sha256 = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash
        archive = 'files.tar'
    }
    [IO.File]::WriteAllText((Join-Path $snapshotDirectory 'snapshot.json'),
        ($manifest | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
    Write-Output $snapshotDirectory
} catch {
    Write-Warning "Snapshot incomplete. Do not use it to restore: $snapshotDirectory"
    throw
} finally {
    if ($null -ne $archiveStream) { $archiveStream.Dispose() }
    if ($processStarted -and -not $process.HasExited) { $process.Kill() }
    $process.Dispose()
}
