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

    implementation(libs.apache.commons.compress)

    implementation(libs.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)
}
