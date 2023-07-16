import groovyjarjarasm.asm.Opcodes

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

val fromVersion = JavaVersion.VERSION_1_8
val toVersion = JavaVersion.VERSION_16

sourceSets {
    for (vers in fromVersion..toVersion) {
        create("java${vers.ordinal + 2}") {
            inputOf(main.get())
            main {
                outputOf(this@create)
            }
        }
    }
}

dependencies {
    implementation(project(":"))
}

for (vers in fromVersion..toVersion) {
    tasks.getByName("compileJava${vers.ordinal + 2}Java") {
        (this as JavaCompile).configCompile(vers)
    }
}

tasks.compileJava {
    configCompile(JavaVersion.VERSION_1_7)
}

tasks.jar {
    from(*sourceSets.toList().map { it.output }.toTypedArray())
}

fun JavaCompile.configCompile(version: JavaVersion) {
    sourceCompatibility = version.toString()
    targetCompatibility = version.toString()

    options.encoding = "UTF-8"
    options.release.set(version.ordinal + 1)
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
    else -> throw IllegalArgumentException("Unsupported Java Version: $vers")
}

tasks.register("downgradeJar11", Jar::class.java) {
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


tasks.register("downgradeJar8", Jar::class.java) {
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