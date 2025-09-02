import xyz.wagyourtail.gradle.shadow.ShadowJar

plugins {
    kotlin("jvm") version libs.versions.kotlin apply false
    java
    `maven-publish`
    `java-library`
    signing
    application
    alias(libs.plugins.gradle.metadata)
    alias(libs.plugins.gradle.test.retry)
    alias(libs.plugins.nmcp)
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "signing")
    apply(plugin = "maven-publish")
    apply(plugin = rootProject.libs.plugins.gradle.metadata.get().pluginId)
    apply(plugin = rootProject.libs.plugins.gradle.test.retry.get().pluginId)
    apply(plugin = rootProject.libs.plugins.nmcp.get().pluginId)

    metadata {
        url.set("https://github.com/unimined/JvmDowngrader")
        license {
            shortName.set("LGPL-2.1")
            fullName.set("GNU Lesser General Public License v2.1")
            url.set("https://www.gnu.org/licenses/lgpl-2.1.html")
        }
        organization {
            url.set("https://wagyourtail.xyz")
        }
        developers {
            register("wagyourtail") {
                fullName.set("William Gray")
                email.set("jvmdowngrader@wagyourtail.xyz")
            }
        }
        github {
            org.set("unimined")
            repo.set("JvmDowngrader")
            pages()
            issues()
        }
    }

    java {
        if (!project.path.startsWith(":testing")) {
            withSourcesJar()
            withJavadocJar()
        }
    }

    version = if (project.hasProperty("version_snapshot")) {
        "${project.properties["version"]}-SNAPSHOT"
    } else {
        project.properties["version"] as String
    }
    group = project.properties["maven_group"] as String

    base {
        archivesName.set("${properties["archives_base_name"]}${if (path == ":") "" else path.replace(":", "-")}")
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly(rootProject.libs.jetbrains.annotations.j5)
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

    tasks.test {
        retry {
            maxRetries = 3
        }
    }

    signing {
        val signingKey = findProperty("signingKey") as String?
        if (signingKey != null)
            useInMemoryPgpKeys(signingKey, findProperty("signingPassword") as String?)
        else
            isRequired = false
    }
}

metadata {
    readableName.set("JvmDowngrader")
    description = "A tool to downgrade java api and bytecode usages"
}

nmcp {
    publishAllPublications {}
    publishAggregation {
        project(":")
        project(":java-api")

        username = project.properties["ossrhUsername"] as String?
        password = project.properties["ossrhPassword"] as String?
        publicationType = "AUTOMATIC"
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
    val api by configurations.getting

    api(libs.asm)
    api(libs.asm.tree)
    api(libs.asm.commons)
    api(libs.asm.util)
}

val mainVersion = project.properties["mainVersion"] as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(mainVersion.toInt()))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

tasks.jar {
    from(sourceSets["main"].output, sourceSets["shared"].output)
    from(project("javac-plugin").sourceSets["main"].output)

    from(rootDir.resolve("LICENSE.md"))
    from(rootDir.resolve("license")) {
        into("license")
    }

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

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
    from(project("javac-plugin").sourceSets["main"].allSource)
    from(rootDir.resolve("LGPLv2.1.md"))

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.javadoc {
    source = sourceSets.main.get().allJava + sourceSets["shared"].allJava
}

project.evaluationDependsOnChildren()

val shadowJar by tasks.registering(ShadowJar::class) {
    from(sourceSets["main"].output, sourceSets["shared"].output)
    from(project("javac-plugin").sourceSets["main"].output)
    from(rootDir.resolve("LGPLv2.1.md"))

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    shadowContents.add(sourceSets.main.get().runtimeClasspath)
    relocate("org.objectweb.asm", "xyz.wagyourtail.jvmdg.shade.asm")

    exclude("module-info.class")

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

    // my version lets you include zip/jars without extracting them, so I can include the java-api jar directly in this step
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
    dependsOn(shadowJar.get())
}

tasks.publish.configure {
    if (!project.hasProperty("version_snapshot")) {
        finalizedBy(tasks.getByName("publishAggregatedPublicationToCentralPortal"))
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

            from(components["java"])

            artifact(shadowJar.get()) {
                classifier = "all"
            }
        }
    }
}

signing {
    if (!project.hasProperty("is_local")) {
        sign(publishing.publications.getByName("maven"))
    }
}