plugins {
    java
    id("xyz.wagyourtail.annotationasm") version "1.0.0-SNAPSHOT"
}

base {
    archivesName = "downgradetest"
}
version = "1.0.0"

repositories {
    maven("https://maven.wagyourtail.xyz/snapshots")
    mavenCentral()
}

dependencies {
    implementation(project(":testing"))
    implementation(libs.asm)
}

val testVersion = JavaVersion.toVersion(project.properties["testVersion"] as String)

java {
    sourceCompatibility = testVersion
    targetCompatibility = testVersion
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(testVersion.majorVersion))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"

    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(testVersion.majorVersion))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}

val removeLibs by tasks.registering {
    doLast {
        delete(fileTree("dir" to "build/libs", "include" to "**/*.jar"))
    }
}

tasks.jar {
    dependsOn(removeLibs)
    destinationDirectory = temporaryDir
}

tasks.annotationASMJar.configure {
    doLast {
//        exec {
//            commandLine(
//                "jarsigner",
//                "-keystore", file("test_app_sign.jks").absolutePath,
//                "-storepass", "changeit",
//                outputs.files.singleFile.absolutePath, "test_app_alias")
//        }.rethrowFailure().assertNormalExitValue()
    }
}

tasks.build {
    dependsOn("annotationASMJar")
}