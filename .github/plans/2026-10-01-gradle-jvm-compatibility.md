# Gradle JVM compatibility

**Date:** 2026-10-01
**Status:** Complete

Gradle 8.14.5 cannot run on the previously selected Java 26 runtime. IntelliJ now has a Java 17 project SDK, and `JAVA_HOME` points to a compatible Java 17 installation.

1. Set the Gradle JVM to `#JAVA_HOME` in `.idea/gradle.xml`, so IDE builds use Java 17 without embedding a machine-specific path.
2. Register JDK 17 in IntelliJ's Project Structure > SDKs and set the project SDK to `17` in `.idea/misc.xml`.
3. Confirmed Gradle 8.14.5 runs on Java 17 and `assembleDebug` succeeds. The Gradle, plugin, and Java/Kotlin target versions remain unchanged.
