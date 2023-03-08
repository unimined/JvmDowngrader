plugins {
    kotlin("jvm") version "1.8.0" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    version = if (project.hasProperty("version_snapshot")) project.properties["version"] as String + "-SNAPSHOT" else project.properties["version"] as String
    group = project.properties["maven_group"] as String

    extensions.getByType(BasePluginExtension::class.java).apply {
        archivesName.set(project.properties["archives_base_name"] as String + project.path.replace(":", "-"))
    }

    repositories {
        mavenCentral()
    }

}