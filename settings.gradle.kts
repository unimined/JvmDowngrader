pluginManagement {
    repositories {
        maven("https://maven.wagyourtail.xyz/snapshots")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}

include("gradle-plugin")
include("java-api")
include("site")

include("testing")
include("testing:downgrade")
include("testing:multi-version")

rootProject.name = "JvmDowngrader"

