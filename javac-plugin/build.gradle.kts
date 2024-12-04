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
    testAnnotationProcessor(rootProject.sourceSets["main"].runtimeClasspath)
}

tasks.compileTestJava {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    val apiJar = project(":java-api").tasks.named("testJar").get().outputs.files.singleFile
    options.compilerArgs.add("-Xplugin:jvmdg downgrade shade --prefix test --classVersion 52 --logLevel info --api ${apiJar.absolutePath}")
}

tasks.test {
    useJUnitPlatform()
}