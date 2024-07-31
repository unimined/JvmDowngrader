plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.7.0")
}

include("gradle-plugin")
include("java-api")
include("site")

include("testing")
include("testing:downgrade")
include("testing:multi-version")

rootProject.name = "JvmDowngrader"

