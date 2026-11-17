import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import xyz.wagyourtail.gradle.coverage.CoverageRunTask
import xyz.wagyourtail.gradle.ctsym.GenerateCtSymTask
import xyz.wagyourtail.gradle.shadow.ShadowJar
import xyz.wagyourtail.jvmdg.util.plus
import xyz.wagyourtail.jvmdg.util.toOpcode

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.asm)
    }
}

metadata {
    readableName.set("JvmDowngrader Java Api")
    description = "JvmDowngrader's implementations of java api, and the actual downgrade providers"
}

nmcp {
    publishAllPublications {}
}

fun SourceSet.inputOf(sourceSet: SourceSet) {
    compileClasspath += sourceSet.compileClasspath
    runtimeClasspath += sourceSet.runtimeClasspath
}

fun SourceSet.outputOf(sourceSet: SourceSet) {
    compileClasspath += sourceSet.output
    runtimeClasspath += sourceSet.output
}

operator fun JavaVersion.rangeTo(that: JavaVersion): Iterable<JavaVersion> {
    return JavaVersion.entries.subList(this.ordinal, that.ordinal + 1)
}

operator fun JavaVersion.minus(int: Int): JavaVersion {
    return JavaVersion.entries[this.ordinal - int]
}

val fromVersion = JavaVersion.toVersion(project.findProperty("stubFromVersion")!!)
val toVersion = JavaVersion.toVersion(project.findProperty("stubToVersion")!!) + 1

sourceSets {
    for (vers in fromVersion..toVersion) {
        create("java${vers.ordinal + 1}") {
            inputOf(sourceSets.main.get())
            main {
                outputOf(this@create)
            }
        }
    }
}

val coverage = sourceSets.create("coverage") {
    inputOf(sourceSets.main.get())
    outputOf(sourceSets.main.get())
    for (vers in fromVersion..toVersion) {
        outputOf(sourceSets["java${vers.ordinal + 1}"])
    }
}

dependencies {
    // classpath-only V52 compiles of the root project's main/shared sources —
    // the project jar dependency would cycle (root's jar is self-downgraded
    // using this project's selfHostApiJar). these dirs carry identical APIs
    // at v52 so the pre-27 toolchains keep working; ASM was previously
    // transitive through project(":") so it's declared explicitly now
    implementation(
        files(rootProject.layout.buildDirectory.dir("classes-v52/main"))
            .builtBy(":compileMainV52")
    )
    implementation(
        files(rootProject.layout.buildDirectory.dir("classes-v52/shared"))
            .builtBy(":compileSharedV52")
    )
    implementation(libs.asm)
    implementation(libs.asm.tree)
    implementation(libs.asm.commons)
    implementation(libs.asm.util)
}

for (vers in fromVersion..toVersion) {
    tasks.getByName("compileJava${vers.ordinal + 1}Java") {
        this as JavaCompile
        if (vers < JavaVersion.VERSION_1_9) {
            // javac < 8 can't read the shared set's v52 classes (self-host
            // downgrades shared back to mainVersion pre-publish). `--release`
            // on a newer toolchain keeps the same
            // target version AND the ct.sym API-surface guard (only APIs of
            // the version each set downgrades to are visible).
            options.release.set((vers - 1).majorVersion.toInt())
            options.encoding = "UTF-8"
            javaCompiler = javaToolchains.compilerFor {
                languageVersion = JavaLanguageVersion.of(11)
                vendor.set(JvmVendorSpec.AZUL)
            }
        } else {
            configCompile(vers - 1)
        }
    }
}

val mainVersion = JavaVersion.toVersion(project.findProperty("mainVersion")!!)

