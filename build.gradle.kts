plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `maven-publish`
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    version = if (project.hasProperty("version_snapshot")) "${project.properties["version"]}-SNAPSHOT" else project.properties["version"] as String
    group = project.properties["maven_group"] as String

    base {
        archivesName.set("${properties["archives_base_name"]}${path.replace(":", "-")}")
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.ow2.asm:asm:9.4")
        implementation("org.ow2.asm:asm-commons:9.4")
        implementation("org.ow2.asm:asm-tree:9.4")
        implementation("org.ow2.asm:asm-util:9.4")
        implementation("org.ow2.asm:asm-analysis:9.4")
        implementation("io.github.classgraph:classgraph:4.8.156")
    }

    tasks.jar {
        manifest {
            attributes(
                    "Manifest-Version" to "1.0",
                    "Implementation-Title" to base.archivesName.get(),
                    "Implementation-Version" to project.version,
            )
        }
    }
}

configurations {
    create("jij")
}

sourceSets {
    create("gradle")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("com.google.code.gson:gson:2.9.0")
    testImplementation("org.apache.commons:commons-compress:1.21")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.jar {
    from(sourceSets["gradle"].output, sourceSets["main"].output)
}

tasks.test {
    useJUnitPlatform()
    dependsOn(
            project(":downgradetest").tasks.build,
            project(":java-api").tasks.build
    )
    jvmArgs("-Djvmdg.debug=true")
}

tasks.register("jarInJar", Jar::class.java) {
    dependsOn(tasks.shadowJar)
    archiveClassifier.set("jij")

    // Copy the shadow jar contents into the jij jar
    from(zipTree(tasks.shadowJar.get().outputs.files.singleFile))

    from(configurations["jij"]) {
        into("META-INF/lib")
        rename {
            "java-api.jar"
        }
    }
}

tasks.compileJava {
    sourceCompatibility = JavaVersion.VERSION_1_7.toString()
    targetCompatibility = JavaVersion.VERSION_1_7.toString()

    options.encoding = "UTF-8"

    var version = 7
    if (JavaVersion.current().isJava9Compatible()) {
        options.release.set(version)
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
                username = project.properties["mvn.user"] as String? ?: System.getenv("USERNAME")
                password = project.properties["mvn.key"] as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = rootProject.group as String
            artifactId = rootProject.property("archives_base_name") as String
            version = rootProject.version as String

            artifact(project.tasks.jar) {}
            artifact(project.tasks.shadowJar) {
                classifier = "all"
            }
            artifact(project.tasks["jarInJar"]) {
                classifier = "all-java-api"
            }
        }
    }
}