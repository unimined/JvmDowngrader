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
        classpath("org.ow2.asm:asm:${project.properties["asm_version"]}")
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

operator fun JavaVersion.rangeTo(that: JavaVersion): Array<JavaVersion> {
    return JavaVersion.values().copyOfRange(this.ordinal, that.ordinal + 1)
}

operator fun JavaVersion.minus(int: Int): JavaVersion {
    return JavaVersion.values()[this.ordinal - int]
}

val fromVersion = JavaVersion.toVersion(project.properties["stubFromVersion"]!!)
val toVersion = JavaVersion.toVersion(project.properties["stubToVersion"]!!) + 1

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

val coverage by sourceSets.creating {
    inputOf(sourceSets.main.get())
    outputOf(sourceSets.main.get())
    for (vers in fromVersion..toVersion) {
        outputOf(sourceSets["java${vers.ordinal + 1}"])
    }
}

dependencies {
    implementation(project(":"))

    implementation(rootProject.sourceSets.getByName("shared").output)
}

for (vers in fromVersion..toVersion) {
    tasks.getByName("compileJava${vers.ordinal + 1}Java") {
        (this as JavaCompile).configCompile(vers - 1)
    }
}

val mainVersion = JavaVersion.toVersion(project.properties["mainVersion"]!!)

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

val testVersion = JavaVersion.toVersion(project.properties["testVersion"]!!)

tasks.compileTestJava {
    configCompile(testVersion)
}

tasks.getByName<JavaCompile>("compileCoverageJava") {
    configCompile(testVersion)
}

val genCtSym by tasks.registering(GenerateCtSymTask::class) {
    group = "jvmdg"
    upperVersion = toVersion - 1
}

val coverageReport by tasks.registering(CoverageRunTask::class) {
    group = "jvmdg"
    dependsOn(testJar, genCtSym, tasks.getByName("compileCoverageJava"))
    apiJar.set(testJar.get().archiveFile.get().asFile)
    classpath = coverage.runtimeClasspath
    ctSym.set(genCtSym.get().ctSym)
    javaVersion = toVersion - 1
}

tasks.jar {
    dependsOn(coverageReport)
    from(*((fromVersion..toVersion).map { sourceSets["java${it.ordinal + 1}"].output }).toTypedArray())
    from(rootProject.sourceSets.getByName("shared").output)
    from(coverageReport.get().coverageReports) {
        into("META-INF/coverage")
        exclude("**/unmatched.txt")
        exclude("**/parentOnly.txt")
    }
    from(projectDir.parentFile.resolve("LICENSE.md"))
    from(projectDir.parentFile.resolve("license")) {
        into("license")
    }

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val testJar by tasks.registering(Jar::class) {
    from(*((fromVersion..toVersion).map { sourceSets["java${it.ordinal + 1}"].output }).toTypedArray())
    from(rootProject.sourceSets.getByName("shared").output)
    from(sourceSets.main.get().output)

    destinationDirectory = temporaryDir

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.getByName<Jar>("sourcesJar") {
    from(*((fromVersion..toVersion).map { sourceSets["java${it.ordinal + 1}"].allSource }).toTypedArray())
    from(rootProject.sourceSets.getByName("shared").allSource)
    from(projectDir.parentFile.resolve("LICENSE.md"))
    from(projectDir.parentFile.resolve("license")) {
        into("license")
    }
}

tasks.javadoc {
    javadocTool = javaToolchains.javadocToolFor {
        languageVersion.set(JavaLanguageVersion.of((toVersion - 1).majorVersion))
    }
    for (vers in fromVersion..toVersion) {
        source += sourceSets["java${vers.ordinal + 1}"].allJava
    }
    source += rootProject.sourceSets.getByName("shared").allJava
    (options as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
}

val shadowJar by tasks.registering(ShadowJar::class) {
    dependsOn(tasks.jar.get().taskDependencies)
    from(*((fromVersion..toVersion).map { sourceSets["java${it.ordinal + 1}"].output } + sourceSets.main.get().output).toTypedArray())

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

    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(version.majorVersion)
    }
}

fun downgradeApi(version: JavaVersion): TaskProvider<Jar> {
    val tempFile = project.layout.buildDirectory.get().asFile.resolve("jvmdg").resolve("javaApi-${project.version}-downgraded-${version.majorVersion}.jar")

    val downgradeJarExec = tasks.register("downgradeJar${version.majorVersion}Exec", JavaExec::class) {
        group = "jvmdg"
        dependsOn(tasks.jar)
        val apiJar = tasks.jar.get().archiveFile.get().asFile.absolutePath

        val rootMain = project(":").sourceSets.main.get()

        mainClass.set("xyz.wagyourtail.jvmdg.compile.ZipDowngrader")
        classpath = files(rootMain.output, rootMain.runtimeClasspath)
        workingDir = project.layout.buildDirectory.get().asFile
        jvmArgs = listOf("-Djvmdg.javaApi=$apiJar")
        args = listOf(
            version.toOpcode().toString(),
            apiJar,
            tempFile.absolutePath
        )

        javaLauncher = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of((toVersion - 1).majorVersion))
        }
    }

    return tasks.register("downgradeJar${version.majorVersion}", Jar::class) {
        group = "jvmdg"
        dependsOn(downgradeJarExec)
        archiveClassifier.set("downgraded-${version.majorVersion}")
        from(zipTree(tempFile))
    }
}

val downgradeJar21 = downgradeApi(JavaVersion.VERSION_21)
val downgradeJar17 = downgradeApi(JavaVersion.VERSION_17)
val downgradeJar11 = downgradeApi(JavaVersion.VERSION_11)
val downgradeJar8 = downgradeApi(JavaVersion.VERSION_1_8)

tasks.assemble {
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