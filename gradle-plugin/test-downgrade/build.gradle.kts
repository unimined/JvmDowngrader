import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeAPI
import java.util.*

plugins {
    `java`
    id("xyz.wagyourtail.jvmdowngrader")
}

val props = projectDir.parentFile.parentFile.resolve("gradle.properties").inputStream().use {
    val props = Properties()
    props.load(it)
    props
}

jvmdg.version = props.getProperty("version") as String


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

dependencies {
    implementation("org.jetbrains:annotations-java5:24.1.0")
}

val downgradeJar9 by tasks.creating(DowngradeJar::class) {
    inputFile.set(tasks.jar.get().archiveFile)
    archiveClassifier.set("downgraded-9")
    downgradeTo = JavaVersion.VERSION_1_9
    archiveVersion.set(props.getProperty("version") as String)
}

val shadeDowngradedApi9 by tasks.creating(ShadeAPI::class) {
    inputFile.set(downgradeJar9.archiveFile)
    archiveClassifier.set("downgraded-shaded-9")
    downgradeTo = JavaVersion.VERSION_1_9
}

tasks.build.get().dependsOn(tasks.shadeDowngradedApi)
tasks.build.get().dependsOn(shadeDowngradedApi9)