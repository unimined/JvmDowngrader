plugins {
    kotlin("jvm") version "1.8.0"
    `java-gradle-plugin`
    `maven-publish`
}


version = if (project.hasProperty("version_snapshot")) "${project.properties["version"]}-SNAPSHOT" else project.properties["version"] as String
group = project.properties["maven_group"] as String

base {
    archivesName.set("${properties["archives_base_name"]}-gradle-plugin")
}

repositories {
    mavenCentral()
}

dependencies {
    // commons compress
    implementation("org.apache.commons:commons-compress:1.21")

    // asm
    implementation("org.ow2.asm:asm:${project.properties["asm_version"]}")
    implementation("org.ow2.asm:asm-commons:${project.properties["asm_version"]}")
    implementation("org.ow2.asm:asm-tree:${project.properties["asm_version"]}")
    implementation("org.ow2.asm:asm-util:${project.properties["asm_version"]}")
    implementation("org.ow2.asm:asm-analysis:${project.properties["asm_version"]}")

    testImplementation(kotlin("test"))
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
    publications {
        create<MavenPublication>("maven") {
            groupId = rootProject.group as String
            artifactId = "gradle-plugin"
            version = rootProject.version as String

            artifact(tasks["jar"]) {}
        }
    }
}