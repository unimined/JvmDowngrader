package xyz.wagyourtail.jvmdg.test

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderPlugin
import xyz.wagyourtail.jvmdg.gradle.flags.convention
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wiring tests for the flag-spec schema — the `jvmdg.specs`
 * container, convention precedence, `dg(dep, spec)` attribute
 * wiring, `enabled` flow, and the java-plugin-gated default tasks.
 */
class SpecWiringTest {

    private fun project(java: Boolean = true): Project =
        ProjectBuilder.builder().build().also {
            if (java) it.pluginManager.apply("java")
            it.pluginManager.apply(JVMDowngraderPlugin::class.java)
        }

    private fun jvmdg(project: Project): JVMDowngraderExtension =
        project.extensions.getByType(JVMDowngraderExtension::class.java)

    @Test
    fun `spec conventions the extension's flags`() {
        val ext = jvmdg(project())
        val spec = ext.spec("s")
        assertEquals(JavaVersion.VERSION_1_8, spec.downgradeTo.get())
        assertEquals("INFO", spec.logLevel.get())
    }

    @Test
    fun `spec's own sets win over the extension`() {
        val ext = jvmdg(project())
        ext.downgradeTo.set(JavaVersion.VERSION_17)
        val spec = ext.spec("s") {
            downgradeTo.set(JavaVersion.VERSION_11)
        }
        assertEquals(JavaVersion.VERSION_11, spec.downgradeTo.get())
    }

    @Test
    fun `enabled flows through spec chaining`() {
        val ext = jvmdg(project())
        val parent = ext.spec("parent") {
            enabled.set(false)
        }
        val child = ext.spec("child")
        child.convention(parent)
        assertFalse(child.enabled.get())
        // an explicit set still wins over the chain
        val grandchild = ext.spec("grandchild")
        grandchild.convention(child)
        grandchild.enabled.set(true)
        assertTrue(grandchild.enabled.get())
    }

    @Test
    fun `dg(dep, spec) sets the configuration attributes`() {
        val p = project()
        val ext = jvmdg(p)
        val conf = p.configurations.create("custom")
        ext.dg(conf, ext.spec("s"))
        assertEquals(
            true,
            conf.attributes.getAttribute(Attribute.of("jvmdg.dg.custom", Boolean::class.javaObjectType))
        )
        assertEquals(
            true,
            conf.attributes.getAttribute(Attribute.of("jvmdg.shade.custom", Boolean::class.javaObjectType))
        )
    }

    @Test
    fun `dg(dep, spec, shade=false) registers only the downgrade attribute`() {
        val p = project()
        val ext = jvmdg(p)
        val conf = p.configurations.create("noshade")
        ext.dg(conf, ext.spec("s"), shade = false)
        assertEquals(
            true,
            conf.attributes.getAttribute(Attribute.of("jvmdg.dg.noshade", Boolean::class.javaObjectType))
        )
        assertNull(
            conf.attributes.getAttribute(Attribute.of("jvmdg.shade.noshade", Boolean::class.javaObjectType))
        )
    }

    @Test
    fun `default tasks exist only when the java plugin is applied`() {
        val withJava = jvmdg(project(java = true))
        assertNotNull(withJava.defaultTask)
        assertNotNull(withJava.project.tasks.findByName("downgradeJar"))
        assertNotNull(withJava.project.tasks.findByName("shadeDowngradedApi"))

        val withoutJava = project(java = false)
        assertNull(withoutJava.tasks.findByName("downgradeJar"))
        assertNull(withoutJava.tasks.findByName("shadeDowngradedApi"))
        assertNull(jvmdg(withoutJava).defaultTask)
        assertNull(jvmdg(withoutJava).defaultShadeTask)
    }

    @Test
    fun `javaApi configuration is registered`() {
        val ext = jvmdg(project())
        val conf = ext.javaApi.get()
        assertFalse(conf.isCanBeConsumed)
        assertTrue(conf.isCanBeResolved)
        // the jvmdowngrader-java-api dep is added via defaultDependencies,
        // which only fires at resolution time
    }

}
