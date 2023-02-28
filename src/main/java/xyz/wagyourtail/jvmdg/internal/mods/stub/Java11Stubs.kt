package xyz.wagyourtail.jvmdg.internal.mods.stub

import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer
import xyz.wagyourtail.jvmdg.internal.mods.stub._11.J_L_String

object Java11Stubs {
    fun apply() {
        MethodReplacer.apply {
            // -- java.base --
            addClass(J_L_String::class.java)
        }
    }
}