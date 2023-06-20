plugins {
    kotlin("jvm") version "1.8.0"
    `java-gradle-plugin`
}

dependencies {
    // commons compress
    testImplementation("org.apache.commons:commons-compress:1.21")
}


tasks.jar {
    manifest {
        attributes.putAll(
            mapOf(
                "Implementation-Version" to project.version,
            )
        )
    }
}


gradlePlugin {
    plugins {
        create("simplePlugin") {
            id = "xyz.wagyourtail.jvmdowngrader"
            implementationClass = "xyz.wagyourtail.jvmdg.gradle.JVMDowngraderPlugin"
        }
    }
}