tasks.compileJava {
    doFirst {
        val tempDir = project.projectDir.resolve("build/tmp/compileJava").resolve("stubClasspath")
        for (vers in fromVersion..toVersion) {
            classpath -= sourceSets["java${vers.ordinal + 1}"].output
            sourceSets["java${vers.ordinal + 1}"].output.files.forEach {
                for (file in it.walk()) {
                    if (file.isFile && file.extension == "class") {
                        // write class file to subdir of tempDir as empty class file of version 1.7
                        val path = file.relativeTo(it).path
                        val stubFile = tempDir.resolve(path)
                        stubFile.parentFile.mkdirs()
                        val stubClass = ClassWriter(0)
                        val reader = file.inputStream().buffered().use { ClassReader(it) }
                        val node = ClassNode()
                        reader.accept(node, ClassReader.SKIP_CODE)
                        stubClass.visit(
                            mainVersion.toOpcode(),
                            node.access,
                            node.name,
                            node.signature,
                            node.superName,
                            node.interfaces.toTypedArray()
                        )
                        for (innerClass in node.innerClasses ?: listOf()) {
                            stubClass.visitInnerClass(
                                innerClass.name,
                                innerClass.outerName,
                                innerClass.innerName,
                                innerClass.access
                            )
                        }
                        stubClass.visitEnd()
                        stubFile.writeBytes(stubClass.toByteArray())
                    }
                }
            }
        }
        classpath += files(tempDir)
    }
    configCompile(mainVersion)
}

tasks.compileTestJava {
    configCompile(toVersion - 1)
}

tasks.getByName<JavaCompile>("compileCoverageJava") {
    configCompile(toVersion - 1)
}

val genCtSym = tasks.register<GenerateCtSymTask>("genCtSym") {
    group = "jvmdg"
    lowerVersion = fromVersion
    upperVersion = toVersion - 1
}

val coverageReport = tasks.register<CoverageRunTask>("coverageReport") {
    group = "jvmdg"
    dependsOn(testJar, genCtSym, tasks.getByName("compileCoverageJava"), "genStubIndex")
    apiJar.set(testJar.get().archiveFile.get().asFile)
    // the generated-artifact dir rides the classpath so registered handlers
    // pointing at generated classes ($jvmdg$fwd/$retro/$dispatch) resolve
    classpath = coverage.runtimeClasspath + files(layout.buildDirectory.dir("generated/stubindex"))
    ctSym.set(genCtSym.get().ctSym)
    javaVersion = toVersion - 1
}

// serialize every provider's reflective-scan registry to META-INF/jvmdg/stubs.idx —
// the runtime loads this instead of re-running the scan (parity: same scan, dumped)
val stubIndexDir = layout.buildDirectory.dir("generated/stubindex")
val genStubIndex = tasks.register<JavaExec>("genStubIndex") {
    group = "jvmdg"
    dependsOn(":classes", "classes")
    for (vers in fromVersion..toVersion) {
        dependsOn("java${vers.ordinal + 1}Classes")
    }
    classpath = files(
        rootProject.sourceSets["main"].output,
        rootProject.sourceSets["shared"].output,
        sourceSets.main.get().output,
        (fromVersion..toVersion).map { sourceSets["java${it.ordinal + 1}"].output }
    ) + configurations.runtimeClasspath.get()
    mainClass.set("xyz.wagyourtail.jvmdg.version.idx.StubIndexWriter")
    args = listOf(
        stubIndexDir.get().file("META-INF/jvmdg/stubs.idx").asFile.absolutePath,
        project.version.toString(),
        genCtSym.get().ctSym.get().asFile.absolutePath,
        stubIndexDir.get().file("META-INF/jvmdg/retrotypes").asFile.absolutePath,
        stubIndexDir.get().asFile.absolutePath,
        stubIndexDir.get().file("META-INF/jvmdg/excludes").asFile.absolutePath,
        stubIndexDir.get().file("META-INF/jvmdg/wraps").asFile.absolutePath,
        layout.buildDirectory.dir("generated/gen-src").get().asFile.absolutePath,
        stubIndexDir.get().file("META-INF/jvmdg/gained").asFile.absolutePath,
        stubIndexDir.get().file("META-INF/jvmdg/gained-calls").asFile.absolutePath,
        stubIndexDir.get().file("xyz/wagyourtail/jvmdg/stub/reflection.idx").asFile.absolutePath
    )
    inputs.file(genCtSym.flatMap { it.ctSym })
    dependsOn(genCtSym)
    outputs.dir(stubIndexDir)
    // generated output accumulates across runs otherwise — a renamed
    // stub or a removed generator would leave stale classes in the jar
    doFirst {
        delete(stubIndexDir)
        delete(layout.buildDirectory.dir("generated/gen-src"))
    }
    javaLauncher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of((toVersion - 1).majorVersion))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

