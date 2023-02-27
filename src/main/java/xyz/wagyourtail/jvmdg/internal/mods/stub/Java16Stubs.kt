package xyz.wagyourtail.jvmdg.internal.mods.stub

import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer
import xyz.wagyourtail.jvmdg.internal.mods.stub._16.J_L_Class
import xyz.wagyourtail.jvmdg.internal.mods.stub._16.J_L_R_RecordComponent

object Java16Stubs {
    fun apply() {
        MethodReplacer.apply {
            // -- java.base --
            addClass(J_L_Class::class.java)


            addClass(J_L_R_RecordComponent::class.java)

        }
    }
}