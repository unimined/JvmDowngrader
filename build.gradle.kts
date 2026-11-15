import xyz.wagyourtail.gradle.shadow.ShadowJar
import xyz.wagyourtail.jvmdg.util.toOpcode

plugins {
    kotlin("jvm") version libs.versions.kotlin apply false
    java
    `maven-publish`
    `java-library`
    signing
    application
    alias(libs.plugins.gradle.metadata)
//    alias(libs.plugins.gradle.test.retry)
    alias(libs.plugins.nmcp)
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "signing")
    apply(plugin = "maven-publish")
    apply(plugin = rootProject.libs.plugins.gradle.metadata.get().pluginId)
//    apply(plugin = rootProject.libs.plugins.gradle.test.retry.get().pluginId)
    apply(plugin = rootProject.libs.plugins.nmcp.get().pluginId)

    metadata {
        url.set("https://github.com/unimined/JvmDowngrader")
        license {
            shortName.set("LGPL-2.1-or-later")
            fullName.set("GNU Lesser General Public License v2.1 or later")
            url.set("https://www.gnu.org/licenses/lgpl-2.1.html")
        }
        organization {
            url.set("https://wagyourtail.xyz")
        }
        developers {
            register("wagyourtail") {
                fullName.set("William Gray")
                email.set("jvmdowngrader@wagyourtail.xyz")
            }
        }
        github {
            org.set("unimined")
            repo.set("JvmDowngrader")
            pages()
            issues()
        }
    }

    java {
        if (!project.path.startsWith(":testing")) {
            withSourcesJar()
            withJavadocJar()
        }
    }

    version = if (project.hasProperty("version_snapshot")) {
        "${project.findProperty("version")}-SNAPSHOT"
    } else {
        project.findProperty("version") as String
    }
    group = project.findProperty("maven_group") as String

    base {
        archivesName.set("${findProperty("archives_base_name")}${if (path == ":") "" else path.replace(":", "-")}")
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly(rootProject.libs.jetbrains.annotations.j5)
    }

    tasks.jar {
        manifest {
            attributes(
                "Manifest-Version" to "1.0",
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
            )
        }
    }

//    tasks.test {
//        retry {
//            maxRetries = 3
//        }
//    }

    signing {
        val signingKey = findProperty("signingKey") as String?
        if (signingKey != null)
            useInMemoryPgpKeys(signingKey, findProperty("signingPassword") as String?)
        else
            isRequired = false
    }
}

metadata {
    readableName.set("JvmDowngrader")
    description = "A tool to downgrade java api and bytecode usages"
}

nmcp {
    publishAllPublications {}
    publishAggregation {
        project(":")
        project(":java-api")

        username = project.findProperty("ossrhUsername") as String?
        password = project.findProperty("ossrhPassword") as String?
        publicationType = "AUTOMATIC"
    }
}

val shared = sourceSets.create("shared") {
    compileClasspath += sourceSets["main"].compileClasspath
    runtimeClasspath += sourceSets["main"].runtimeClasspath
}

application {
    mainClass.set("xyz.wagyourtail.jvmdg.cli.Main")
}

sourceSets {
    main {
        compileClasspath += shared.output
        runtimeClasspath += shared.output
    }
    test {
        compileClasspath += shared.output
        runtimeClasspath += shared.output
    }
}

dependencies {
    val api = configurations["api"]

    api(libs.asm)
    api(libs.asm.tree)
    api(libs.asm.commons)
    api(libs.asm.util)
}

val mainVersion = project.findProperty("mainVersion") as String
val stubToVersion = project.findProperty("stubToVersion") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(stubToVersion.toInt()))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

// the toolchain emits stubToVersion bytecode, but every published artifact is
// self-downgraded to mainVersion — advertise the shipped compatibility, not
// the raw compile output
listOf("apiElements", "runtimeElements").forEach {
    configurations.named(it) {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, mainVersion.toInt())
    }
}

// CG-5 self-host: main/shared compile at stubToVersion so core code can use
// newer JDK APIs, then self-downgrade back to mainVersion pre-publish — the
// shipped artifact still runs on java 8. The downgrader is this build's own
// classes (dogfooding); correctness is gated by the full j8+ test matrix and
// selfHostSmokeTest below.
val selfHostDir = layout.buildDirectory.dir("selfhost")

