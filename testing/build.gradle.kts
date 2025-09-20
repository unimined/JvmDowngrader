import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    alias(libs.plugins.jmh)
}

val testVersion = JavaVersion.toVersion(project.properties["testVersion"] as String)

java {
    sourceCompatibility = testVersion
    targetCompatibility = testVersion
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(testVersion.majorVersion))
        vendor.set(JvmVendorSpec.AZUL)
    }
}


repositories {
    maven("https://maven.wagyourtail.xyz/snapshots")
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(project(":"))
    testImplementation(rootProject.sourceSets["shared"].output)
    testImplementation(libs.gson)
    testImplementation(libs.apache.commons.compress)
    testImplementation(libs.java.diff.utils)
}

val testTargetVersion = JavaVersion.toVersion(project.properties["testTargetVersion"] as String)

jmh {
    warmupIterations = 2
    iterations = 2
    fork = 2
//    javaLauncher = javaToolchains.launcherFor {
//        languageVersion.set(JavaLanguageVersion.of(8))
//    }
}

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
            vendor.set(JvmVendorSpec.AZUL)
        }.get().executablePath.toString()
    }

    jvmArgs(
        "-Djvmdg.test.version=$version",
        "-Djvmdg.test.originalVersion=$testVersion",
        "-Djvmdg.test.javaVersion=${versions.keys.joinToString(File.pathSeparator) { it.majorVersion }}",
        "-Djvmdg.test.launcher=${versions.values.joinToString(File.pathSeparator)}",
        "-Djvmdg.test.downgradeClasspath=${rootProject.sourceSets["shared"].compileClasspath.joinToString(File.pathSeparator) { it.absolutePath }}",
        "-Djvmdg.test.downgradePath=${
            project(":testing:downgrade").tasks.named("annotationASMJar").get().outputs.files.singleFile.absolutePath
        }",
        "-Djvmdg.test.multiVersionPath=${project(":testing:multi-version").tasks.jar.get().outputs.files.singleFile.absolutePath}",
        "-Djvmdg.test.javaApiPath=${
            project(":java-api").tasks.named("testJar").get().outputs.files.singleFile.absolutePath
        }",
    )

    testLogging {
        events.add(TestLogEvent.PASSED)
        events.add(TestLogEvent.SKIPPED)
        events.add(TestLogEvent.FAILED)
    }
}