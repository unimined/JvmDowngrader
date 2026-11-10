import com.sun.imageio.plugins.jpeg.JPEG.vendor
import org.gradle.api.tasks.testing.logging.TestLogEvent
import sun.jvmstat.monitor.MonitoredVmUtil.jvmArgs
import xyz.wagyourtail.jvmdg.util.rangeTo

plugins {
    java
    alias(libs.plugins.jmh)
}

val testVersion = JavaVersion.toVersion(project.findProperty("testVersion") as String)

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
    // providers (xyz.wagyourtail.jvmdg.providers.*) live in java-api; unit tests
    // need them on the classpath for ServiceLoader-driven transforms
    testImplementation(project(":java-api"))
    testImplementation(rootProject.sourceSets["shared"].output)
    testImplementation(libs.gson)
    testImplementation(libs.apache.commons.compress)
    testImplementation(libs.java.diff.utils)
}

val testTargetVersion = JavaVersion.toVersion(project.findProperty("testTargetVersion") as String)

// stub-parity corpus: enumerate registered stub members and emit real call
// sites. Generation parses the running JVM's runtime classes through ASM, and
// generated sources must compile at the same release they were enumerated on,
// so both are capped at the highest Java version ASM can parse (28 for ASM 9.11.x).
val stubSourceMajor = minOf(testVersion.majorVersion.toInt(), 28)
val stubtestsSrcDir = layout.buildDirectory.dir("generated/stubtests")
val stubtestsClasses = layout.buildDirectory.dir("classes/stubtests")
val stubtestsManualClasses = layout.buildDirectory.dir("classes/stubtests-manual")
val stubtestsReports = layout.buildDirectory.dir("reports/stubtests")

// checked-in manual corpus: Support/StubTest + jvmdg.stubtests.manual.* tests.
// Compiles ahead of generation so the generator can reflect @StubTest methods
// into the emitted Index, and lands in stubtests.jar alongside generated code.
val compileStubTestsManual = tasks.register<JavaCompile>("compileStubTestsManual") {
    group = "verification"
    description = "Compiles checked-in stub-parity support and manual tests"
    source = fileTree("src/stubtestsManual/java") {
        include("**/*.java")
    }
    classpath = files()
    destinationDirectory.set(stubtestsManualClasses)
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(stubSourceMajor))
        vendor.set(JvmVendorSpec.AZUL)
    })
    options.release.set(stubSourceMajor)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-nowarn")
}

val generateStubTests = tasks.register<JavaExec>("generateStubTests") {
    group = "verification"
    description = "Enumerates registered stub members and emits per-owner call sites + Index"
    dependsOn(tasks.testClasses, compileStubTestsManual)
    classpath = sourceSets["test"].runtimeClasspath + files(stubtestsManualClasses)
    mainClass.set("xyz.wagyourtail.jvmdg.test.stubgen.StubTestGenerator")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(stubSourceMajor))
        vendor.set(JvmVendorSpec.AZUL)
    })
    args(
        stubtestsSrcDir.get().asFile.absolutePath,
        stubtestsReports.get().asFile.absolutePath,
        stubtestsManualClasses.get().asFile.absolutePath
    )
    outputs.dir(stubtestsSrcDir)
    outputs.dir(stubtestsReports)
    // optional -PstubCoverageMin=<pct> ratchet: fail when covered call sites
    // fall below the given percent of registered stub members
    doLast {
        (findProperty("stubCoverageMin") as String?)?.toDoubleOrNull()?.let { min ->
            val line = stubtestsReports.get().asFile.resolve("coverage.txt").readLines()
                .firstOrNull { it.startsWith("covered ") }
            val pct = line?.substringAfterLast('(')?.substringBefore('%')?.toDoubleOrNull()
            requireNotNull(pct) { "coverage.txt has no covered-percentage line" }
            require(pct >= min) {
                "stub call-site coverage $pct% is below -PstubCoverageMin=$min%; " +
                    "see ${stubtestsReports.get().asFile}/untestable.txt"
            }
        }
    }
}

val compileStubTests = tasks.register<JavaCompile>("compileStubTests") {
    group = "verification"
    description = "Compiles the generated stub-parity call sites"
    dependsOn(generateStubTests, compileStubTestsManual)
    source = fileTree(stubtestsSrcDir) {
        include("**/*.java")
    }
    classpath = files(stubtestsManualClasses)
    destinationDirectory.set(stubtestsClasses)
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(stubSourceMajor))
        vendor.set(JvmVendorSpec.AZUL)
    })
    options.release.set(stubSourceMajor)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-nowarn")
}

val stubtestsJar = tasks.register<Jar>("stubtestsJar") {
    group = "verification"
    description = "Packages the stub-parity corpus jar"
    dependsOn(compileStubTests)
    from(stubtestsClasses)
    from(stubtestsManualClasses)
    archiveFileName.set("stubtests.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
}

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
        project(":java-api").tasks.named("testJar"),
        rootProject.tasks.named("shadowJar"), // -all agent jar for -javaagent tests
        stubtestsJar
    )

    val versions = (testTargetVersion .. testVersion).associateWith {
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(it.majorVersion))
            vendor.set(JvmVendorSpec.AZUL)
        }.get().executablePath.toString()
    }

    doFirst {
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
            "-Djvmdg.test.stubPath=${stubtestsJar.get().archiveFile.get().asFile.absolutePath}",
            "-Djvmdg.test.javaApiPath=${
                project(":java-api").tasks.named("testJar").get().outputs.files.singleFile.absolutePath
            }",
        )
    }

    testLogging {
        events.add(TestLogEvent.PASSED)
        events.add(TestLogEvent.SKIPPED)
        events.add(TestLogEvent.FAILED)
    }
}