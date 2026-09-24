# Android Java SDK/JDK research for TaskChain

Access date: 2026-09-17

## Status

This document records an unadopted proposal to pin the Gradle runtime to JDK 25.
The checked-in helpers currently accept an explicit compatible `JAVA_HOME`, then
fall back to the local Gradle-managed Eclipse Temurin JDK 21 or Android Studio
JBR. `AGENTS.md` is the authoritative current build guidance.

The repository already uses Android Gradle Plugin 9.4.1, Gradle 9.6.0, and
Kotlin 2.3.21. Android application source and bytecode remain targeted at Java
17. JDK 25 is not pinned by the current build configuration.

## Proposal

Consider a JDK 25 LTS distribution for the Gradle runtime only if the project
chooses to pin and verify it across command-line and IDE builds. Keep the
Android application source and bytecode target at Java 17 unless the product
specification gains a concrete reason to use newer language or API features.

JDK 25 would be a build-time choice; it would not be shipped to the phone or
emulator. Android Studio could continue to run on its bundled JetBrains Runtime,
while the Gradle JDK would be pinned to JDK 25 for reproducible command-line and
IDE builds.

## Why this is the spec-level choice

- The app has no runtime Java requirement beyond Android's toolchain and has no
  reason to depend on Java 21 or Java 25 APIs.
- JDK 25 is the current LTS line and has a longer support horizon than JDK 21.
- Gradle's compatibility matrix supports running Gradle on Java 25 from
  Gradle 9.1 onward. This makes JDK 25 viable for a current Android build,
  while Java 26 is non-LTS and Java 27 is outside the current Gradle runtime
  support table.
- Java 17 remains the sensible Android compilation target because it preserves
  broad device/tool compatibility. A newer build JDK does not require newer
  app bytecode or Java APIs.

JDK 21 is the current checked-in fallback. Moving to a pinned JDK 25 runtime is
a separate build-policy decision, not an application requirement.

## Dependency implications

JDK 25 does not inherently require changing the app's Android dependencies.
The current configuration and unadopted runtime proposal are:

| Component | Current | Proposed change |
|---|---:|---:|
| JDK used by Gradle | Explicit compatible `JAVA_HOME`, then JDK 21/JBR fallback | Pin 25 LTS |
| Android Gradle Plugin | 9.4.1 | None |
| Gradle wrapper | 9.6.0 | None |
| Kotlin Gradle/plugin stack | 2.3.21 | None |
| Android Java source/bytecode target | 17 | None |

The AGP 9.4 line requires Gradle 9.6.0 and JDK 17 or newer. Kotlin 2.3.21
documents compatibility through AGP 9.4.1. Therefore this
alignment is feasible, but it is a build-tool upgrade rather than a JDK
requirement. After changing it, run the full unit-test, lint, and debug-build
checks; the Compose compiler plugin follows the Kotlin version.

The AGP, Gradle, and Kotlin alignment described above is already checked in. It
does not establish or require a JDK 25 runtime policy.

## Scope if the JDK 25 proposal is adopted

Adoption would be build configuration only:

- Pin the Gradle daemon/runtime requirement to JDK 25; Android Studio's own
  bundled JBR may remain the launcher JVM.
- Keep Java source/target, Kotlin JVM target, and the Java compilation toolchain
  at 17. JDK 25 would run the build, while the APK and library output would stay
  Java 17-compatible.
- Regenerate daemon JVM criteria rather than hand-editing provider URLs, and
  update build-helper wording so it no longer falsely equates the launcher JBR
  with the Gradle daemon JDK.

No application source, Android API level, dependency, or runtime behavior
change is part of this proposal.

## Sources

- Android Developers, [Java versions in Android builds](https://developer.android.com/build/jdks)
- Android Developers, [AGP release notes](https://developer.android.com/build/releases/gradle-plugin)
- Gradle, [compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)
- Kotlin, [Gradle and AGP compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)
- Oracle, [Java SE support roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
