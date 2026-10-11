# On-device data lifecycle

TaskChain keeps user data offline in app-private internal storage. `AppContainer` owns
the shared repositories; UI recreation does not create another storage owner.

## Persisted schema

| Path relative to `files/` | Data |
| --- | --- |
| `data/routines/<routineId>.json` | Routine definition, tasks/subtasks, schedule and feedback settings |
| `data/sessions/active-session.json` | Current run snapshot, timestamps and confirmation state |
| `data/history/events.json` | Completed run events, deduplicated by run ID |
| `datastore/user_preferences.preferences_pb` | Preferences DataStore settings |

JSON records use an envelope: `{"schemaVersion":1,"data":<record-or-array>}`.
The former unversioned object/array is version zero. Startup upgrades it atomically,
including the legacy step/task and cue/subtask names. Current names take precedence
when both names occur. Repeating migration leaves current records unchanged.
`EntityMetadata.revision` tracks entity revisions, not storage format versions.

Unsupported schema versions fail without replacement, deletion or quarantine. Future
schema changes must add an explicit migration and old-version fixtures before raising
the supported version. Downgrading an app cannot safely edit newer records merely
because unknown JSON keys are ignored.

Repositories and migration serialize file access through one process-wide mutex.
`AtomicFile` protects replacement writes and recovers its legacy `.bak` file; it does
not provide locking. Malformed JSON is preserved as `.corrupt-*`, with rename success
checked. I/O failures are surfaced rather than treated as empty storage. Startup
offers Retry and withholds editable screens when saved data cannot be opened.

Only completed runs enter history. Completion recovery appends before clearing the
run, so retrying an interrupted write does not duplicate history. Aborted runs stay
in the current run file and do not reschedule completion reminders.

## Restarts and updates

Reboot and process recreation preserve files and preferences. Default internal
storage becomes available after the first unlock following reboot. The receiver
reconciles reminders after boot, clock/time-zone changes and package replacement.
Pre-unlock reminders are not implemented.

Persisted run timestamps use wall-clock time. Existing semantics count elapsed time
while an unpaused task is running; this change does not introduce a new reboot or
clock-change timer policy.

An update must retain the application ID and compatible signing identity. Use an
in-place install (`adb install -r`) rather than uninstalling the app first. Uninstall
and clear-data operations remove user data. Changing the application ID creates a
different sandbox and does not transfer existing records.
Published releases need increasing `versionCode` values; storage `schemaVersion`
changes independently when the persisted format changes.

`allowBackup="false"` preserves the offline cloud-backup policy. It is not a user
export/import feature; device-transfer behavior can vary by manufacturer.

## Device testing

The regular debug app remains `com.taskchain`. Instrumentation targets the dedicated
`deviceTest` build, `com.taskchain.testapp`, with a separate app-private directory.
This sandbox is disposable and uses synthetic fixtures. Reset helpers reject the
regular app ID. Never point fixture cleanup, `pm clear`, or test orchestration at
the app containing valued user data.

With a complete JDK 17 configured, build using `scripts/gradle-agent.cmd`:

```text
:app:testDebugUnitTest :app:lintDebug :app:assembleDebug
:app:assembleDeviceTest :app:assembleDeviceTestAndroidTest
:app:connectedDeviceTestAndroidTest
```

The connected test task may replace or remove the testing app. It must not be used
as an update-preservation test. Run upgrade tests separately: seed an older testing
APK, update that same package in place, and verify routines, run, history and settings.
Reboot tests likewise need seeded test data and checks after unlocking. UI state
restoration alone does not prove reboot durability.

Before development operations on valued data, create a fresh local snapshot:

```powershell
./scripts/backup-device-data.ps1 -AdbPath '<SDK>/platform-tools/adb.exe' -DeviceSerial '<serial>'
```

The script checks `run-as`, stops the app for consistency, copies `files/` as binary
tar, validates the archive and records SHA-256. It creates a new folder under the
ignored `.scratch/device-backups/` and leaves the app stopped. It requires a
debuggable build. Retain snapshots outside the device; an uninstall cannot preserve
the only copy stored inside the app sandbox. Restoring must use a compatible app,
verified trusted archive, a stopped process, and binary stdin; PowerShell 5 text
redirection can corrupt tar archives.

## Validation

On 2026-10-11, JDK 17 builds of the regular debug app and isolated device-test app,
Android test compilation, Android lint, and all 116 JVM tests passed. Seven storage
instrumentation tests passed on a Pixel 7, covering migration, backup recovery,
unsupported schemas, quarantine, I/O errors and concurrent access. The snapshot
script produced a validated archive from the isolated package; a same-APK in-place
install and cold launch retained its existing file hash. All five stored files in
the regular app retained their pre-test SHA-256 hashes.

UI checks are pending an unlocked device: the attempted run encountered the lock
screen and could not find Compose hierarchies. A real device reboot and an
older-APK-to-newer-APK upgrade have not been verified.

## Remaining domain work

[ADR 0001](adr/0001-one-run-file-per-routine.md) still describes a pending per-routine
run migration and 18-hour expiry. This hardening retains the global active-run file.
Historical aborted events already written by older versions are retained; this
change does not delete existing history. User-facing export/import and a revised
clock policy require separate behavior decisions.

## References

- [App-specific storage](https://developer.android.com/training/data-storage/app-specific)
- [AtomicFile](https://developer.android.com/reference/android/util/AtomicFile)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Direct Boot](https://developer.android.com/privacy-and-security/direct-boot)
- [Android test runner and clearPackageData](https://developer.android.com/training/testing/instrumented-tests/androidx-test-libraries/runner)
- [Build variants](https://developer.android.com/build/build-variants)
- [ADB package installation](https://developer.android.com/tools/adb)