tasks.jar {
    dependsOn(coverageReport)
    dependsOn(":selfDowngradeShared")
    from(*((fromVersion..toVersion).map { sourceSets["java${it.ordinal + 1}"].output }).toTypedArray())
    from(zipTree(rootProject.layout.buildDirectory.file("selfhost/shared-downgraded-8.jar")))
    from(genStubIndex)
    from(coverageReport.get().coverageReports) {
        into("META-INF/coverage")
        exclude("**/unmatched.txt")
        exclude("**/parentOnly.txt")
    }
    from(projectDir.parentFile.resolve("LICENSE.md"))

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    manifest {
        attributes(mapOf(
            "Manifest-Version" to "1.0",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Automatic-Module-Name" to "xyz.wagyourtail.jvmdg.java_api"
        ))
    }
}

// self-host input for the root project's selfDowngrade execs — providers +
// stubs + idx with raw (stubToVersion) shared classes inside; only ever
// loaded by the self-host exec on a stubToVersion launcher, never shipped
val selfHostApiJar = tasks.register<Jar>("selfHostApiJar") {
    from(*((fromVersion..toVersion).map { sourceSets["java${it.ordinal + 1}"].output }).toTypedArray())
    from(rootProject.sourceSets.getByName("shared").output)
    from(sourceSets.main.get().output)
    from(genStubIndex)
    destinationDirectory = layout.buildDirectory.dir("selfhost")

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val testJar = tasks.register<Jar>("testJar") {
    dependsOn(":selfDowngradeShared")
    from(*((fromVersion..toVersion).map { sourceSets["java${it.ordinal + 1}"].output }).toTypedArray())
    from(zipTree(rootProject.layout.buildDirectory.file("selfhost/shared-downgraded-8.jar")))
    from(sourceSets.main.get().output)
    from(genStubIndex)

    destinationDirectory = temporaryDir

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.getByName<Jar>("sourcesJar") {
    from(*((fromVersion..toVersion).map { sourceSets["java${it.ordinal + 1}"].allSource }).toTypedArray())
    from(rootProject.sourceSets.getByName("shared").allSource)
    from(projectDir.parentFile.resolve("LICENSE.md"))
}

tasks.javadoc {
    javadocTool = javaToolchains.javadocToolFor {
        languageVersion.set(JavaLanguageVersion.of((toVersion - 1).majorVersion))
        vendor.set(JvmVendorSpec.AZUL)
    }
    for (vers in fromVersion..toVersion) {
        source += sourceSets["java${vers.ordinal + 1}"].allJava
    }
    source += rootProject.sourceSets.getByName("shared").allJava
    (options as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
}

val shadowJar = tasks.register<ShadowJar>("shadowJar") {
    dependsOn(tasks.jar.get().taskDependencies)
    from(*((fromVersion..toVersion).map { sourceSets["java${it.ordinal + 1}"].output } + sourceSets.main.get().output).toTypedArray())
    from(genStubIndex)

    archiveClassifier.set("all")

    destinationDirectory.set(temporaryDir)

    relocate("org.objectweb.asm", "xyz.wagyourtail.jvmdg.shade.asm")

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

fun JavaCompile.configCompile(version: JavaVersion) {
    sourceCompatibility = version.toString()
    targetCompatibility = version.toString()

    options.encoding = "UTF-8"

    // EOL toolchain JVMs (zulu 9/10/... predate current CPU-detection fixes)
    // can silently corrupt string/array copies on newer runner CPUs via
    // misdetected AVX intrinsics — observed in CI as Gradle worker daemons
    // dying at slf4j init with NUL-mangled class names. Disabling AVX use
    // removes the corrupting intrinsics. Gradle hands jvmArgs to whatever
    // JVM/argv runs the compile: toolchain <= 7 execs `javac` (needs the
    // -J prefix), >= 8 spawns a worker daemon on the toolchain JVM (plain).
    options.isFork = true
    options.forkOptions.jvmArgs =
        listOf(if (version >= JavaVersion.VERSION_1_8) "-XX:UseAVX=0" else "-J-XX:UseAVX=0")

    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(version.majorVersion)
        vendor.set(JvmVendorSpec.AZUL)
    }
}

fun downgradeApi(version: JavaVersion): TaskProvider<Jar> {
    val tempFile = project.layout.buildDirectory.get().asFile.resolve("jvmdg").resolve("javaApi-${project.version}-downgraded-${version.majorVersion}.jar")

    val downgradeJarExec = tasks.register("downgradeJar${version.majorVersion}Exec", JavaExec::class) {
        group = "jvmdg"
        dependsOn(tasks.jar)
        val apiJar = tasks.jar.get().archiveFile.get().asFile.absolutePath

        mainClass.set("xyz.wagyourtail.jvmdg.compile.ZipDowngrader")
        classpath = configurations.runtimeClasspath.get()
        workingDir = project.layout.buildDirectory.get().asFile
        jvmArgs = listOf("-Djvmdg.javaApi=$apiJar")
        args = listOf(
            version.toOpcode().toString(),
            apiJar,
            tempFile.absolutePath
        )

        javaLauncher = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of((toVersion - 1).majorVersion))
            vendor.set(JvmVendorSpec.AZUL)
        }
    }

    return tasks.register("downgradeJar${version.majorVersion}", Jar::class) {
        group = "jvmdg"
        dependsOn(downgradeJarExec)
        archiveClassifier.set("downgraded-${version.majorVersion}")
        from(zipTree(tempFile))

        manifest {
            attributes(mapOf(
                "Manifest-Version" to "1.0",
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Automatic-Module-Name" to "xyz.wagyourtail.jvmdg.java_api"
            ))
        }
    }
}

val downgradeJar25 = downgradeApi(JavaVersion.VERSION_25)
val downgradeJar21 = downgradeApi(JavaVersion.VERSION_21)
val downgradeJar17 = downgradeApi(JavaVersion.VERSION_17)
val downgradeJar11 = downgradeApi(JavaVersion.VERSION_11)
val downgradeJar8 = downgradeApi(JavaVersion.VERSION_1_8)

tasks.assemble {
    dependsOn(downgradeJar25)
    dependsOn(downgradeJar21)
    dependsOn(downgradeJar17)
    dependsOn(downgradeJar11)
    dependsOn(downgradeJar8)
}


publishing {
    repositories {
        maven {
            name = "WagYourMaven"
            url = if (project.hasProperty("version_snapshot")) {
                uri("https://maven.wagyourtail.xyz/snapshots/")
            } else {
                uri("https://maven.wagyourtail.xyz/releases/")
            }
            credentials {
                username = project.findProperty("mvn.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("mvn.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = rootProject.group as String
            artifactId = rootProject.property("archives_base_name") as String + "-java-api"
            version = rootProject.version as String

            from(components["java"])

            artifact(downgradeJar25) {
                classifier = "downgraded-25"
            }

            artifact(downgradeJar21) {
                classifier = "downgraded-21"
            }

            artifact(downgradeJar17) {
                classifier = "downgraded-17"
            }

            artifact(downgradeJar11) {
                classifier = "downgraded-11"
            }

            artifact(downgradeJar8) {
                classifier = "downgraded-8"
            }
        }
    }
}


signing {
    if (!project.hasProperty("is_local")) {
        sign(publishing.publications.getByName("maven"))
    }
}