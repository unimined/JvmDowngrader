package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.*
import xyz.wagyourtail.jvmdg.logging.Logger
import java.io.File

interface DowngradeFlags: TransformParameters, BuildServiceParameters {

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

}
