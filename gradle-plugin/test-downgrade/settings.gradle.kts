pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("baseLibs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "test-downgrade"
