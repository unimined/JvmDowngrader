import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    `java-gradle-plugin`
    `maven-publish`
}

java {
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // commons compress
    implementation("org.apache.commons:commons-compress:1.26.1")

    // asm
    implementation("org.ow2.asm:asm:${project.properties["asm_version"]}")
    implementation("org.ow2.asm:asm-commons:${project.properties["asm_version"]}")
    implementation("org.ow2.asm:asm-tree:${project.properties["asm_version"]}")

    testImplementation(kotlin("test"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaCompile> {
    options.release = 8
}

tasks.jar {
    manifest {
        attributes.putAll(
            mapOf(
                "Implementation-Version" to project.version,
            )
        )
    }
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
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group as String
                artifactId = project.properties["archives_base_name"] as String? ?: project.name
                version = project.version as String
                from(components["java"])
            }
        }
    }
}