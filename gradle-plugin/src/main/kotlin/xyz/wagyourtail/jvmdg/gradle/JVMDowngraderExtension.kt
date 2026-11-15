package xyz.wagyourtail.jvmdg.gradle

import groovy.lang.Closure
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.Internal
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader
import xyz.wagyourtail.jvmdg.gradle.flags.DowngradeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.FlagsSpec
import xyz.wagyourtail.jvmdg.gradle.flags.ShadeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.convention
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeJar
import xyz.wagyourtail.jvmdg.gradle.transform.DowngradeTransform
import xyz.wagyourtail.jvmdg.gradle.transform.ShadeTransform
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.defaultedMapOf
import java.io.File
import javax.inject.Inject

val Project.jvmdg
    get() = project.extensions.getByType(JVMDowngraderExtension::class.java)

private val API_EXTRACTION_LOCK = Any()

abstract class JVMDowngraderExtension @Inject constructor(@get:Internal val project: Project): ShadeFlags by project.gradle.sharedServices.registrations.getByName("${project.path}:jvmdgDefaultFlags").parameters as ShadeFlags {
    @get:Internal
    val version by FinalizeOnRead(JVMDowngraderPlugin::class.java.`package`.implementationVersion ?: "0.7.0")

    @get:Internal
    val defaultTask = project.tasks.register("downgradeJar", DowngradeJar::class.java).apply {
        configure {
            val jar = (project.tasks.findByName("shadowJar") ?: project.tasks.getByName("jar")) as Jar
            it.inputFile.set(jar.archiveFile)
            it.archiveClassifier.set("downgraded")
        }
    }

    @get:Internal
    val defaultShadeTask = project.tasks.register("shadeDowngradedApi", ShadeJar::class.java).apply {
        configure {
            it.inputFile.set(defaultTask.get().archiveFile)
            it.archiveClassifier.set("downgraded-shaded")
        }
    }

    /**
     * Named [FlagsSpec]s for multi-target/multi-scope builds —
     * `jvmdg.spec("test") { downgradeTo.set(JavaVersion.VERSION_11) }`
     * or `jvmdg.specs.create("test") { }`. Every spec conventions this
     * extension's flag values, so global settings flow through and
     * per-spec sets win. Specs do nothing on their own — bind them to
     * a task with `convention(jvmdg.specs["test"])` inside the task's
     * configuration, or read them from another plugin's wiring.
     *
     * @since 2.1.0
     */
    @get:Internal
    val specs: NamedDomainObjectContainer<FlagsSpec> = project.objects
        .domainObjectContainer(FlagsSpec::class.java)
        .also { container ->
            container.configureEach { spec ->
                spec.convention(this@JVMDowngraderExtension)
            }
        }

    /**
     * Get or create a named [FlagsSpec] in [specs].
     * @since 2.1.0
     */
    fun spec(name: String): FlagsSpec = specs.maybeCreate(name)

    /**
     * Configure a named [FlagsSpec] in [specs], creating it if needed.
     * @since 2.1.0
     */
    fun spec(name: String, action: FlagsSpec.() -> Unit): FlagsSpec =
        specs.maybeCreate(name).apply(action)

    init {
        downgradeTo.convention(JavaVersion.VERSION_1_8).finalizeValueOnRead()
        // the provider captures only serializable values (project dir,
        // version string, refresh flag) — capturing `project` here
        // poisons configuration-cache serialization when a task input
        // chains this convention
        val apiJarDir = project.layout.projectDirectory.asFile
        val implVersion = version
        val refreshDependencies = project.gradle.startParameter.isRefreshDependencies
        apiJar.convention(project.provider {
            val apiJar = apiJarDir.resolve(".gradle/jvmdg/java-api-${implVersion}.jar")
            if (!apiJar.exists() || refreshDependencies) {
                synchronized(API_EXTRACTION_LOCK) {
                    if (!apiJar.exists() || refreshDependencies) {
                        apiJar.parentFile.mkdirs()
                        JVMDowngraderExtension::class.java.getResourceAsStream("/META-INF/lib/java-api.jar")
                            .use { stream ->
                                if (stream == null) throw IllegalStateException("java-api.jar not found in resources")
                                apiJar.outputStream().use { os ->
                                    stream.copyTo(os)
                                }
                            }
                    }
                }
            }
            setOf(apiJar)
        }).finalizeValueOnRead()
        quiet.convention(false).finalizeValueOnRead()
        logAnsiColors.convention(true).finalizeValueOnRead()
        logLevel.convention("INFO").finalizeValueOnRead()
        ignoreWarningsIn.convention(emptySet()).finalizeValueOnRead()
        debug.convention(false).finalizeValueOnRead()
        debugSkipStub.convention(emptySet()).finalizeValueOnRead()
        debugSkipStubs.convention(emptySet()).finalizeValueOnRead()
        debugDumpClasses.convention(false).finalizeValueOnRead()
        debugNoSynthetic.convention(false).finalizeValueOnRead()
        shadeInlining.convention(true).finalizeValueOnRead()
        multiReleaseOriginal.convention(false).finalizeValueOnRead()
        multiReleaseVersions.convention(emptySet()).finalizeValueOnRead()
    }

