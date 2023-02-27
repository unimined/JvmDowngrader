package xyz.wagyourtail.jvmdg.internal.mods._9

import org.gradle.api.JavaVersion
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode

object PrivateInterfaceFixer {

    fun apply(classNode: ClassNode, target: JavaVersion) {
        if (target > JavaVersion.VERSION_1_8) return
        if (classNode.access and Opcodes.ACC_INTERFACE == 0) return

        val privateMethods = classNode.methods.filter { it.access and Opcodes.ACC_PRIVATE != 0 }.map { it.name to it.desc }

        for (method in classNode.methods) {
            if (method.instructions == null) continue
            for (insn in method.instructions) {
                if (insn is MethodInsnNode) {
                    if (insn.opcode == Opcodes.INVOKEINTERFACE && insn.owner == classNode.name && privateMethods.contains(insn.name to insn.desc)) {
                        insn.opcode = Opcodes.INVOKESPECIAL
                    }
                } else if (insn is InvokeDynamicInsnNode) {
                    if (insn.bsm.owner == "java/lang/invoke/LambdaMetafactory") {
                        val lambda = insn.bsmArgs[1] as Handle
                        if (classNode.name == lambda.owner && lambda.tag == Opcodes.H_INVOKEINTERFACE) {
                            if (privateMethods.contains(lambda.name to lambda.desc)) {
                                insn.bsmArgs[1] = Handle(Opcodes.H_INVOKEVIRTUAL, lambda.owner, lambda.name, lambda.desc, lambda.isInterface)
                            }
                        }
                    }
                }
            }
        }
    }
}