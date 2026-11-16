# JvmDowngrader

Downgrades modern java bytecode to older versions. at either compile or runtime.

This is currently capable of downgrading from Java 22 to Java 8. Java 7 may come in the future.

Currently attempting to downgrade to Java 7 will produce valid class files, but some of the API stubs are broken, and
many common ones dont exist.

### After downgrading you must either shade, or add the api jar to the classpath at runtime.

**It is recommended to use the shade task/cli as documented below, as it will only include the necessary methods in your
jar.**

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
    id 'xyz.wagyourtail.jvmdowngrader' version '$jvmdgVersion'
}

// optionally you can change some globals, here are their default values:
jvmdg.downgradeTo = JavaVersion.VERSION_1_8
jvmdg.apiJar = [this.getClass().getResourceAsStream("jvmdg/java-api-${version}.jar").writeToFile("build/jvmdg/java-api-${version}.jar")]
jvmdg.quiet = false
jvmdg.debug = false
jvmdg.logLevel = "INFO"
jvmdg.ignoreWarningsIn = [].toSet()
jvmdg.debugSkipStubs = [].toSet()
jvmdg.debugDumpClasses = false
jvmdg.shadePath = {
    it.substringBefore(".").substringBeforeLast("-").replace(Regex("[.;\\[/]"), "-")
}
jvmdg.shadeInlining = true
jvmdg.multiReleaseOriginal = false
jvmdg.multiReleaseVersions = [].toSet()
```

You can see more of what these values mean
by [checking the docs on the flags classes](gradle-plugin/src/main/kotlin/xyz/wagyourtail/jvmdg/gradle/flags)

Once the `java` plugin is applied, this will register a default downgrade task for `jar` (or `shadowJar` if present)
called `downgradeJar` that will downgrade the output to java 8 by default.
as well as a `shadeDowngradedApi` to then insert the required classes for not having a runtime dependency on the api
jar. (Without `java`, no tasks are registered — the flags/spec/`dg` surfaces still work.)

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

If you are merging multiple downgraded jars, please merge from the downgradeJar tasks, and then shade on the resulting
mono-jar.
otherwise some API stubs may be missing, due to how shade only includes what is used.

Optionally, you can also depend on the shadeDowngradedApi task when running build.

```gradle
assemble.dependsOn shadeDowngradedApi
```

### Custom Tasks

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

### Downgrade FileCollection

```gradle
task downgradeFileCollection(type: xyz.wagyourtail.jvmdg.gradle.task.files.DowngradeFiles) {
    inputCollection = files("file1.jar", "file2.jar")
    downgradeTo = JavaVersion.VERSION_1_8 // default
    classpath = sourceSets.main.runtimeClasspath // default when java plugin is present, else empty
}

