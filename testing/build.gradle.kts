import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
}

val testVersion = JavaVersion.toVersion(project.properties["testVersion"] as String)

java {
    sourceCompatibility = testVersion
    targetCompatibility = testVersion
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(testVersion.majorVersion))
    }
}


repositories {
    mavenCentral()
}

sourceSets {
    test {
        compileClasspath += rootProject.sourceSets.main.get().compileClasspath + rootProject.sourceSets.main.get().output
        runtimeClasspath += rootProject.sourceSets.main.get().runtimeClasspath + rootProject.sourceSets.main.get().output
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("com.google.code.gson:gson:2.10")
    testImplementation("org.apache.commons:commons-compress:1.26.1")
    testImplementation("io.github.java-diff-utils:java-diff-utils:4.12")
}

val testTargetVersion = JavaVersion.toVersion(project.properties["testTargetVersion"] as String)

tasks.test {
    useJUnitPlatform()

    dependsOn(
        project(":testing:downgrade").tasks.build,
        project(":testing:multi-version").tasks.build,
        project(":java-api").tasks.named("testJar")
    )

    val versions = listOf(
        testVersion,
        testTargetVersion,
        JavaVersion.VERSION_1_7,
        JavaVersion.VERSION_11,
        JavaVersion.VERSION_17
    ).associateWith {
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(it.majorVersion))
        }.get().executablePath.toString()
    }

    jvmArgs(
        "-Djvmdg.test.version=$version",
        "-Djvmdg.test.originalVersion=$testVersion",
        "-Djvmdg.test.javaVersion=${versions.keys.joinToString(File.pathSeparator) { it.majorVersion }}",
        "-Djvmdg.test.launcher=${versions.values.joinToString(File.pathSeparator)}",
        "-Djvmdg.test.downgradeClasspath=${rootProject.sourceSets["shared"].compileClasspath.joinToString(File.pathSeparator) { it.absolutePath }}",
        "-Djvmdg.test.downgradePath=${project(":testing:downgrade").tasks.jar.get().outputs.files.singleFile.absolutePath}",
        "-Djvmdg.test.multiVersionPath=${project(":testing:multi-version").tasks.jar.get().outputs.files.singleFile.absolutePath}",
        "-Djvmdg.test.javaApiPath=${project(":java-api").tasks.named("testJar").get().outputs.files.singleFile.absolutePath}",
    )

    testLogging {
        events.add(TestLogEvent.PASSED)
        events.add(TestLogEvent.SKIPPED)
        events.add(TestLogEvent.FAILED)
    }
}