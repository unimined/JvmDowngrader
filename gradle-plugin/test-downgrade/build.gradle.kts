@file:Suppress("DSL_SCOPE_VIOLATION")

import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.flags.convention
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeJar
import xyz.wagyourtail.jvmdg.gradle.task.files.DowngradeFiles
import xyz.wagyourtail.jvmdg.gradle.task.files.ShadeFiles
import java.util.*

buildscript {
    repositories {
        flatDir {
            dirs("../../build/libs")
        }
        flatDir {
            dirs("../build/libs")
        }
    }
    dependencies {
        val props = projectDir.parentFile.parentFile.resolve("gradle.properties").inputStream().use {
            @Suppress("UNRESOLVED_REFERENCE", "RemoveRedundantQualifierName") // lmao
            val props: java.util.Properties = java.util.Properties()
            props.load(it)
            props
        }

        if (!project.hasProperty("runningTest")) {
            classpath("xyz.wagyourtail.jvmdowngrader:jvmdowngrader-gradle-plugin:${props.getProperty("version")}")
            classpath("xyz.wagyourtail.jvmdowngrader:jvmdowngrader:${props.getProperty("version")}")
        }

        classpath("org.apache.commons:commons-compress:1.26.1")

        val asm_version = "9.8"


        classpath(baseLibs.asm)
        classpath(baseLibs.asm.commons)
        classpath(baseLibs.asm.tree)
        classpath(baseLibs.asm.util)
    }
}

val props = projectDir.parentFile.parentFile.resolve("gradle.properties").inputStream().use {
    val props = Properties()
    props.load(it)
    props
}


plugins {
    java
    if (project.hasProperty("runningTest")) {
        id("xyz.wagyourtail.jvmdowngrader")
    }
}
if (!project.hasProperty("runningTest")) {
    apply(plugin = "xyz.wagyourtail.jvmdowngrader")
}

val stubToVersion: JavaVersion = JavaVersion.toVersion(props.getProperty("stubToVersion") as String)

java {
    sourceCompatibility = stubToVersion
    targetCompatibility = stubToVersion
}

tasks.compileJava {
    options.encoding = "UTF-8"

    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(stubToVersion.majorVersion))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

repositories {
    flatDir {
        dirs("../../java-api/build/libs")
    }
    flatDir {
        dirs("../../build/libs")
    }
    mavenCentral()
}

sourceSets {
    main {
        java {
            srcDirs("../../downgradetest/src/main/java")
        }
    }
}

val jvmdg = extensions.getByType(JVMDowngraderExtension::class.java)

if (project.hasProperty("runningTest")) {
    jvmdg.apiJar =
        listOf(project.file("../../java-api/build/tmp/testJar/jvmdowngrader-java-api-${props.getProperty("version")}.jar"))
}

val downgrade by configurations.creating
// bind the dep transforms to a named spec — its flags replace the
// extension's for this configuration's downgrades
val dgScoped = jvmdg.spec("dg-scoped") {
    downgradeTo.set(JavaVersion.VERSION_11)
}
jvmdg.dg(downgrade, dgScoped)

dependencies {
    implementation("org.jetbrains:annotations-java5:24.1.0")

    // first thing I could think of that's not java 8
    downgrade("com.github.javakeyring:java-keyring:1.0.4")

    implementation(files(downgrade.files))
}

val downgradeFiles by tasks.creating(DowngradeFiles::class) {
    inputCollection = sourceSets.main.get().runtimeClasspath
    downgradeTo = JavaVersion.VERSION_1_8
}

// scoped flag specs: a named FlagsSpec conventions the jvmdg
// extension's values; explicit sets win
val scoped = jvmdg.spec("test-scoped") {
    enabled.set(false)
    downgradeTo.set(JavaVersion.VERSION_11)
}

// spec-to-spec chaining: enabled flows through EnabledFlag-aware
// convention(), unset flags fall back to the chained spec
val child = jvmdg.spec("test-child").also {
    it.convention(scoped)
    it.downgradeTo.set(JavaVersion.VERSION_17)
}

tasks.register("verifyScopedSpec") {
    // capture the Property objects, not the spec/script — script references
    // aren't serializable under the configuration cache
    val specEnabled = scoped.enabled
    val specDowngradeTo = scoped.downgradeTo
    val dgSpecDowngradeTo = dgScoped.downgradeTo
    val specLogLevel = scoped.logLevel
    val childEnabled = child.enabled
    val childDowngradeTo = child.downgradeTo
    val childLogLevel = child.logLevel
    doLast {
        check(!specEnabled.get()) { "spec enabled should be false" }
        check(specDowngradeTo.get() == JavaVersion.VERSION_11) { "spec downgradeTo should be 11" }
        check(dgSpecDowngradeTo.get() == JavaVersion.VERSION_11) { "dg spec downgradeTo should be 11" }
        // unset flags inherit the extension's conventions
        check(specLogLevel.get() == "INFO") { "spec logLevel should inherit jvmdg" }
        // chained spec: enabled flows through, own set wins
        check(!childEnabled.get()) { "child spec should inherit enabled=false from parent" }
        check(childDowngradeTo.get() == JavaVersion.VERSION_17) { "child spec downgradeTo should be its own 17" }
        check(childLogLevel.get() == "INFO") { "child spec logLevel should inherit through parent to jvmdg" }
    }
}
tasks.named("build") { dependsOn("verifyScopedSpec") }

val shadeFiles by tasks.creating(ShadeFiles::class) {
    dependsOn(downgradeFiles)
    inputCollection = downgradeFiles.outputCollection
    downgradeTo = JavaVersion.VERSION_1_8
}

val downgradeJar9 by tasks.creating(DowngradeJar::class) {
    inputFile.set(tasks.jar.get().archiveFile)
    archiveClassifier.set("downgraded-9")
    downgradeTo = JavaVersion.VERSION_1_9
    archiveVersion.set(props.getProperty("version") as String)
//    destinationDirectory.set(temporaryDir)
}

val shadeDowngradedApi9 by tasks.creating(ShadeJar::class) {
    inputFile.set(downgradeJar9.archiveFile)
    archiveClassifier.set("downgraded-shaded-9")
    downgradeTo = JavaVersion.VERSION_1_9
}

tasks.getByName<DowngradeJar>("downgradeJar") {
//    destinationDirectory.set(temporaryDir)
}

tasks.build.get().dependsOn(tasks.getByName("shadeDowngradedApi"))
tasks.build.get().dependsOn(shadeDowngradedApi9)