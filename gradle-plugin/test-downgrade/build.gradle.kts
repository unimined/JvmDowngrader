import java.util.*

plugins {
    `java`
    id("xyz.wagyourtail.jvmdowngrader")
}

val props = projectDir.parentFile.parentFile.resolve("gradle.properties").inputStream().use {
    val props = Properties()
    props.load(it)
    props
}

jvmdg.version = props.getProperty("version") as String


val testVersion: JavaVersion = JavaVersion.toVersion(props.getProperty("testVersion") as String)

java {
    sourceCompatibility = testVersion
    targetCompatibility = testVersion
}

tasks.compileJava {
    options.encoding = "UTF-8"

    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(testVersion.majorVersion))
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

dependencies {
    implementation("org.jetbrains:annotations-java5:24.1.0")
}

tasks.build.get().dependsOn(tasks.shadeDowngradedApi)