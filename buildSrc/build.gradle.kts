import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    kotlin("jvm") version "1.9.22"
}

repositories {
    mavenCentral()
}

file("src/utils").deleteRecursively()
file("../gradle-plugin/src/utils").copyRecursively(file("src/utils"))

val asmVersion: String = project.properties["asm_version"]?.toString() ?: run {
    projectDir.parentFile.resolve("gradle.properties").inputStream().use {
        val props = Properties()
        props.load(it)
        props.getProperty("asm_version") as String
    }
}

sourceSets.main {
    java.srcDirs("src/utils/java")
    kotlin.srcDirs("src/utils/kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    val targetVersion = 8
    if (JavaVersion.current().isJava9Compatible) {
        options.release.set(targetVersion)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation(gradleApi())

    // commons compress
    implementation("org.apache.commons:commons-compress:1.26.1")

    implementation("org.ow2.asm:asm:${asmVersion}")
    implementation("org.ow2.asm:asm-commons:${asmVersion}")
    implementation("org.ow2.asm:asm-tree:${asmVersion}")
}