val rawCoreJar = tasks.register<Jar>("rawCoreJar") {
    group = "jvmdg"
    description = "raw main+shared classes at stubToVersion, input to selfDowngrade"
    from(sourceSets["main"].output, sourceSets["shared"].output)
    destinationDirectory.set(selfHostDir)
    archiveClassifier.set("raw-core")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val rawSharedJar = tasks.register<Jar>("rawSharedJar") {
    group = "jvmdg"
    description = "raw shared classes at stubToVersion, input to selfDowngrade"
    from(sourceSets["shared"].output)
    destinationDirectory.set(selfHostDir)
    archiveClassifier.set("raw-shared")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

/**
 * Runs this build's own ZipDowngrader (raw classes on a stubToVersion
 * launcher) over [inputJar] against the selfHost api jar, emitting the
 * mainVersion-target jar other artifacts consume.
 */
fun selfDowngrade(name: String, inputJar: TaskProvider<Jar>): TaskProvider<JavaExec> {
    val out = selfHostDir.get().file("$name-downgraded-${mainVersion}.jar").asFile
    return tasks.register<JavaExec>("selfDowngrade${name.replaceFirstChar { it.uppercase() }}") {
        // resolved lazily — java-api isn't evaluated yet at script-eval time
        val apiJar = project(":java-api").tasks.named("selfHostApiJar", Jar::class)
        group = "jvmdg"
        dependsOn(inputJar, apiJar)
        mainClass.set("xyz.wagyourtail.jvmdg.compile.ZipDowngrader")
        classpath = files(
            sourceSets["main"].output,
            sourceSets["shared"].output,
            apiJar.flatMap { it.archiveFile }
        ) + configurations.runtimeClasspath.get()
        workingDir = selfHostDir.get().asFile
        jvmArgs = listOf("-Djvmdg.javaApi=${apiJar.get().archiveFile.get().asFile.absolutePath}")
        args = listOf(
            JavaVersion.toVersion(mainVersion).toOpcode().toString(),
            inputJar.get().archiveFile.get().asFile.absolutePath,
            out.absolutePath
        )
        inputs.files(inputJar.flatMap { it.archiveFile }, apiJar.flatMap { it.archiveFile })
        outputs.file(out)
        javaLauncher = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(stubToVersion.toInt()))
            vendor.set(JvmVendorSpec.AZUL)
        }
    }
}

val selfDowngradeCore = selfDowngrade("core", rawCoreJar)
val selfDowngradeShared = selfDowngrade("shared", rawSharedJar)
val selfDowngradedCoreJar = selfHostDir.map { it.file("core-downgraded-${mainVersion}.jar") }
val selfDowngradedSharedJar = selfHostDir.map { it.file("shared-downgraded-${mainVersion}.jar") }

// classpath-only V52 compiles of the same sources — java-api and the
// gradle-plugin compile on pre-27 toolchains that can't read the raw
// outputs, and routing their classpaths through the self-downgraded jars
// would cycle (the downgrader's api input embeds java-api classes). these
// are never shipped; the artifacts everyone consumes are the
// self-downgraded jars above.
val sharedV52Dir = layout.buildDirectory.dir("classes-v52/shared")
val mainV52Dir = layout.buildDirectory.dir("classes-v52/main")

val compileSharedV52 = tasks.register<JavaCompile>("compileSharedV52") {
    source = sourceSets["shared"].java
    classpath = configurations["compileClasspath"]
    destinationDirectory.set(sharedV52Dir)
    options.encoding = "UTF-8"
    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(mainVersion.toInt()))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

val compileMainV52 = tasks.register<JavaCompile>("compileMainV52") {
    dependsOn(compileSharedV52)
    source = sourceSets["main"].java
    classpath = sourceSets["main"].compileClasspath.minus(sourceSets["shared"].output).plus(files(sharedV52Dir))
    destinationDirectory.set(mainV52Dir)
    options.encoding = "UTF-8"
    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(mainVersion.toInt()))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

// self-hosted repackaging: raw main/shared compile at stubToVersion and
// ship in self-downgraded form. the java plugin still feeds jar/shadowJar
// the raw output, so any class still above mainVersion's bytecode level on
// disk is the raw toolchain output — drop it; the downgraded zip supplies
// the v52 copy. duplicatesStrategy resolves identical resource dupes.
val dropRawToolchainClasses = Action<org.gradle.api.file.FileCopyDetails> {
    if (!isDirectory && name.endsWith(".class") && file.isFile) {
        file.inputStream().use { ins ->
            ins.skip(6)
            if (ins.read() * 256 + ins.read() > mainVersion.toInt() + 44) exclude()
        }
    }
}

