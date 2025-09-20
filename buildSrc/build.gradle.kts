plugins {
    kotlin("jvm") version "2.2.0"
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
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation(gradleApi())

    implementation(libs.apache.commons.compress)

    implementation(libs.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)
}