// or bind a source set directly — its output classes dirs become the
// inputs and its runtimeClasspath the member-resolution classpath:
task downgradeTestClasses(type: xyz.wagyourtail.jvmdg.gradle.task.files.DowngradeFiles) {
    sourceSet "test"
    downgradeTo = JavaVersion.VERSION_1_8
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

### Downgrade Configuration

To depend on more modern jars on a lower java version you can downgrade the configuration.

```gradle
configurations {
    downgrade
    implementation.extendsFrom downgrade
}

jvmdg.dg(configurations.downgrade) {
    downgradeTo = JavaVersion.VERSION_1_8 // default
}

dependencies {
    downgrade "newer.java:version:1.0"
}

```

#### Downgrading for shading/jij'ing dependencies

This is the expected usage of these functions.

`jvmdg.dg` has optional parameter, `shade: Boolean`, turn this to false if you plan on shadow'ing this configuration and
running `shadeDowngradedApi`, as otherwise there may be duplicate jvmdg-api classes.

### Scoped flag specs

For multi-target builds (or plugins building on jvmdg) the extension carries named
`FlagsSpec`s — standalone bundles of all the same flags, conventioned to the global
`jvmdg` values so explicit sets win:

```gradle
jvmdg.spec("test") {
    downgradeTo = JavaVersion.VERSION_11
    enabled = false // declared but inert — check this before using the spec
}

// bind a task to a spec's flags
task downgradeTest(type: xyz.wagyourtail.jvmdg.gradle.task.files.DowngradeFiles) {
    inputCollection = sourceSets.test.runtimeClasspath
    convention(jvmdg.specs["test"])
    onlyIf { jvmdg.specs["test"].enabled.get() }
}
```

Specs can also chain each other — `spec("b") { convention(spec("a")) }` —
and plugin authors can create their own standalone specs with
`objects.newInstance(FlagsSpec::class.java, "name")`.

Dependency transforms bind to a spec with the `dg(dep, spec)` overload — the
spec's flags replace the extension's for that configuration's downgrades:

```gradle
jvmdg.dg(configurations.downgrade, jvmdg.spec("java11") {
    downgradeTo = JavaVersion.VERSION_11
})
```
ie. `jvmdg.dg(configurations.downgrade, false)`

you may have to include whatever configuration you're downgrading in ShadowJar yourself.
I do not automatically make the configuration shadowed into your output.

### Embedding jvmdg in your own plugin

The plugin is deliberately consumable without its task surface: `apply` only
registers the shared flag defaults and the `jvmdg` extension, and the default
tasks only appear once `java` is applied. The pieces meant for embedders:

- **`jvmdg.specs` / `jvmdg.spec("name")`** — per-scope `FlagsSpec`s that
  convention the global extension; your own scopes (source sets, environments,
  variants) get `own set > spec > global > built-in` precedence for free.
- **`spec.enabled`** — specs can be declared but inert; gate bound tasks with
  `onlyIf { spec.enabled.get() }` (Gradle task types can't carry an `enabled`
  *property* — the name is already a plain boolean on `Task`).
- **`convention(spec)`** — binds any `DowngradeFlags` carrier (a task, a
  transform's params, another spec) to a spec: set values win, unset values
  keep their existing conventions. `enabled` flows when both ends are
  `EnabledFlag`s.
- **`toFlags()`** — converts any flags carrier to the engine's `cli.Flags`;
  engine entry points are `ClassDowngrader`, `PathDowngrader` (dirs),
  `ZipDowngrader` (jars) and `ApiShader` (stub shading).
- **`jvmdg.javaApi`** — a resolvable `jvmdgApi` configuration carrying the
  `jvmdowngrader-java-api` artifact at this plugin's version, for consumers
  that prefer lazy maven resolution over the bundled api jar extraction:
  `spec.apiJar.addAll(jvmdg.javaApi.map { it.files })`.
- **Compile-vs-emit is yours** — jvmdg only knows the *emit* level
  (`downgradeTo`); what java level you compile at, and how a user asks for a
  higher compile level, is the consuming plugin's schema (e.g. unimined's
  `source`/`target` pair).
- **Supported input majors are a release property** — downgrading is a pure
  bytecode transform (inputs are never `defineClass`'d; api stubs load
  through a downgrading classloader), so there is no "build JVM ≥ source"
  requirement. The ceiling is the `VersionProvider` coverage and bundled ASM
  in the jvmdg release itself — document yours accordingly.

#### Including newer dependencies on lower java version building

It is **strongly** not recommended due to the following but is still possible.
instead, you *should* just shade dependencies into your project and downgrade the combined output.
The dependencies may not be correctly represented in the pom with this method.

Since 2.1.0, `dg()` on an external module dependency registers a
component metadata rule that rewrites the module's
`org.gradle.jvm.version` to the downgrade target, so a consumer
resolving at that level passes variant compatibility against
Gradle Module Metadata that only publishes higher-jvm variants —
metadata sources and transitive resolution work as-is. File
dependencies carry no metadata and are unaffected.

On older versions, there may be issues with metadata breaking because
of how early gradle checks the java version. You can disable this by
setting `artifact` in the `metadataSources` function on repositories
to explicitly disable metadata, if you are lucky, you may be able to
include `mavenPom` as well, so trasitive dependencies can resolve,
otherwise **transitive dependencies will not be included**, and must
be included manually.

for example:

```gradle
repositories {
    mavenCentral {
        metadataSources {
            mavenPom() // may still break with this line on some dependencies
            artifact()
        }
    }
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
loader.addDelegate(new URL[]{new File("jarname.jar").toURI().toURL()});
```

## Testing

The `testing` module runs unit tests plus an end-to-end suite that downgrades a
compiled test jar on every supported target JDK. The version matrix is
configured in `gradle.properties`:

```properties
testTargetVersion=8     # lowest JDK the downgraded output is verified on
stubFromVersion=7       # lowest JDK java-api stubs cover
stubToVersion=27        # highest JDK java-api stubs cover (also the JDK tests run on)
```

Gradle auto-provisions each Azul JDK in the matrix via the foojay toolchain
resolver, so no manual JDK installs are needed:

```
./gradlew :testing:test          # unit + full JDK 8..27 downgrade matrix
```

The mirrored `commons-asm`/`commons-core` tests live in the separate
`wagyourtail/commons` repo — run `./gradlew :commons-asm:test` there.

Both `build.yml` and `build_snapshot.yml` run the full matrix in CI before
publishing. Test reports land in `testing/build/reports/tests/`.

### commons-asm mirrors

Several classes exist both here and in `wagyourtail/commons`
(`commons-asm`/`commons-core`/`commons-java7`) as diverged forks. When fixing
one side, port the fix to the counterpart:

| JvmDowngrader | commons |
|---|---|
| `jvmdg.compile.shade.ReferenceGraph` | `commons.asm.graph.ReferenceGraph` |
| `jvmdg.asm.ASMUtils` | `commons.asm.ASMUtils` |
| `jvmdg.asm.AnnotationUtils` | `commons.asm.AnnotationUtils` |
| `jvmdg.classloader.ResourceClassLoader` | `commons.classloader.ResourceClassLoader` |
| `jvmdg.util.Lazy` | `commons.lazy.Lazy` |
| `jvmdg.util.AsyncUtils` | `commons.java7.AsyncUtils` (commons-java7) |
| `jvmdg.collection.{Map,FlatMap}Enumeration` | `commons.enumerations.{Map,FlatMap}Enumeration` |

See `AGENTS.md` for the full mapping and verification commands.

## License

JvmDowngrader is licensed under the [GNU Lesser General Public License v2.1](LICENSE.md) or
(at your option) any later version.

### LGPLv2.1 License Concerns

Some people think that shading would mean they're bound by the stricter GPL license due to the inclusion of
jvmdowngrader's java class files. I don't believe this to be the case.

For the purpose of Licensing, the produced jar from this task, or the downgrading task, should be considered a
"Combined Work", as it contains the original code from the input jar and the shaded code from jvmdowngrader's api.

And this does, usually, mean that you shouldn't need to use the *exact* same license.
Running this tool, should be a thing the end-user is capable of doing, thus section 6.a should be satisfied as long as
your project provides the unshaded/undowngraded jar as well, or alternatively provides source code to build said jar,
or the post-shaded jar.


### inspired by

https://github.com/Chocohead/Not-So-New and https://github.com/luontola/retrolambda
