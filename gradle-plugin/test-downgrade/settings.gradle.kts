pluginManagement {
    repositories {
        mavenCentral()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("baseLibs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "test-downgrade"