package xyz.wagyourtail.jvmdg.test

import kotlin.test.Test
import kotlin.test.assertEquals
import xyz.wagyourtail.jvmdg.util.defaultShadePrefix

class TestUtil {

    @Test
    fun shadePrefixDerivesProjectName() {
        assertEquals("my-lib/", defaultShadePrefix("my-lib-1.2.jar"))
        assertEquals("jvmdowngrader-gradle/", defaultShadePrefix("jvmdowngrader-gradle-3.0.0.jar"))
        assertEquals("foo/", defaultShadePrefix("foo.jar"))
        assertEquals("plain/", defaultShadePrefix("plain"))
    }
}
