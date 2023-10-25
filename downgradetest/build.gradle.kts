plugins {
    "java"
}

base {
    archivesName = "downgradetest"
}
version = "1.0.0"

val testVersion = JavaVersion.toVersion(project.properties["testVersion"] as String)

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

val removeLibs by tasks.registering {
    doLast {
        delete(fileTree("dir" to "build/libs", "include" to "**/*.jar"))
    }
}

tasks.jar {
    dependsOn(removeLibs)
}