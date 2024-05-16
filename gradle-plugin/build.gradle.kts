import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    kotlin("jvm") version "1.9.22"
    `java-gradle-plugin`
    `maven-publish`
    id("io.github.sgtsilvio.gradle.metadata") version "0.5.0"
}

metadata {
    readableName.set("JvmDowngrader Gradle Plugin")
    description = "A Gradle plugin to downgrade java api and bytecode usages"
}

java {
    withSourcesJar()
}

repositories {
    mavenCentral()
}

val asmVersion: String = project.properties["asm_version"]?.toString() ?: run {
    projectDir.parentFile.resolve("gradle.properties").inputStream().use {
        val props = Properties()
        props.load(it)
        props.getProperty("asm_version") as String
    }
}

dependencies {
    // commons compress
    implementation("org.apache.commons:commons-compress:1.26.1")

    implementation(project(":"))
    implementation(rootProject.sourceSets.getByName("shared").output)

    testImplementation(kotlin("test"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaCompile> {
    options.release = 8
}

tasks.jar {
    from(projectDir.parentFile.resolve("LICENSE.md"))

    manifest {
        attributes.putAll(
            mapOf(
                "Implementation-Version" to project.version,
            )
        )
    }

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.getByName<Jar>("sourcesJar") {
    from(projectDir.parentFile.resolve("LICENSE.md"))

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

gradlePlugin {
    plugins {
        create("simplePlugin") {
            id = "xyz.wagyourtail.jvmdowngrader"
            implementationClass = "xyz.wagyourtail.jvmdg.gradle.JVMDowngraderPlugin"
            version = project.version as String
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
}