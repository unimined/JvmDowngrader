java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

val java8 = javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(8)) }.get()

dependencies {
    compileOnly(rootProject.sourceSets["main"].output)
    compileOnly(rootProject.sourceSets["shared"].output)

    implementation(files("${java8.metadata.installationPath}/lib/tools.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.compileTestJava {
    classpath += project(":java-api").tasks.getByName("testJar").outputs.files +
            files(rootProject.sourceSets.map { it.compileClasspath }.flatten())

    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    options.compilerArgs.add("-Xplugin:jvmdg --classVersion 52 --logLevel info")
}

tasks.test {
    useJUnitPlatform()
}