package xyz.wagyourtail.jvmdg.internal.mods._17

import org.gradle.api.JavaVersion
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import xyz.wagyourtail.jvmdg.Constants

object SealRemover {
    fun apply(classNode: ClassNode, target: JavaVersion) {
        if (target > JavaVersion.VERSION_16) return
        // add field to classNode to store the permitted subclasses
        if (classNode.permittedSubclasses != null) {
            classNode.fields.add(0, FieldNode(Constants.synthetic(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC), "permittedSubclasses\$jvmdowngrader", "Ljava/lang/String;", null, classNode.permittedSubclasses.joinToString("") { "$it;" }))
            classNode.permittedSubclasses = null
        }

    }
}