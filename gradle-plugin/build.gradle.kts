import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    kotlin("jvm") version "1.9.22"
    `java-gradle-plugin`
    `java-library`
    `maven-publish`
    id("io.github.sgtsilvio.gradle.metadata") version "0.5.0"
    id("com.gradle.plugin-publish") version "1.2.1"
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
//
//val utils by sourceSets.creating {
//    compileClasspath += sourceSets["main"].compileClasspath
//    runtimeClasspath += sourceSets["main"].runtimeClasspath
//
//    sourceSets["main"].compileClasspath += this.output
//    sourceSets["main"].runtimeClasspath += this.output
//
//    sourceSets["test"].compileClasspath += this.output
//    sourceSets["test"].runtimeClasspath += this.output
//}

val asmVersion: String = project.properties["asm_version"]?.toString() ?: run {
    projectDir.parentFile.resolve("gradle.properties").inputStream().use {
        val props = Properties()
        props.load(it)
        props.getProperty("asm_version") as String
    }
}

dependencies {
    gradleApi()

    // commons compress
    implementation("org.apache.commons:commons-compress:1.26.1")

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

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.test {
    dependsOn(":java-api:assemble")
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