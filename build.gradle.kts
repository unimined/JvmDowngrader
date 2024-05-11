plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    if (this.path.equals(":java-api")) {
        apply(plugin = "com.github.johnrengelman.shadow")
    }

    version =
        if (project.hasProperty("version_snapshot")) "${project.properties["version"]}-SNAPSHOT" else project.properties["version"] as String
    group = project.properties["maven_group"] as String

    base {
        archivesName.set("${properties["archives_base_name"]}${if (path == ":") "" else path.replace(":", "-")}")
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.jetbrains:annotations-java5:24.1.0")

        implementation("org.ow2.asm:asm:${project.properties["asm_version"]}")
        implementation("org.ow2.asm:asm-tree:${project.properties["asm_version"]}")
        implementation("org.ow2.asm:asm-commons:${project.properties["asm_version"]}")
        implementation("org.ow2.asm:asm-util:${project.properties["asm_version"]}")
    }

    tasks.jar {
        manifest {
            attributes(
                "Manifest-Version" to "1.0",
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
            )
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("com.google.code.gson:gson:2.10")
    testImplementation("org.apache.commons:commons-compress:1.26.1")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

base {

}

val mainVersion = project.properties["mainVersion"] as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(mainVersion.toInt()))
    }
}

tasks.jar {
    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Premain-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Agent-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Can-Retransform-Classes" to "true",
        )
    }
}

val testVersion = project.properties["testVersion"] as String

tasks.compileTestJava {
    options.encoding = "UTF-8"

    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(testVersion.toInt()))
    }
}

tasks.test {
    useJUnitPlatform()
    dependsOn(
        project(":downgradetest").tasks.build,
        project(":java-api").tasks.build
    )
    jvmArgs("-Djvmdg.debug=true")
    javaLauncher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(testVersion.toInt()))
    }
}

tasks.shadowJar {
    relocate("org.objectweb.asm", "xyz.wagyourtail.jvmdg.shade.asm")
}

val jarInJar by tasks.registering(Jar::class) {
    group = "jvmdg"
    dependsOn(tasks.shadowJar)

    archiveClassifier.set("jij")
    duplicatesStrategy = DuplicatesStrategy.WARN

    // Copy the shadow jar contents into the jij jar
    from(zipTree(tasks.shadowJar.get().outputs.files.singleFile))

    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Premain-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Agent-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Can-Retransform-Classes" to "true",
        )
    }

    dependsOn(project(":java-api").tasks.shadowJar)

    from(project.project(":java-api").tasks.getByName("shadowJar").outputs.files) {
        into("META-INF/lib")
        rename {
            "java-api.jar"
        }
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
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
            artifact(project.tasks["jarInJar"]) {
                classifier = "all"
            }
        }
    }
}