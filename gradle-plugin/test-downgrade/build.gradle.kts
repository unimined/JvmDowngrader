import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeAPI
import java.util.*

buildscript {
    repositories {
        flatDir {
            dirs("../../build/libs")
        }
        flatDir {
            dirs("../build/libs")
        }
    }
    dependencies {
        val props = projectDir.parentFile.parentFile.resolve("gradle.properties").inputStream().use {
            val props = java.util.Properties()
            props.load(it)
            props
        }
        if (!project.hasProperty("runningTest")) {
            classpath("xyz.wagyourtail.jvmdowngrader:jvmdowngrader-gradle-plugin:${props.getProperty("version")}")
            classpath("xyz.wagyourtail.jvmdowngrader:jvmdowngrader:${props.getProperty("version")}")
        }

        classpath("org.apache.commons:commons-compress:1.26.1")

        classpath("org.ow2.asm:asm:${props.getProperty("asm_version")}")
        classpath("org.ow2.asm:asm-commons:${props.getProperty("asm_version")}")
        classpath("org.ow2.asm:asm-tree:${props.getProperty("asm_version")}")
        classpath("org.ow2.asm:asm-util:${props.getProperty("asm_version")}")
    }
}

val props = projectDir.parentFile.parentFile.resolve("gradle.properties").inputStream().use {
    val props = Properties()
    props.load(it)
    props
}


plugins {
    java
    id("xyz.wagyourtail.jvmdowngrader")
}

val testVersion: JavaVersion = JavaVersion.toVersion(props.getProperty("testVersion") as String)

java {
    sourceCompatibility = testVersion
    targetCompatibility = testVersion
}

tasks.compileJava {
    options.encoding = "UTF-8"

    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(testVersion.majorVersion))
    }
}

repositories {
    flatDir {
        dirs("../../java-api/build/libs")
    }
    flatDir {
        dirs("../../build/libs")
    }
    mavenCentral()
}

sourceSets {
    main {
        java {
            srcDirs("../../downgradetest/src/main/java")
        }
    }
}

val jvmdg = extensions.getByType(JVMDowngraderExtension::class.java)

if (project.hasProperty("runningTest")) {
    jvmdg.apiJar = project.file("../../java-api/build/libs/jvmdowngrader-java-api-${props.getProperty("version")}.jar")
}

val downgrade by configurations.creating
jvmdg.dg(downgrade)

dependencies {
    implementation("org.jetbrains:annotations-java5:24.1.0")

    // first thing I could think of that's not java 8
    downgrade("com.github.javakeyring:java-keyring:1.0.4")

    implementation(files(downgrade.files))
}


val downgradeJar9 by tasks.creating(DowngradeJar::class) {
    inputFile.set(tasks.jar.get().archiveFile)
    archiveClassifier.set("downgraded-9")
    downgradeTo = JavaVersion.VERSION_1_9
    archiveVersion.set(props.getProperty("version") as String)
//    destinationDirectory.set(temporaryDir)
}

val shadeDowngradedApi9 by tasks.creating(ShadeAPI::class) {
    inputFile.set(downgradeJar9.archiveFile)
    archiveClassifier.set("downgraded-shaded-9")
    downgradeTo = JavaVersion.VERSION_1_9
}

tasks.getByName<DowngradeJar>("downgradeJar") {
//    destinationDirectory.set(temporaryDir)
}

tasks.build.get().dependsOn(tasks.getByName("shadeDowngradedApi"))
tasks.build.get().dependsOn(shadeDowngradedApi9)