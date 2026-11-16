package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.JavaVersion
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import xyz.wagyourtail.jvmdg.logging.Logger
import java.io.File

interface DowngradeFlags {

    /**
     * jvmdg.mf config files — applied in order below explicit DSL values.
     * Editing a file listed here invalidates task/transform caches.
     * @since 3.0.0
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    val config: ConfigurableFileCollection

    /**
     * sets the target class version to downgrade to,
     * default is [JavaVersion.VERSION_1_8]
     */
    @get:Input
    @get:Optional
    val downgradeTo: Property<JavaVersion>

    /**
     * sets the api jar to use for downgrading
     * default is null
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    val apiJar: ListProperty<File>

    /**
     * sets the log level to [Logger.Level.FATAL]
     * @deprecated use [logLevel], if this is true, it will override [logLevel] to [Logger.Level.FATAL]
     */
    @get:Input
    @get:Optional
    @get:Deprecated(message = "use logLevel", replaceWith = ReplaceWith("logLevel = LogLevel.FATAL"))
    val quiet: Property<Boolean>

    /**
     * sets if the logger should use ansi colors for the console to look pretty
     */
    @get:Input
    @get:Optional
    val logAnsiColors: Property<Boolean>


    /**
     * sets the log level, default is [Logger.Level.INFO] as a string
     */
    @get:Input
    @get:Optional
    val logLevel: Property<String>

    /**
     * sets if any classes should be set to ignore missing class/member warnings
     * this will prevent [xyz.wagyourtail.jvmdg.version.VersionProvider.printWarnings] for
     * any classes in this set
     *
     * This set also allows for packages to be ignored, by ending with a `*` or `**` to ignore all sub-packages
     * @since 0.9.0
     */
    @get:Input
    @get:Optional
    val ignoreWarningsIn: ListProperty<String>

    /**
     * sets if the logger should print debug messages
     * @deprecated use [logLevel], if this is true, it will override [logLevel] to [Logger.Level.DEBUG]
     */
    @get:Input
    @get:Optional
    @get:Deprecated(message = "use logLevel", replaceWith = ReplaceWith("logLevel = LogLevel.DEBUG"))
    val debug: Property<Boolean>

    /**
     * this skips applying stubs for the specified input class version, this will still apply the
     * [xyz.wagyourtail.jvmdg.version.VersionProvider.otherTransforms]
     * such as `INVOKE_INTERFACE` -> `INVOKE_SPECIAL` for private interface methods in java 9 -> 8
     */
    @get:Input
    @get:Optional
    val debugSkipStubs: SetProperty<JavaVersion>

    /**
     * sets if classes should be dumped to the [xyz.wagyourtail.jvmdg.Constants.DEBUG_DIR] directory
     * @since 0.9.0
     */
    @get:Input
    @get:Optional
    val debugDumpClasses: Property<Boolean>

    /**
     * set generated methods to not be synthetic
     * @since 1.2.3
     */
    @get:Input
    @get:Optional
    val debugNoSynthetic: Property<Boolean>

    /**
     * sets if the original versions of classes should be retained in the output jar as a Multi-Release
     * @since 1.0.0
     */
    @get:Input
    @get:Optional
    val multiReleaseOriginal: Property<Boolean>

    /**
     * sets if semi-downgraded versions of classes should be retained in the output jar as a Multi-Release
     * @since 1.0.0
     */
    @get:Input
    @get:Optional
    val multiReleaseVersions: SetProperty<JavaVersion>

    /**
     * sets if should downgrade classess from the multi-release folder, instead of selecting the normal one
     * @since 1.2.1
     */
    @get:Input
    @get:Optional
    val downgradeFromMultiReleases: Property<Boolean>

    /**
     * emit no invokedynamic/ConstantDynamic in the output — every bootstrap
     * site desugars to ordinary invokes/fields at transform time
     * @since 3.1.0
     */
    @get:Input
    @get:Optional
    val noIndy: Property<Boolean>


    /**
     * accept preview-compiled input class files (javac --enable-preview sets
     * the class-file minor version to 0xFFFF) and activate PREVIEW-condition
     * stubs — without it a preview input fails naming the class
     * @since 3.1.0
     */
    @get:Input
    @get:Optional
    val allowPreview: Property<Boolean>

    /**
     * this skips applying specific stub classes/methods, for example to disable the JEP 400, you would put
     * {@link xyz.wagyourtail.jvmdg.j18.stub.java_base.J_L_System} in.
     *
     * To add to this list, add either classes with `Lpackage/to/ClassName;` or methods/fields like `Lpackage/to/ClassName;methodName;()V`.
     * where each part is seperated by a `;` (with the first one pulling double-duty as the class descriptor terminator)
     *
     * @since 1.3.0
     */
    @get:Input
    @get:Optional
    val debugSkipStub: SetProperty<String>

    /**
     * this skips applying specific jeps, for example to disable the JEP 400, you would put `400` in.
     *
     * @since 1.4.0
     */
    @get:Input
    @get:Optional
    val debugSkipJeps: SetProperty<Int>

    /**
     * which stub kinds callsites may use — `full` applies all stubs,
     * `noBehavior` skips BEHAVIOR stubs (never wrap an API the target
     * already has), `mechanical` keeps LANGUAGE desugars only and reports
     * MISSING_API hits per [onMissingApi].
     * No convention is set so an unset property defers to config files.
     *
     * @since 3.0.0
     */
    @get:Input
    @get:Optional
    val stubPolicy: Property<String>

    /**
     * what `mechanical` does with a MISSING_API callsite — `error` aborts
     * the transform naming the site + required Java version; `warn` reports
     * it and keeps building.
     *
     * @since 3.0.0
     */
    @get:Input
    @get:Optional
    val onMissingApi: Property<String>

    /**
     * stub members always emitted — an explicit member-level statement
     * that beats [debugSkipStub]/[debugSkipJeps] scoped excludes and
     * [stubPolicy] drops (the `mechanical` escape hatch). Members are
     * `Lpackage/to/ClassName;methodName;()V`, same form as [debugSkipStub].
     *
     * @since 3.0.0
     */
    @get:Input
    @get:Optional
    val includeStub: SetProperty<String>

    /**
     * allows maven lookup for auto-resolving [apiJar]
     * @since 3.0.0
     */
    @get:Input
    @get:Optional
    val allowMaven: Property<Boolean>

    /**
     * disables [xyz.wagyourtail.jvmdg.pipeline.FusibleTransform] fusion —
     * sequential path is the correctness oracle
     * @since 3.0.0
     */
    @get:Input
    @get:Optional
    val debugNoFuse: Property<Boolean>

    /**
     * forces the reflective stub scan even when the api jar ships a stubs.idx
     * @since 3.0.0
     */
    @get:Input
    @get:Optional
    val debugNoStubIdx: Property<Boolean>

    /**
     * do not apply META-INF/jvmdg/ `.mf` rules embedded in input jars
     * @since 3.0.0
     */
    @get:Input
    @get:Optional
    val ignoreEmbeddedRules: Property<Boolean>

    /**
     * copy META-INF/jvmdg/ `.mf` rule files into the output instead of stripping
     * @since 3.0.0
     */
    @get:Input
    @get:Optional
    val keepEmbeddedRules: Property<Boolean>

    /**
     * prefix used when shading api classes into the output — the task's
     * shadePath property overrides it
     * @since 3.0.0
     */
    @get:Input
    @get:Optional
    val shadePrefix: Property<String>

}
