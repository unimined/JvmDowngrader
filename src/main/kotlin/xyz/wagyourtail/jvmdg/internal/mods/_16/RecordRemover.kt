package xyz.wagyourtail.jvmdg.internal.mods._16

import org.gradle.api.JavaVersion
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import xyz.wagyourtail.jvmdg.Constants

object RecordRemover {

    fun apply(classNode: ClassNode, target: JavaVersion) {
        if (target > JavaVersion.VERSION_15) return
        if (classNode.access and Opcodes.ACC_RECORD != 0) {
            classNode.access = classNode.access and Opcodes.ACC_RECORD.inv()
            // add a field
            classNode.fields.add(0, FieldNode(Constants.synthetic(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC), "recordComponents\$jvmdowngrader", "Ljava/lang/String;", null, classNode.recordComponents.joinToString(":") { "${it.name} ${it.signature}" }))
            classNode.recordComponents = null
        }
    }
}