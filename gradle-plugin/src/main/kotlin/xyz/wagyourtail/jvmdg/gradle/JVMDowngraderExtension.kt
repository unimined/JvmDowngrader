package xyz.wagyourtail.jvmdg.gradle

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.gradle.flags.DowngradeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.FlagsSpec
import xyz.wagyourtail.jvmdg.gradle.flags.ShadeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.convention
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ExtractApiJar
import xyz.wagyourtail.jvmdg.gradle.task.PrintConfig
import xyz.wagyourtail.jvmdg.gradle.task.ShadeJar
import xyz.wagyourtail.jvmdg.gradle.transform.DowngradeTransform
import xyz.wagyourtail.jvmdg.gradle.transform.ShadeTransform
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import javax.inject.Inject

val Project.jvmdg
    get() = project.extensions.getByType(JVMDowngraderExtension::class.java)

/**
 * Global jvmdg defaults — implements [ShadeFlags] directly so the flag
 * properties are generated members of the extension itself. Tasks and
 * transform parameters `convention(this)`, so `jvmdg { ... }` writes
 * land on the same provider chain every consumer reads.
 */
abstract class JVMDowngraderExtension @Inject constructor(@get:Internal val project: Project): ShadeFlags {
    @get:Internal
    val version by FinalizeOnRead(JVMDowngraderPlugin::class.java.`package`.implementationVersion ?: "0.7.0")

    /**
     * The default `downgradeJar` task — registered only once the `java`
     * plugin is present, since it wraps `shadowJar`/`jar`. Embedders
     * that never apply `java` get no task noise from this plugin.
     */
    @get:Internal
    var defaultTask: TaskProvider<DowngradeJar>? = null
        private set

    /**
     * The default `shadeDowngradedApi` task — registered only once the
     * `java` plugin is present.
     */
    @get:Internal
    var defaultShadeTask: TaskProvider<ShadeJar>? = null
        private set

