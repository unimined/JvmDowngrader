# JvmDowngrader

Downgrades modern java bytecode to older versions. at either compile or runtime.

This is currently capable of downgrading from Java 22 to Java 8. Java 7 may come in the future.

Currently attempting to downgrade to Java 7 will produce valid class files, but some of the API stubs are broken, and many common ones dont exist.

### After downgrading you must  either shade, or add the api jar to the classpath at runtime.

**It is recommended to use the shade task/cli as documented below, as it will only include necessary methods in your jar.**

alternatively, you can add the api jar in its entirety to the classpath when running the jar.

The api jar can be found at `xyz.wagyourtail.jvmdowngrader:jvmdowngrader-java-api:0.9.0:downgraded-8`
there is also a `downgraded-11` jar there. 

alternatively, to produce other versions you can generate one yourself using the cli:
`java -jar JvmDowngrader-all.jar -c 53 debug downgradeApi ./java-api-9.jar`

Api jars for older java versions *can* still be used for newer java, but may not be as efficient.

## Gradle Plugin

This downgrades the output of a jar task using another task.
Note that certain things like reflection and dynamic class definition downgrading will not work without runtime
downgrading.
dynamic class definitions being things like `MethodHandles$Lookup#defineClass` and classloader shenanigans.

~~add my maven in `settings.gradle`:~~

JvmDowngrader is now on the gradle plugin portal, so you can skip this step, unless you want to use a snapshot version.

```gradle
pluginManagement {
    repositories {
        mavenLocal()
        maven {
            url = "https://maven.wagyourtail.xyz/releases"
        }
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
    id 'xyz.wagyourtail.jvmdowngrader' version '1.0.0'
}

// optionally you can change some globals, here are their default values:
jvmdg.downgradeTo = JavaVersion.VERSION_1_8
jvmdg.apiJar = [this.getClass().getResourceAsStream("jvmdg/java-api-${version}.jar").writeToFile("build/jvmdg/java-api-${version}.jar")]
jvmdg.quiet = false
jvmdg.debug = false
jvmdg.debugSkipStubs = [].toSet()
jvmdg.shadePath = {
    it.substringBefore(".").substringBeforeLast("-").replace(Regex("[.;\\[/]"), "-")
}
```

This will create a default downgrade task for `jar` (or `shadowJar` if present) called `downgradeJar` that will
downgrade the output to java 8 by default.
as well as a `shadeDowngradedApi` to then insert the required classes for not having a runtime dependency on the api
jar.

you can change the downgrade version by doing:

```gradle
downgradeJar {
    downgradeTo = JavaVersion.VERSION_11
}

shadeDowngradedApi {
    downgradeTo = JavaVersion.VERSION_11
}
```

The tasks have all the same flags as the extension, so you can change them separately, 
their default value is to use the global one from the extension.

If you are merging multiple downgraded jars, please merge from the downgradeJar tasks, and then shade on the resulting mono-jar.
otherwise some API stubs may be missing, due to how shade only includes what is used.

Optionally, you can also depend on the shadeDowngradedApi task when running build.

```gradle
assemble.dependsOn shadeDowngradedApi
```

you can create a custom task by doing:

```gradle
task customDowngrade(type: xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar) {
    inputFile = tasks.jar.archiveFile
    downgradeTo = JavaVersion.VERSION_1_8 // default
    classpath = sourceSets.main.compileClasspath // default
    archiveClassifier = "downgraded-8"
}

task customShadeDowngradedApi(type: xyz.wagyourtail.jvmdg.gradle.task.ShadeJar) {
    inputFile = customDowngrade.archiveFile
    archiveClassifier = "downgraded-8-shaded"
}
```

you can also downgrade/shade a `FileCollection`:

```gradle
task downgradeFileCollection(type: xyz.wagyourtail.jvmdg.gradle.task.files.DowngradeFiles) {
    inputCollection = files("file1.jar", "file2.jar")
    downgradeTo = JavaVersion.VERSION_1_8 // default
    classpath = sourceSets.main.runtimeClasspath // default
}

// get the output with
downgradeFileCollection.outputCollection

task shadeFileCollection(type: xyz.wagyourtail.jvmdg.gradle.task.files.ShadeFiles) {
    inputCollection = downgradeFileCollection.outputCollection
    downgradeTo = JavaVersion.VERSION_1_8 // default
}

// get the output with
shadeFileCollection.outputCollection
```

Make sure the task is configured before trying to use the outputCollection,
it's computed from the `toDowngrade` files.

you can downgrade a configuration:

```gradle
configurations {
    downgrade
    implementation.extendsFrom downgrade
}

jvmdg.dg(configurations.downgrade)

dependencies {
    downgrade "newer.java:version:1.0"
}

```

## "Compile" Time Downgrading

### Zip/Path Downgrading

Downgrades the contents of a zip file or path to an older version.
you can specify multiple targets for bulk operations. (and they will include eachother in classpath searches during
downgrading)

ex. `java -jar JvmDowngrader-all.jar -c 52 downgrade -t input.jar output.jar -cp classpath.jar -cp classpath2.jar`

### Shading Api

Analyze a downgraded zip file for API usages and shade them into the output.
you can specify multiple targets for bulk operations.

ex. `java -jar JvmDowngrader-all.jar -c 52 shade -p "shade/prefix" -t input.jar output.jar`

The class version can be replaced with a path to the pre-downgraded api jar to save time, using `-d`.

## Runtime Downgrading

This is basically only here so I can take funny screenshots of minecraft running on java 8.
I recommend the agent method, as it's most reliable.

### Agent Downgrading

Uses the java agent to downgrade at runtime.

ex. `java -javaagent:JvmDowngrader-all.jar -jar myapp.jar`

### Bootstrap Downgrading

Uses the bootstrap main class

ex. `java -jar JvmDowngrader-all.jar bootstrap -cp myapp.jar;classpath.jar;classpath2.jar --main mainclass args`

## From Code

### Downgrading ClassLoader

This is what the bootstrap downgrader essentially uses internally.

```groovy
// add jar to default downgrading classloader
ClassDowngrader.getCurrentVersionDowngrader().getClassLoader().addDelegate(new URL[] { new File("jarname.jar").toURI().toURL() });

// call main method
ClassDowngrader.getCurrentVersionDowngrader().getClassLoader().loadClass("mainclass").getMethod("main", String[].class).invoke(null, new Object[] { new String[] { "args" } });
```

You can also create your own downgrading classloader, for more complicated environments.

```groovy
DowngradingClassLoader loader = new DowngradingClassLoader(ClassDowngrader.getCurrentVersionDowngrader(), parent);

// adding jars
loader.addDelegate(new URL[] { new File("jarname.jar").toURI().toURL() });
```

### inspired by

https://github.com/Chocohead/Not-So-New and https://github.com/luontola/retrolambda
