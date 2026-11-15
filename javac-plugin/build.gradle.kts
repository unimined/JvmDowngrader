java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

val java8 = javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(8)) }.get()

dependencies {
    // classpath-only v52 compiles — the raw outputs are v71 under the
    // CG-5 self-host toolchain and this project's javac-8 can't read them
    compileOnly(
        rootProject.files(rootProject.layout.buildDirectory.dir("classes-v52/main"))
            .builtBy(":compileMainV52")
    )
    compileOnly(
        rootProject.files(rootProject.layout.buildDirectory.dir("classes-v52/shared"))
            .builtBy(":compileSharedV52")
    )

    implementation(files("${java8.metadata.installationPath}/lib/tools.jar"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testAnnotationProcessor(project(":"))
}

tasks.compileTestJava {
    dependsOn(project(":java-api").tasks.named("testJar"))

    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.AZUL)
    }

    val apiJar = project(":java-api").tasks.named("testJar").get().outputs.files.singleFile
    options.compilerArgs.add("-Xplugin:jvmdg downgrade shade --prefix test --classVersion 52 --logLevel info --api ${apiJar.absolutePath}")
}

tasks.test {
    useJUnitPlatform()
    // the plugin shades the api jar into the test output dir; only TheTest is a
    // test — discovery must not try to resolve the shaded stub classes (they
    // reference post-8 JDK types absent on this task's Java 8 runtime)
    include("**/TheTest.class")
}