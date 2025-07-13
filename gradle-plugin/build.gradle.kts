import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `java-library`
    `maven-publish`
    alias(libs.plugins.gradle.metadata)
    alias(libs.plugins.gradle.plugin.publish)
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

sourceSets.main {
    java.srcDirs("src/utils/java")
    kotlin.srcDirs("src/utils/kotlin")
}

dependencies {
    gradleApi()

    // commons compress
    implementation(libs.apache.commons.compress)

    api(project(":"))
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
    from(projectDir.parentFile.resolve("license")) {
        into("license")
    }

    // so we don't have to retrieve it.
    dependsOn(project.project(":java-api").tasks.jar.get())
    from(project.project(":java-api").tasks.jar.get().outputs.files) {
        into("META-INF/lib")
        rename {
            "java-api.jar"
        }
    }

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
    from(projectDir.parentFile.resolve("license")) {
        into("license")
    }

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.test {
    dependsOn(":java-api:testJar")
}

signing.isRequired = !project.hasProperty("is_local")

gradlePlugin {
    website = metadata.url
    vcsUrl = metadata.github.get().vcsUrl
    plugins {
        create("simplePlugin") {
            id = "xyz.wagyourtail.jvmdowngrader"
            displayName = metadata.readableName.get()
            description = project.description
            tags.set(listOf("jvm", "bytecode", "converter"))
            implementationClass = "xyz.wagyourtail.jvmdg.gradle.JVMDowngraderPlugin"
            version = project.version as String
        }
    }
}

tasks.publish.configure {
    if (!project.hasProperty("version_snapshot")) {
        finalizedBy(tasks.getByName("publishPlugins"))
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