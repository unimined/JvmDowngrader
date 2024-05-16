# JvmDowngrader

downgrades modern java bytecode to older versions. at either compile or runtime.

## Gradle Plugin

This downgrades the output of a jar task using another task.
Note that certain things like reflection and dynamic class definition downgrading will not work without runtime downgrading.
dynamic class definitions being things like `MethodHandles$Lookup#defineClass` and classloader shenanigans.

add my maven in `settings.gradle`:
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
    id 'xyz.wagyourtail.jvmdowngrader' version '0.3.0'
}

// optionally you can change some globals:

jvmdg.defaultMavens = false // stops from inserting my maven into the project repositories

jvmdg.group = "xyz.wagyourtail.jvmdowngrader" // default
jvmdg.coreArchiveName "jvmdowngrader" // default
jvmdg.apiArchiveName "jvmdowngrader-java-api" // default
jvmdg.version = "0.3.0" // default
jvmdg.asmVersion = "9.7" // default

```

This will create a default downgrade task for `jar` (or `shadowJar` if present) called `downgradeJar` that will downgrade the output to java 8 by default.
as well as a `shadeDowngradedApi` to then insert the required classes for not having a runtime dependency on the api jar.

you can change the downgrade version by doing:
```gradle
downgradeJar {
    downgradeVersion = JavaVersion.VERSION_1_11
    archiveClassifier = "downgraded-11"
}

shadeDowngradedApi {
    downgradeVersion = JavaVersion.VERSION_1_11
    archiveClassifier = "downgraded-11-shaded"
}
```

Optionally, you can also depend on the sahdeDowngradedApi task when running build.
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
    configureDowngrade {
        jvmArgs += ["-Djvmdg.quiet=true"]
    }
}

task customShadeDowngradedApi(type: xyz.wagyourtail.jvmdg.gradle.task.ShadeApi) {
    inputFile = customDowngrade.archiveFile
    downgradeTo = JavaVersion.VERSION_1_8 // default
    shadePath = "${archiveBaseName}/jvmdg/api" // default, where the shaded classes will be placed
    archiveClassifier = "downgraded-8-shaded"
    configureShade {
        jvmArgs += ["-Djvmdg.quiet=true"]
    }
}
```

## "Compile" Time Downgrading

### Zip/Path Downgrading

Downgrades the contents of a zip file or path to an older version.
you can specify multiple targets for bulk operations. (and they will include eachother in classpath searches during downgrading)

ex. `java -jar JvmDowngrader-all.jar -c 52 downgrade -t input.jar output.jar -cp classpath.jar -cp classpath2.jar`

### Shading Api

Analyze a downgraded zip file for API usages and shade them into the output.
you can specify multiple targets for bulk operations.

ex. `java -jar JvmDowngrader-all.jar -c 52 shade -p "shade/prefix" -t input.jar output.jar`

The class version can be replaced with a path to the pre-downgraded api jar to save time, using `-d`.

#### License Concerns

Some people think that shading would mean they're bound by the stricter GPL license. I don't belive this to be the case.

For the purpose of Licensing, the produced jar from this task should be considered a "Combined Work", 
as it contains the original code from the input jar and the shaded code from jvmdowngrader's api.

And this does mean that you shouldn't need to use the *exact* same license, as long as you comply with section 4 of the LGPL.
Running this tool, should be a thing the end-user is capable of doing, thus section 4.d.0 is satisfied as long as
your project provides the pre-shaded jar as well, or alternatively provides source code to build said jar (or this jar).

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
ClassDowngrader.classLoader.addDelegate(new URL[] { new File("jarname.jar").toURI().toURL() });

// call main method
ClassDowngrader.classLoader.loadClass("mainclass").getMethod("main", String[].class).invoke(null, new Object[] { new String[] { "args" } });
```

You can also create your own downgrading classloader, for more complicated environments.
```groovy
// construct with parent of the default class downgrader classloader, as that contains the downgraded api classes.
DowngradingClassLoader loader = new DowngradingClassLoader(ClassDowngrader.classLoader);

// adding jars
loader.addDelegate(new URL[] { new File("jarname.jar").toURI().toURL() });
```

### inspired by

https://github.com/Chocohead/Not-So-New and https://github.com/luontola/retrolambda
