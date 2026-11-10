import org.gradle.api.tasks.testing.logging.TestLogEvent
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

sourceSets {
    // checked-in manual stub-parity corpus: Support/StubTest +
    // jvmdg.stubtests.manual.* tests. Compiles ahead of generation so the
    // generator can reflect @StubTest methods into the emitted Index, and
    // lands in stubtests.jar alongside generated code.
    create("stubtestsManual")
    // real-program fixture corpus (jvmdg.featuretests.*): whole programs with
    // real main() entry points, downgraded + shaded end-to-end and diffed
    // against native runs. Modular — module-info exercises module handling.
    // Compiles at full testVersion since it exercises newest javac features.
    create("featuretests")
    // multi-release fixture: version-gated mains (*J<N>) run on launchers >= N
    // for multiReleaseOriginal/multiReleaseVersions/downgradeFromMultiReleases
    // flag coverage. Kept out of the corpus jar since those mains are
    // intentionally broken below their version.
    create("multiVersion")
}

val stubtestsManual = sourceSets["stubtestsManual"]

// manual corpus references @Ref (src/shared) for stub-member keys
stubtestsManual.compileClasspath += files(rootProject.sourceSets["shared"].output)
stubtestsManual.runtimeClasspath += files(rootProject.sourceSets["shared"].output)

val featuretests = sourceSets["featuretests"]
val multiVersion = sourceSets["multiVersion"]

// module-info requires xyz.wagyourtail.jvmdg.testing.main (testing's own main
// output, for @TestMain discovery) on the module path
featuretests.compileClasspath += files(sourceSets["main"].output)
featuretests.runtimeClasspath += files(sourceSets["main"].output)

// woven fixture classes javac can't emit (ldc condy, nested condy in bsmArgs)
// are generated as .class files and put on the featuretests compile classpath
// so javac resolves CondyWoven references; they land in featuretests.jar too
val featureClassesDir = layout.buildDirectory.dir("generated/featureClasses")

val generateFeatureClasses = tasks.register<JavaExec>("generateFeatureClasses") {
    group = "verification"
    description = "Emits woven feature-corpus classes (condy) as .class files"
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("xyz.wagyourtail.jvmdg.test.corpusgen.FeatureCorpusGenerator")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(testVersion.majorVersion))
        vendor.set(JvmVendorSpec.AZUL)
    })
    args(featureClassesDir.get().asFile.absolutePath, testVersion.majorVersion)
    outputs.dir(featureClassesDir)
}

tasks.named<JavaCompile>("compileFeaturetestsJava") {
    dependsOn(generateFeatureClasses)
    options.release.set(testVersion.majorVersion.toInt())
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-nowarn",
        // graft generated woven classes (CondyWoven) into this module —
        // classpath entries are unreadable when compiling module-info
        "--patch-module=xyz.wagyourtail.jvmdg.testing.downgrade.main=${featureClassesDir.get().asFile.absolutePath}"
    ))
}

tasks.named<JavaCompile>("compileMultiVersionJava") {
    options.release.set(testVersion.majorVersion.toInt())
    options.encoding = "UTF-8"
    options.compilerArgs.add("-nowarn")
}

val featureJar = tasks.register<Jar>("featureJar") {
    group = "verification"
    description = "Packages the real-program feature-corpus fixture jar"
    dependsOn("compileFeaturetestsJava")
    from(featuretests.output)
    from(featureClassesDir)
    archiveFileName.set("featuretests.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
}

val multiVersionJar = tasks.register<Jar>("multiVersionJar") {
    group = "verification"
    description = "Packages the multi-release fixture jar"
    dependsOn("compileMultiVersionJava")
    from(multiVersion.output)
    archiveFileName.set("multi-version.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
}

// stub-parity corpus: enumerate registered stub members and emit real call
// sites. Generation parses the running JVM's runtime classes through ASM, and
// generated sources must compile at the same release they were enumerated on,
// so both are capped at the highest Java version ASM can parse (28 for ASM 9.11.x).
val stubSourceMajor = minOf(testVersion.majorVersion.toInt(), 28)
val stubtestsSrcDir = layout.buildDirectory.dir("generated/stubtests")
val stubtestsClasses = layout.buildDirectory.dir("classes/stubtests")
val stubtestsReports = layout.buildDirectory.dir("reports/stubtests")

tasks.named<JavaCompile>("compileStubtestsManualJava") {
    options.release.set(stubSourceMajor)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-nowarn")
}

val generateStubTests = tasks.register<JavaExec>("generateStubTests") {
    group = "verification"
    description = "Enumerates registered stub members and emits per-owner call sites + Index"
    dependsOn(tasks.testClasses, "compileStubtestsManualJava")
    classpath = sourceSets["test"].runtimeClasspath + stubtestsManual.output
    mainClass.set("xyz.wagyourtail.jvmdg.test.stubgen.StubTestGenerator")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(stubSourceMajor))
        vendor.set(JvmVendorSpec.AZUL)
    })
    args(
        stubtestsSrcDir.get().asFile.absolutePath,
        stubtestsReports.get().asFile.absolutePath,
        stubtestsManual.output.classesDirs.singleFile.absolutePath
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
    dependsOn(generateStubTests, "compileStubtestsManualJava")
    source = fileTree(stubtestsSrcDir) {
        include("**/*.java")
    }
    classpath = files(stubtestsManual.output)
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
    from(stubtestsManual.output)
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
        featureJar,
        multiVersionJar,
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
            "-Djvmdg.test.downgradePath=${featureJar.get().archiveFile.get().asFile.absolutePath}",
            "-Djvmdg.test.multiVersionPath=${multiVersionJar.get().archiveFile.get().asFile.absolutePath}",
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