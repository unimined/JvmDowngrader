# Contributing

So, you've decided to contribute to JvmDowngrader? Great! 
This document will explain how the project is structured, and some common issues that may arise.

## Project Setup

1. Clone the repository
2. build the project with `./gradlew build`

## Outputs

JvmDowngrader has multiple subProjects,
- The root project exports a `-all` jar, which is the CLI artifact.
- `java-api` is the api stubs for newer java versions.
- `gradle-plugin` is the gradle plugin.
- `downgradetest` is a project that builds a jar for Integration testing. this is used by the tests to run all it's `main` methods.
    - after running the tests, the downgraded jar from the tests will also be in that folder in case it needs to be inspected.

It is recommended that you use `publishToMavenLocal` if you need to test a fix for a problem discovered in another project.

## Stub Guidelines

For licensing reasons, please do not directly copy code from the JDK.

# Troubleshooting

### Unable to download toolchain matching the requirements ({languageVersion=7

JvmDowngrader requires java 7 to build `src/main` and `src/shared` as they target java 7, and access `sun.misc.Unsafe` 
which only has ct.sym entries for java 9+ and `java-8-unsupported-shim` is not compatible with java 7.

The recommended solution is to manually install java 7. If this is not possible for some reason, in [`gradle.properties`](../gradle.properties)
you can change the `mainVersion` from `7` to `8`, and the `sharedVersion` from `7` to `8`. Just be aware for contributing of which
APIs are available in java 7, most notably the lack of lambdas and streams.

### Provisioned Toolchain could not be probed

This usually indicates that gradle tried to use a broken installation of java.
delete the provisioned toolchain from the `GRADLE_HOME/jdks` directory.

If the issue persists, The easiest fix is to manually install the java version stated in the error message.

You can also try modifying [the build gradle](../java-api/build.gradle.kts) to use a different java vendor, as shown in the
[Gradle documentation](https://docs.gradle.org/current/userguide/toolchains.html#sec:configuring_jvm_toolchain)
just search for each instance of `javaToolchains` in that file.

### No matching variant of com.gradleup.nmcp:nmcp:0.0.7 was found.

Gradle requires using *at least* java 11 to run. you will have to change the default java binary on your system, or 
change the version that gradle uses in some other way.

#### IDEA

Changing this in Intellij Idea is fairly simple. In the gradle tab on the right, click on the ⚙ (settings) icon.
Then, in the "Gradle JVM" dropdown, select the java 11 version you have installed, or any newer java version, such as java 17 or 21.
