import xyz.wagyourtail.gradle.shadow.ShadowJar

plugins {
    kotlin("jvm") version "1.9.22"
    application
}

repositories {
    mavenCentral()
}

val ktorVersion by project.properties

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("io.ktor:ktor-server-auto-head-response:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

    implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.13")

    implementation("org.jetbrains:markdown:0.7.3")

    implementation(project(":"))
    implementation(rootProject.sourceSets.getByName("shared").output)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass = "xyz.wagyourtail.jvmdg.maven.MainKt"
}

tasks.getByName("run", JavaExec::class) {
    dependsOn(project(":java-api").tasks.jar)
    jvmArgs("-Djvmdg.java-api=${project(":java-api").tasks.jar.get().archiveFile.get().asFile.absolutePath}")
    workingDir = temporaryDir
    classpath += files(project.rootDir)
}

val myShadowJar by tasks.creating(ShadowJar::class) {
    from(sourceSets.main.get().output)
    from(projectDir.parentFile.resolve("LICENSE.md"))
    from(projectDir.parentFile.resolve("README.md"))

    shadowContents = listOf(project.configurations.compileClasspath.get())

    // so we don't have to retrieve it.
    dependsOn(project.project(":java-api").tasks.jar.get())
    from(project.project(":java-api").tasks.jar.get().outputs.files) {
        into("META-INF/lib")
        rename {
            "java-api.jar"
        }
    }

    exclude("META-INF/versions/**/*")
    exclude("module-info.class")

    duplicatesStrategy = DuplicatesStrategy.WARN

    manifest {
        attributes.putAll(
            mapOf(
                "Implementation-Version" to project.version,
                "Main-Class" to "xyz.wagyourtail.jvmdg.maven.MainKt"
            )
        )
    }

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.assemble {
    dependsOn(myShadowJar)
}
