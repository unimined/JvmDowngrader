plugins {
    `cpp-library`
    `java-library`
    application
}

val mainVersion: String by project.properties
val jniImplementation by configurations.creating

configurations.matching { it.name.startsWith("cppCompile") || it.name.startsWith("nativeLink") || it.name.startsWith("nativeRuntime") }.all {
    extendsFrom(jniImplementation)
}

val jniHeaderDirectory = layout.buildDirectory.dir("jniHeaders")

val javaPath = javaToolchains.compilerFor {
    languageVersion.set(JavaLanguageVersion.of(mainVersion.toInt()))
}.get().metadata.installationPath.asFile.canonicalPath

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(mainVersion.toInt()))
    }
}

application {
    mainClass.set("jnitest.JniFunctions")
}

tasks.compileJava {
//    outputs.dir(jniHeaderDirectory)
//    options.compilerArgumentProviders.add(CommandLineArgumentProvider { listOf("-h", jniHeaderDirectory.get().asFile.canonicalPath) })
    doLast {
        exec {
            commandLine(
                "$javaPath/bin/javah",
                "-d", jniHeaderDirectory.get().asFile.canonicalPath,
                "-classpath", sourceSets.main.get().runtimeClasspath.asPath,
                "jnitest.JniFunctions"
            )
        }.rethrowFailure().assertNormalExitValue()
    }
}

library {
    baseName.set("jvmdg")
    binaries.configureEach {
        compileTask.get().dependsOn(tasks.compileJava)
        compileTask.get().compilerArgs.addAll(jniHeaderDirectory.map { listOf("-I", it.asFile.canonicalPath) })
        compileTask.get().compilerArgs.addAll(compileTask.get().targetPlatform.map {
            listOf("-I", "${javaPath}/include") + when {
                it.operatingSystem.isMacOsX -> listOf("-I", "${javaPath}/include/darwin")
                it.operatingSystem.isLinux -> listOf("-I", "${javaPath}/include/linux")
                else -> emptyList()
            }
        })
    }
}

tasks.test {
    val sharedLib = library.developmentBinary.get() as CppSharedLibrary
    dependsOn(sharedLib.linkTask)
    systemProperty("java.library.path", sharedLib.linkFile.get().asFile.parentFile)
}

tasks.named<JavaExec>("run").configure {
    val sharedLib = library.developmentBinary.get() as CppSharedLibrary
    dependsOn(sharedLib.linkTask)
    systemProperty("java.library.path", sharedLib.linkFile.get().asFile.parentFile)
}

tasks.jar {
    from(library.developmentBinary.flatMap { (it as CppSharedLibrary).linkFile })
}