    /**
     * Materializes the bundled `java-api.jar` resource into
     * `build/jvmdg-api/` — the [apiJar] convention reads this task's
     * output, so the extraction is a tracked task rather than a
     * side-effecting provider (which used to self-invalidate the
     * configuration-cache entry on the store that created the file).
     */
    @get:Internal
    val extractApiTask = project.tasks.register("jvmdgExtractApi", ExtractApiJar::class.java) { task ->
        task.apiVersion.set(version)
        task.refreshDependencies.set(project.gradle.startParameter.isRefreshDependencies)
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

    /** Modules already rewritten by [DowngradeTargetJvmRule] — dedupes
     *  `withModule` across `dg()` calls sharing a dep. */
    @get:Internal
    private val jvmRuleModules = mutableSetOf<String>()

    /** Modules whose [DowngradeTargetJvmRule] registration waits on
     *  the DSL settling — rule params are eager values, so the target
     *  must be read after `jvmdg { downgradeTo.set(...) }` has run. */
    @get:Internal
    private val pendingModuleRules = linkedMapOf<String, DowngradeFlags>()

    /** Post-DSL flag — a `dg()` call from a later `afterEvaluate`
     *  registers its rules immediately instead of queueing. */
    @get:Internal
    private var dslEvaluated = false

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

    /**
     * Groovy/Java-friendly [Action] overload of [spec].
     * @since 2.1.0
     */
    fun spec(name: String, action: Action<FlagsSpec>): FlagsSpec =
        specs.maybeCreate(name).also { action.execute(it) }

    /**
     * Groovy-friendly [Closure] overload of [spec] — the closure's
     * delegate is the spec (`DELEGATE_FIRST`), so bare flag names
     * resolve.
     * @since 2.1.0
     */
    fun spec(name: String, @DelegatesTo(FlagsSpec::class) action: Closure<*>): FlagsSpec =
        specs.maybeCreate(name).also { action.delegatingTo(it) }

    @get:Internal
    val printConfigTask = project.tasks.register("jvmdgPrintConfig", PrintConfig::class.java)

    init {
        downgradeTo.convention(JavaVersion.VERSION_1_8).finalizeValueOnRead()
        apiJar.convention(
            extractApiTask.flatMap { it.outputJar }.map { setOf(it.asFile) }
        ).finalizeValueOnRead()
        logAnsiColors.convention(true).finalizeValueOnRead()
        logLevel.convention("INFO").finalizeValueOnRead()
        ignoreWarningsIn.convention(emptySet()).finalizeValueOnRead()
        debugSkipStub.convention(emptySet()).finalizeValueOnRead()
        debugSkipStubs.convention(emptySet()).finalizeValueOnRead()
        debugDumpClasses.convention(false).finalizeValueOnRead()
        debugNoSynthetic.convention(false).finalizeValueOnRead()
        noIndy.convention(false).finalizeValueOnRead()
        allowPreview.convention(false).finalizeValueOnRead()
        shadeInlining.convention(true).finalizeValueOnRead()
        shadeReflection.convention("usedOwners").finalizeValueOnRead()
        shadeKeepReflection.convention(emptyList()).finalizeValueOnRead()
        multiReleaseOriginal.convention(false).finalizeValueOnRead()
        multiReleaseVersions.convention(emptySet()).finalizeValueOnRead()
        allowMaven.convention(false).finalizeValueOnRead()
        debugNoFuse.convention(false).finalizeValueOnRead()
        debugNoStubIdx.convention(false).finalizeValueOnRead()
        ignoreEmbeddedRules.convention(false).finalizeValueOnRead()
        keepEmbeddedRules.convention(false).finalizeValueOnRead()

        // component-rule params are eager values captured at
        // registration — defer the actual withModule calls until the
        // DSL has settled so a `jvmdg {}` block appearing after `dg()`
        // still supplies the effective target
        project.afterEvaluate {
            dslEvaluated = true
            pendingModuleRules.forEach(::registerJvmRule)
            pendingModuleRules.clear()
        }

        // only register the default tasks where they can work — they wrap
        // `jar`/`shadowJar` outputs, so without the java plugin there is
        // nothing to bind; deferring registration also keeps the task list
        // clean for embedders that only want the flags/dg surfaces
        project.pluginManager.withPlugin("java") {
            val jarTask = project.tasks.named("jar", Jar::class.java)
            defaultTask = project.tasks.register("downgradeJar", DowngradeJar::class.java)
            defaultTask?.configure { task ->
                task.inputFile.convention(jarTask.flatMap { it.archiveFile })
                task.archiveClassifier.set("downgraded")
            }
            // shadow-family plugins replace `jar`'s packaging — prefer a
            // `shadowJar` task's output whenever one appears; the explicit
            // set beats the jar convention above
            project.tasks.matching { it is Jar && it.name == "shadowJar" }.configureEach { shadow ->
                defaultTask?.configure { task ->
                    task.inputFile.set((shadow as Jar).archiveFile)
                }
            }
            defaultShadeTask = project.tasks.register("shadeDowngradedApi", ShadeJar::class.java)
            defaultShadeTask?.configure { task ->
                task.inputFile.set(defaultTask!!.flatMap { it.archiveFile })
                task.archiveClassifier.set("downgraded-shaded")
            }
        }
    }

    private fun registerJvmRule(module: String, flags: DowngradeFlags) {
        project.dependencies.components.withModule(
            module,
            DowngradeTargetJvmRule::class.java
        ) { r ->
            r.params(flags.downgradeTo.get().majorVersion.toInt())
        }
    }

    /**
     * A resolvable configuration carrying the `jvmdowngrader-java-api`
     * artifact, for consumers that prefer lazy maven resolution over
     * the bundled api jar ([apiJar]'s resource extraction). Wire it
     * into flags with `apiJar.addAll(javaApi.map { it.files })` — or
     * set a spec's `apiJar` the same way for a scoped api.
     *
     * @since 2.1.0
     */
    @get:Internal
    val javaApi: NamedDomainObjectProvider<Configuration> =
        project.configurations.register("jvmdgApi") {
            it.isCanBeConsumed = false
            it.isCanBeResolved = true
            it.isVisible = false
            it.description = "JvmDowngrader java-api stub jar"
            it.defaultDependencies { deps ->
                deps.add(project.dependencies.create("xyz.wagyourtail.jvmdowngrader:jvmdowngrader-java-api:$version"))
            }
        }

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

    /**
     * Groovy/Java-friendly [Action] overload of [dg].
     * @since 2.1.0
     */
    fun dg(dep: Configuration, shade: Boolean, config: Action<DowngradeFlags>) =
        dg(dep, shade) { config.execute(this) }

    /**
     * Groovy-friendly [Closure] overloads of [dg] — the closure's
     * delegate is the transform flags (`DELEGATE_FIRST`), so bare
     * flag names resolve.
     * @since 2.1.0
     */
    fun dg(dep: Configuration, config: Closure<*>) = dg(dep, true, config)

    /**
     * @since 2.1.0
     */
    fun dg(dep: Configuration, shade: Boolean, config: Closure<*>) =
        dg(dep, shade) { config.delegatingTo(this) }

    private fun <T> Closure<*>.delegatingTo(target: T) {
        delegate = target
        resolveStrategy = Closure.DELEGATE_FIRST
        call(target)
    }

    /**
     * Downgrade (and optionally api-shade) the jars resolving through
     * [dep] — registers the artifact transforms and, for each external
     * module dependency declared on the configuration, a
     * [DowngradeTargetJvmRule] rewriting `org.gradle.jvm.version` to
     * `downgradeTo` so Gradle Module Metadata published only for
     * higher jvm versions still resolves (pre-2.1.0 this required
     * disabling `metadataSources`).
     */
    @JvmOverloads
    fun dg(dep: Configuration, shade: Boolean = true, config: DowngradeFlags.() -> Unit = {}) {
        val artifactType = Attribute.of("artifactType", String::class.java)
        val downgradeAttr = Attribute.of("jvmdg.dg.${dep.name}", Boolean::class.javaObjectType)
        val shadeAttr = Attribute.of("jvmdg.shade.${dep.name}", Boolean::class.javaObjectType)
        var ruleFlags: DowngradeFlags? = null
        // first registration wins for a module — dg() with a differing
        // target on the same dep keeps the earlier rewrite

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
                    ruleFlags = it
                }
            }
        }

        // rewriting the module's org.gradle.jvm.version to the emit
        // level lets variant selection succeed against GMM-published
        // deps that only offer higher-jvm variants — without this a
        // consumer targeting `downgradeTo` fails compatibility checks
        // before the transform can apply. `allDependencies.all` fires
        // for declared deps now and ones added later; file deps carry
        // no metadata and project deps produce their own
        val flags = ruleFlags!!
        dep.allDependencies.all {
            val g = (it as? ExternalModuleDependency)?.group
                ?: return@all
            val module = "$g:${it.name}"
            if (jvmRuleModules.add(module)) {
                if (dslEvaluated) {
                    registerJvmRule(module, flags)
                } else {
                    pendingModuleRules[module] = flags
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
