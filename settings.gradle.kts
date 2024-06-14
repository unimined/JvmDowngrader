plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.7.0")
}

include("downgradetest")
include("gradle-plugin")
include("java-api")
include("site")

rootProject.name = "JvmDowngrader"

