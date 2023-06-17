plugins {
    kotlin("jvm") version "1.8.0"
    `java-gradle-plugin`
}

dependencies {
    // commons compress
    testImplementation("org.apache.commons:commons-compress:1.21")
}


tasks.jar {
//    from(sourceSets["api"].output, sourceSets["main"].output)

    archiveClassifier.set("gradle")

    manifest {
        attributes.putAll(
            mapOf(
                "Implementation-Version" to project.version,
            )
        )
    }
}


publishing {
    repositories {
        maven {
            name = "WagYourMaven"
            if (project.hasProperty("version_snapshot")) {
                url = uri("https://maven.wagyourtail.xyz/snapshots/")
            } else {
                url = uri("https://maven.wagyourtail.xyz/releases/")
            }
            credentials {
                username = project.findProperty("mvn.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("mvn.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group as String
            artifactId = rootProject.name
            version = project.version as String

            from(components["java"])
        }
    }
}