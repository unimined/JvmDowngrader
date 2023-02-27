package xyz.wagyourtail.jvmdg

import org.objectweb.asm.Opcodes

object Constants {
    val DEBUG = true

    fun synthetic(opcodes: Int): Int {
        if (DEBUG) return opcodes
        return opcodes or Opcodes.ACC_SYNTHETIC
    }
}