plugins {
    kotlin("jvm") version "1.8.0"
}

fun SourceSet.inputOf(sourceSet: SourceSet) {
    compileClasspath += sourceSet.compileClasspath
    runtimeClasspath += sourceSet.runtimeClasspath
}

fun SourceSet.outputOf(sourceSet: SourceSet) {
    compileClasspath += sourceSet.output
    runtimeClasspath += sourceSet.output
}

operator fun JavaVersion.rangeTo(that: JavaVersion): Array<JavaVersion> {
    return JavaVersion.values().copyOfRange(this.ordinal, that.ordinal + 1)
}

sourceSets {
    create("standalone") {
        inputOf(main.get())
        outputOf(main.get())
        for (vers in JavaVersion.VERSION_1_8..JavaVersion.VERSION_16) {
            var set = create("java${vers.ordinal + 2}") {
                inputOf(main.get())
                outputOf(main.get())
            }
            outputOf(set)
        }
    }
    test {
        inputOf(sourceSets["standalone"])
        outputOf(sourceSets["standalone"])
    }
}

dependencies {

    // ow2 asm
    implementation("org.ow2.asm:asm:9.4")
    implementation("org.ow2.asm:asm-commons:9.4")
    implementation("org.ow2.asm:asm-tree:9.4")
    implementation("org.ow2.asm:asm-util:9.4")
    implementation("org.ow2.asm:asm-analysis:9.4")

    // classgraph
    implementation("io.github.classgraph:classgraph:4.8.156")

    testImplementation(kotlin("test"))
    testImplementation("org.apache.commons:commons-compress:1.21")
}

fun JavaCompile.configCompile(version: JavaVersion) {
    sourceCompatibility = version.toString()
    targetCompatibility = version.toString()

    options.encoding = "UTF-8"
    options.release.set(version.ordinal + 1)
}

tasks.compileJava {
    configCompile(JavaVersion.VERSION_1_7)
}

tasks.getByName("compileStandaloneJava") {
    (this as JavaCompile).configCompile(JavaVersion.VERSION_1_7)
}

for (vers in JavaVersion.VERSION_1_8..JavaVersion.VERSION_16) {
    tasks.getByName("compileJava${vers.ordinal + 2}Java") {
        (this as JavaCompile).configCompile(vers);
    }
}

tasks.jar {
    from(*sourceSets.toList().map { it.output }.toTypedArray())
}

tasks.test {
    useJUnitPlatform()
    dependsOn(
        project(":downgradetest").tasks["build"],
//        tasks.build
    )
}
