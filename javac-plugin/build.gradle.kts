import org.gradle.internal.os.OperatingSystem

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    compileOnly(rootProject.sourceSets["main"].output)
    compileOnly(rootProject.sourceSets["shared"].output)

    // macos doesn't link tools.jar by default for some reason
    val javaHome = javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(8)) }.get().metadata.installationPath
    implementation(files("$javaHome/lib/tools.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.compileTestJava {
    classpath += rootProject.tasks["shadowJar"].outputs.files

    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    options.compilerArgs.add("-Xplugin:jvmdg target=8 log=info")
}

tasks.test {
    useJUnitPlatform()

    javaLauncher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}