# JvmDowngrader

downgrades modern java bytecode to older versions. at either compile and/or runtime (requires runtime for certain things
to work such as `MethodHandles$Lookup#defineClass`).

## "Compile" Time Downgrading

### Zip Downgrading

~~Downgrades the contents of a zip file to an older version.~~

~~ex. `java -cp JvmDowngrader-all.jar xyz.wagyourtail.jvmdg.compile.ZipDowngrader 52 input.jar output.jar classpath.jar;classpath2.jar`~~

Shading the required api is currently only supported in the gradle plugin. so this isn't recommended outside of debugging.

### Directory Downgrading

This feature is not complete yet.

### Gradle Plugin

This downgrades the output of a jar task using another task.
Note that certain things like reflection and dynamic class definition downgrading will not work without runtime downgrading.
dynamic class definitions being things like `MethodHandles$Lookup#defineClass` and classloader shenanigans.

add my maven in `settings.gradle`:
```gradle
pluginManagement {
    repositories {
        mavenLocal()
        maven {
            url = "https://maven.wagyourtail.xyz/snapshots"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
```

in `build.gradle`:
```gradle
// add the plugin
plugins {
    id 'xyz.wagyourtail.jvmdowngrader' version '0.0.1-SNAPSHOT'
}
```

This will create a default downgrade task for `jar` called `downgradeJar` that will downgrade the output to java 8 by default, if you have `shadowJar`, it will use that instead of `jar`.
as well as a `shadeDowngradedApi` to then insert the required classes for not having a runtime dependency.

you can change the downgrade version by doing:
```gradle
downgradeJar {
    downgradeVersion = JavaVersion.VERSION_1_11
}
```

Optionally, you can also depend on the sahdeDowngradedApi task when running build.
```gradle
build.dependsOn shadeDowngradedApi
```

you can create a custom task by doing:
```gradle
task customDowngrade(type: xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar) {
    inputFile = tasks.jar.archiveFile
    downgradeTo = JavaVersion.VERSION_1_8 // default
    archiveClassifier = "downgraded-8"
}

task customShadeDowngradedApi(type: xyz.wagyourtail.jvmdg.gradle.task.ShadeDowngradedApi) {
    inputFile = customDowngrade.archiveFile
    downgradeTo = JavaVersion.VERSION_1_8 // default
    sourceSet = sourceSets.main // default
    shadePath = "${archiveBaseName}/jvmdg/api" // default
    archiveClassifier = "downgraded-8-shaded"
}
```

## Runtime Downgrading

### Agent Downgrading
Uses the java agent to downgrade at runtime.

ex. `java -javaagent:JvmDowngrader-all.jar -jar myapp.jar`

### Bootstrap Downgrading
Uses the bootstrap main class

ex. `java -jar JvmDowngrader-all.jar myapp.jar;classpath.jar;classpath2.jar mainclass args`

### inspired by

https://github.com/Chocohead/Not-So-New and https://github.com/luontola/retrolambda
