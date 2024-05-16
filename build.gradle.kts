import xyz.wagyourtail.gradle.shadow.ShadowJar

plugins {
    java
    `maven-publish`
    `java-library`
    application
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        if (project.name != "downgradetest") {
            withSourcesJar()
            withJavadocJar()
        }
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
        compileOnly("org.jetbrains:annotations-java5:24.1.0")
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

val shared by sourceSets.creating {
    compileClasspath += sourceSets["main"].compileClasspath
    runtimeClasspath += sourceSets["main"].runtimeClasspath
}

application {
    mainClass.set("xyz.wagyourtail.jvmdg.cli.Main")
}

sourceSets {
    main {
        compileClasspath += shared.output
        runtimeClasspath += shared.output
    }
    test {
        compileClasspath += shared.output
        runtimeClasspath += shared.output
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("com.google.code.gson:gson:2.10")
    testImplementation("org.apache.commons:commons-compress:1.26.1")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")

    val api by configurations.getting

    api("org.ow2.asm:asm:${project.properties["asm_version"]}")
    api("org.ow2.asm:asm-tree:${project.properties["asm_version"]}")
    api("org.ow2.asm:asm-commons:${project.properties["asm_version"]}")
    api("org.ow2.asm:asm-util:${project.properties["asm_version"]}")
}

val mainVersion = project.properties["mainVersion"] as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(mainVersion.toInt()))
    }
}

tasks.jar {
    from(sourceSets["main"].output, sourceSets["shared"].output)
    from(rootDir.resolve("LICENSE.md"))

    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to "xyz.wagyourtail.jvmdg.cli.Main",
            "Premain-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Agent-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Can-Retransform-Classes" to "true",
        )
    }
}

tasks.getByName<Jar>("sourcesJar") {
    from(sourceSets["shared"].allSource)
    from(rootDir.resolve("LICENSE.md"))
}

tasks.javadoc {
    source = sourceSets.main.get().allJava + sourceSets["shared"].allJava
}

val testVersion = project.properties["testVersion"] as String

tasks.compileTestJava {
    options.encoding = "UTF-8"

    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(testVersion.toInt()))
    }
}

tasks.test {
    outputs.upToDateWhen { false }
    useJUnitPlatform()

    dependsOn(
        project(":downgradetest").tasks.build,
        project(":java-api").tasks.build
    )
//    jvmArgs("-Djvmdg.debug=true")
    javaLauncher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(testVersion.toInt()))
    }
}


val shadowJar by tasks.registering(ShadowJar::class) {
    from(sourceSets["main"].output, sourceSets["shared"].output)
    relocate("org.objectweb.asm", "xyz.wagyourtail.jvmdg.shade.asm")
    shadowContents.add(sourceSets.main.get().runtimeClasspath)

    destinationDirectory.set(temporaryDir)

    exclude("module-info.class")
}

project.evaluationDependsOnChildren()

val jarInJar by tasks.registering(Jar::class) {
    group = "jvmdg"

    dependsOn(shadowJar.get())
    from(zipTree(shadowJar.get().outputs.files.singleFile))
    from(rootDir.resolve("LICENSE.md"))

    archiveClassifier.set("all")

    duplicatesStrategy = DuplicatesStrategy.WARN

    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to "xyz.wagyourtail.jvmdg.cli.Main",
            "Premain-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Agent-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Can-Retransform-Classes" to "true",
        )
    }

    dependsOn(project(":java-api").tasks.getByName("shadowJar"))

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

tasks.assemble.configure {
    dependsOn(jarInJar.get())
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

            from(components["java"])

            artifact(jarInJar.get()) {
                classifier = "all"
            }
        }
    }
}