    @get:Internal
    @get:Deprecated("not compatibile with configuration cache for use in tasks")
    internal val downgradedApis = defaultedMapOf<JavaVersion, Set<File>> { version ->
        val jars = mutableSetOf<File>()
        for (path in apiJar.get()) {
            val downgraded = path.resolveSibling(path.nameWithoutExtension + "-downgraded-${version}.jar")
            if (!downgraded.exists() || project.gradle.startParameter.isRefreshDependencies) {
                ClassDowngrader.downgradeTo(this.toFlags()).use {
                    ZipDowngrader.downgradeZip(it, path.toPath(), emptySet(), downgraded.toPath())
                }
            }
            jars.add(downgraded)
        }
        jars
    }

    @Deprecated("not compatibile with configuration cache for use in tasks")
    fun getDowngradedApi(version: JavaVersion): Set<File> = downgradedApis[version]

    /**
     * Like [dg] but binds the transforms to a [ShadeFlags] source —
     * typically a named [FlagsSpec] from [specs]:
     * `jvmdg.dg(configurations.test, jvmdg.specs["test"])`. The spec's
     * flag values replace this extension's for the transform (the
     * spec itself still conventions the extension for unset flags).
     *
     * @since 2.1.0
     */
    @JvmOverloads
    fun dg(dep: Configuration, spec: ShadeFlags, shade: Boolean = true) {
        dg(dep, shade) { convention(spec) }
    }

    @JvmOverloads
    fun dg(dep: Configuration, shade: Boolean = true, config: DowngradeFlags.() -> Unit = {}) {
        val artifactType = Attribute.of("artifactType", String::class.java)
        val downgradeAttr = Attribute.of("jvmdg.dg.${dep.name}", Boolean::class.javaObjectType)
        val shadeAttr = Attribute.of("jvmdg.shade.${dep.name}", Boolean::class.javaObjectType)
        val javaVersionAttr = Attribute.of("org.gradle.jvm.version", Int::class.javaObjectType)

        lateinit var javaVersion: JavaVersion

        project.dependencies.apply {
            attributesSchema {
                it.attribute(downgradeAttr)
            }
            artifactTypes.getByName("jar") {
                it.attributes.attribute(downgradeAttr, false)
            }
            registerTransform(DowngradeTransform::class.java) { spec ->
                spec.from.attribute(artifactType, "jar").attribute(downgradeAttr, false).attribute(shadeAttr, false)
                spec.to.attribute(artifactType, "jar").attribute(downgradeAttr, true).attribute(shadeAttr, false)

                spec.parameters {
                    it.convention(this@JVMDowngraderExtension)
                    config(it)
                    javaVersion = it.downgradeTo.get()
                }
            }
        }

        if (shade) {
            project.dependencies.apply {
                attributesSchema {
                    it.attribute(shadeAttr)
                }
                artifactTypes.getByName("jar") {
                    it.attributes.attribute(shadeAttr, false)
                }
                registerTransform(ShadeTransform::class.java) { spec ->
                    spec.from.attribute(artifactType, "jar").attribute(shadeAttr, false).attribute(downgradeAttr, true)
                    spec.to.attribute(artifactType, "jar").attribute(shadeAttr, true).attribute(downgradeAttr, true)

                    spec.parameters {
                        it.convention(this@JVMDowngraderExtension)
                        config(it)
                    }
                }
            }
        }

        dep.attributes {
            it.attribute(downgradeAttr, true)
            if (shade) {
                it.attribute(shadeAttr, true)
            }
        }
    }

}
