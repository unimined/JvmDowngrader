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

    testAnnotationProcessor(sourceSets["main"].output)
    testAnnotationProcessor(project.project(":java-api").tasks.getByName("testJar").outputs.files)
    testAnnotationProcessor(rootProject.sourceSets["main"].runtimeClasspath)
}

tasks.compileTestJava {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    options.compilerArgs.add("-Xplugin:jvmdg --classVersion 52 --logLevel info")
}

tasks.test {
    useJUnitPlatform()
}