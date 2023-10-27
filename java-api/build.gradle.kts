import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.ow2.asm:asm:${project.properties["asm_version"]}")
    }
}

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
val toVersion = JavaVersion.toVersion(project.properties["stubToVersion"]!!)

sourceSets {
    for (vers in fromVersion..toVersion) {
        create("java${vers.ordinal + 1}") {
            inputOf(main.get())
            main {
                outputOf(this@create)
            }
        }
    }
}

dependencies {
    implementation(project(":")) {
        isTransitive = false
    }
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
                            jvToOpc(mainVersion),
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

tasks.jar {
    from(*sourceSets.toList().map { it.output }.toTypedArray())
}

tasks.shadowJar {
    from(*sourceSets.toList().map { it.output }.toTypedArray())
    archiveClassifier.set("jij")
    relocate("org.objectweb.asm", "xyz.wagyourtail.jvmdg.shade.asm")
    configurations = listOf()
}

fun JavaCompile.configCompile(version: JavaVersion) {
    sourceCompatibility = version.toString()
    targetCompatibility = version.toString()

    options.encoding = "UTF-8"

    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(version.majorVersion)
    }
}

fun jvToOpc(vers: JavaVersion): Int = when (vers) {
    JavaVersion.VERSION_1_1 -> Opcodes.V1_1
    JavaVersion.VERSION_1_2 -> Opcodes.V1_2
    JavaVersion.VERSION_1_3 -> Opcodes.V1_3
    JavaVersion.VERSION_1_4 -> Opcodes.V1_4
    JavaVersion.VERSION_1_5 -> Opcodes.V1_5
    JavaVersion.VERSION_1_6 -> Opcodes.V1_6
    JavaVersion.VERSION_1_7 -> Opcodes.V1_7
    JavaVersion.VERSION_1_8 -> Opcodes.V1_8
    JavaVersion.VERSION_1_9 -> Opcodes.V9
    JavaVersion.VERSION_1_10 -> Opcodes.V10
    JavaVersion.VERSION_11 -> Opcodes.V11
    JavaVersion.VERSION_12 -> Opcodes.V12
    JavaVersion.VERSION_13 -> Opcodes.V13
    JavaVersion.VERSION_14 -> Opcodes.V14
    JavaVersion.VERSION_15 -> Opcodes.V15
    JavaVersion.VERSION_16 -> Opcodes.V16
    JavaVersion.VERSION_17 -> Opcodes.V17
    JavaVersion.VERSION_18 -> Opcodes.V18
    JavaVersion.VERSION_19 -> Opcodes.V19
    JavaVersion.VERSION_20 -> Opcodes.V20
    JavaVersion.VERSION_21 -> Opcodes.V21
    JavaVersion.VERSION_22 -> Opcodes.V22
    else -> throw IllegalArgumentException("Unsupported Java Version: $vers")
}

val downgradeJar11 by tasks.registering(Jar::class) {
    dependsOn(tasks.jar)
    archiveClassifier.set("downgraded-11")

    doLast {
        val apiJar = tasks.jar.get().archiveFile.get().asFile.absolutePath

        val result = javaexec {
            mainClass.set("xyz.wagyourtail.jvmdg.compile.ZipDowngrader")
            classpath = sourceSets.main.get().compileClasspath
            workingDir = project.buildDir
            jvmArgs = listOf("-Djvmdg.java-api=$apiJar")
            args = listOf(
                jvToOpc(JavaVersion.VERSION_11).toString(),
                apiJar,
                archiveFile.get().asFile.absolutePath
            )
        }
        if (result.exitValue != 0) {
            throw Exception("Failed to downgrade jar")
        }
    }
}


val downgradeJar8 by tasks.registering(Jar::class) {
    dependsOn(tasks.jar)
    archiveClassifier.set("downgraded-8")

    doLast {
        val apiJar = tasks.jar.get().archiveFile.get().asFile.absolutePath

        javaexec {
            mainClass.set("xyz.wagyourtail.jvmdg.compile.ZipDowngrader")
            classpath = sourceSets.main.get().compileClasspath
            workingDir = project.buildDir
            jvmArgs = listOf("-Djvmdg.java-api=$apiJar")
            args = listOf(
                jvToOpc(JavaVersion.VERSION_1_8).toString(),
                apiJar,
                archiveFile.get().asFile.absolutePath
            )
        }
    }
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

            artifact(tasks["jar"]) {}

            artifact(tasks["downgradeJar11"]) {
                classifier = "downgraded-11"
            }

            artifact(tasks["downgradeJar8"]) {
                classifier = "downgraded-8"
            }
        }
    }
}