// the self-downgraded core references api classes at runtime (gained
// dispatchers, stubs) — normally supplied by the agent's loader or an api
// jar on the classpath. for standalone use (java -jar) the v52-downgraded
// api is inlined, the same way the fat jar already nests the raw api jar
// as the downgrade input at META-INF/lib/java-api.jar.
// lazy — java-api isn't evaluated at script-eval time
val apiDowngraded8Jar by lazy { project(":java-api").tasks.named("downgradeJar8", Jar::class) }

tasks.jar {
    dependsOn(selfDowngradeCore)
    from(zipTree(selfDowngradedCoreJar))
    from(zipTree(apiDowngraded8Jar.flatMap { it.archiveFile }))
    from(project("javac-plugin").sourceSets["main"].output)
    eachFile(dropRawToolchainClasses)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(rootDir.resolve("LICENSE.md"))

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to "xyz.wagyourtail.jvmdg.cli.Main",
            "Premain-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Agent-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Can-Retransform-Classes" to "true",
            "Automatic-Module-Name" to "xyz.wagyourtail.jvmdg"
        )
    }
}

tasks.getByName<Jar>("sourcesJar") {
    from(sourceSets["shared"].allSource)
    from(project("javac-plugin").sourceSets["main"].allSource)
    from(rootDir.resolve("LGPLv2.1.md"))

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.javadoc {
    source = sourceSets.main.get().allJava + sourceSets["shared"].allJava
    (options as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
}

project.evaluationDependsOnChildren()

val shadowJar = tasks.register<ShadowJar>("shadowJar") {
    description = "smash everything together"
    dependsOn(selfDowngradeCore)
    from(zipTree(selfDowngradedCoreJar))
    from(zipTree(apiDowngraded8Jar.flatMap { it.archiveFile }))
    from(project("javac-plugin").sourceSets["main"].output)
    eachFile(dropRawToolchainClasses)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(rootDir.resolve("LGPLv2.1.md"))

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    shadowContents.add(sourceSets.main.get().runtimeClasspath)
    relocate("org.objectweb.asm", "xyz.wagyourtail.jvmdg.shade.asm")

    exclude("module-info.class")

    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to "xyz.wagyourtail.jvmdg.cli.Main",
            "Premain-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Agent-Class" to "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            "Can-Retransform-Classes" to "true",
            "Automatic-Module-Name" to "xyz.wagyourtail.jvmdg"
        )
    }

    // my version lets you include zip/jars without extracting them, so I can include the java-api jar directly in this step
    dependsOn(project(":java-api").tasks.getByName("shadowJar"))
    from(project.project(":java-api").tasks.getByName("shadowJar").outputs.files) {
        into("META-INF/lib")
        rename {
            "java-api.jar"
        }
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.assemble.configure {
    dependsOn(shadowJar.get())
}

// CG-5 dogfood gate: the downgraded fat jar (all V52) must run on a j8
// launcher — --version exercises class load + arg parse + indy-desugared
// lambdas in the CLI path
val selfHostSmokeTest = tasks.register<JavaExec>("selfHostSmokeTest") {
    group = "verification"
    description = "runs the self-downgraded fat jar on a java 8 launcher"
    dependsOn(shadowJar)
    classpath = files(shadowJar.flatMap { it.archiveFile })
    mainClass.set("xyz.wagyourtail.jvmdg.cli.Main")
    args = listOf("--version")
    javaLauncher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(mainVersion.toInt()))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

tasks.check {
    dependsOn(selfHostSmokeTest)
}

tasks.publish.configure {
    if (!project.hasProperty("version_snapshot")) {
        finalizedBy(tasks.getByName("publishAggregatedPublicationToCentralPortal"))
    }
}

publishing {
    repositories {
        maven {
            name = "WagYourMaven"
            url = if (project.hasProperty("version_snapshot")) {
                uri("https://maven.wagyourtail.xyz/snapshots/")
            } else {
                uri("https://maven.wagyourtail.xyz/releases/")
            }
            credentials {
                username = project.findProperty("mvn.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("mvn.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = rootProject.group as String
            artifactId = rootProject.property("archives_base_name") as String
            version = rootProject.version as String

            from(components["java"])

            artifact(shadowJar.get()) {
                classifier = "all"
            }
        }
    }
}

signing {
    if (!project.hasProperty("is_local")) {
        sign(publishing.publications.getByName("maven"))
    }
}