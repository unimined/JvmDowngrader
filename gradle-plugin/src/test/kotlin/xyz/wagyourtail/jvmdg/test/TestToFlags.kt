package xyz.wagyourtail.jvmdg.test

import org.gradle.api.JavaVersion
import org.gradle.testfixtures.ProjectBuilder
import xyz.wagyourtail.jvmdg.config.FlagBindings
import xyz.wagyourtail.jvmdg.config.OnMissingApi
import xyz.wagyourtail.jvmdg.config.StubPolicy
import xyz.wagyourtail.jvmdg.gradle.flags.DowngradeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.ShadeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags
import xyz.wagyourtail.jvmdg.util.toOpcode
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The DSL→[xyz.wagyourtail.jvmdg.cli.Flags] application is generated from the
 * bound-flag table — these tests pin that contract: every bound `gradle` name
 * resolves a property, values convert through the flag's own parser, and
 * unset properties defer to config files.
 */
class TestToFlags {

    private val project = ProjectBuilder.builder().build()

    private lateinit var mf: File

    @BeforeTest
    fun setup() {
        mf = File(project.projectDir, "test.mf")
        mf.delete()
    }

    private fun flags() = project.objects.newInstance(DowngradeFlags::class.java)

    @Test
    fun boundGradleNamesAllResolve() {
        // smoke over the drift guard: every bound gradle name must resolve a
        // property on this object or another flags interface
        flags().toFlags()
    }

    @Test
    fun dslValuesApplyThroughTheBoundTable() {
        val df = flags()
        df.stubPolicy.set("mechanical")
        df.onMissingApi.set("warn")
        df.includeStub.set(setOf("Ljava/lang/String;strip;()Ljava/lang/String;"))
        df.debugSkipStub.set(setOf("Ljava/lang/System;getProperty;(Ljava/lang/String;)Ljava/lang/String;"))
        df.debugSkipJeps.set(setOf(400))
        df.downgradeTo.set(JavaVersion.VERSION_11)
        df.logLevel.set("warn")
        df.shadePrefix.set("com/example/jvmdg")
        df.apiJar.set(listOf(File("api.jar"), File("api2.jar")))
        val f = df.toFlags()
        assertEquals(StubPolicy.MECHANICAL, f.stubPolicy)
        assertEquals(OnMissingApi.WARN, f.onMissingApi)
        assertEquals(
            setOf(FullyQualifiedMemberNameAndDesc.of("Ljava/lang/String;strip;()Ljava/lang/String;")),
            f.includeStub)
        assertEquals(
            setOf(FullyQualifiedMemberNameAndDesc.of("Ljava/lang/System;getProperty;(Ljava/lang/String;)Ljava/lang/String;")),
            f.debugSkipStub)
        assertEquals(setOf(400), f.debugSkipJeps)
        assertEquals(JavaVersion.VERSION_11.toOpcode(), f.classVersion)
        assertEquals(xyz.wagyourtail.jvmdg.logging.Logger.Level.WARN, f.logLevel)
        assertEquals("com/example/jvmdg", f.shadePrefix)
        assertEquals(listOf(File("api.jar"), File("api2.jar")), f.api)
    }

    @Test
    fun versionSetPropertiesConvertJavaVersionElements() {
        val df = flags()
        df.debugSkipStubs.set(setOf(JavaVersion.VERSION_11, JavaVersion.VERSION_17))
        val f = df.toFlags()
        assertEquals(setOf(55, 61), f.debugSkipStubs)
    }

    @Test
    fun mapFlagsAccumulatePerElement() {
        val df = flags()
        df.ignoreWarningsIn.set(listOf("com/example/**", "com/other/Exact"))
        val f = df.toFlags()
        assertEquals(
            xyz.wagyourtail.jvmdg.cli.Flags.WildcardType.DOUBLE,
            f.ignoreWarningsIn["com/example/"])
        assertEquals(
            xyz.wagyourtail.jvmdg.cli.Flags.WildcardType.NONE,
            f.ignoreWarningsIn["com/other/Exact"])
    }

    @Test
    fun unsetPropertyDefersToConfigThenDslWins() {
        mf.writeText("Jvmdg-Config-Version: 1\nStub-Policy: mechanical\n")
        val df = flags()
        df.config.from(mf)
        assertEquals(StubPolicy.MECHANICAL, df.toFlags().stubPolicy,
            "config-file value applies when the DSL property is unset")
        df.stubPolicy.set("full")
        assertEquals(StubPolicy.FULL, df.toFlags().stubPolicy,
            "an explicit DSL value beats the config file")
    }

    @Test
    fun shadeFlagsSubtypePropertiesApply() {
        val df = project.objects.newInstance(ShadeFlags::class.java)
        df.shadeInlining.set(false)
        assertEquals(false, df.toFlags().shadeInlining)
        // and the shared props still apply through the same path
        df.stubPolicy.set("noBehavior")
        assertEquals(StubPolicy.NO_BEHAVIOR, df.toFlags().stubPolicy)
    }

    @Test
    fun everyBoundGradleNameHasAPropertyOnSomeInterface() {
        // the drift guard, asserted directly rather than via a throw: each
        // bound gradle name must have a get<Name>() on DowngradeFlags or
        // ShadeFlags — adding a bound flag without the property fails here
        for (b in FlagBindings.ALL) {
            val name = b.metas.firstOrNull { it.gradle.isNotEmpty() }?.gradle ?: continue
            val getter = "get" + name.replaceFirstChar { it.uppercase() }
            assertTrue(
                DowngradeFlags::class.java.methods.any { it.name == getter } ||
                    ShadeFlags::class.java.methods.any { it.name == getter },
                "bound flag '${b.field.name}' names Gradle property '$name' but neither " +
                    "DowngradeFlags nor ShadeFlags declares 'val $name'")
        }
    }

}
