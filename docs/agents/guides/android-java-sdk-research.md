# Android Java SDK/JDK policy for TaskChain

Use JDK 17 for the Gradle launcher, daemon, Java toolchain, and app source/bytecode compatibility. Other runtime versions crash on the user machines.

Set JAVA_HOME to a complete installed JDK 17 containing java, javac, and jlink. The build helpers validate the release version and required executables; they do not select a fallback runtime. CI and Gradle daemon criteria also require version 17.

AGENTS.md is the authoritative build guidance. Do not install or download another runtime to resolve a build failure.
