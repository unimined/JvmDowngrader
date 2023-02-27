package xyz.wagyourtail.jvmdg.internal.mods

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode

object RecordRemover {

    fun apply(classNode: ClassNode) {
        if (classNode.access and Opcodes.ACC_RECORD != 0) {
            classNode.access = classNode.access and Opcodes.ACC_RECORD.inv()
            classNode.superName = "java/lang/Object"
            classNode.recordComponents = null
        }
        for (method in classNode.methods) {
            if (method.desc.contains("Ljava/lang/Record;")) {
                method.desc = method.desc.replace("Ljava/lang/Record;", "Ljava/lang/Object;")
            }
            for (insn in method.instructions) {
                if (insn.opcode == Opcodes.INVOKEDYNAMIC && insn is InvokeDynamicInsnNode) {
                    for (i in 0 until insn.bsmArgs.size) {
                        val arg = insn.bsmArgs[i]
                        if (arg is Handle) {
                            if (arg.desc.contains("Ljava/lang/Record;")) {
                                insn.bsmArgs[i] = Handle(arg.tag, arg.owner, arg.name, arg.desc.replace("Ljava/lang/Record;", "Ljava/lang/Object;"), arg.isInterface)
                            }
                        } else if (arg is Type) {
                            when (arg.sort) {
                                Type.OBJECT, Type.ARRAY -> {
                                    if (arg.descriptor.contains("Ljava/lang/Record;")) {
                                        insn.bsmArgs[i] = Type.getType(arg.descriptor.replace("Ljava/lang/Record;", "Ljava/lang/Object;"))
                                    }
                                }
                                Type.METHOD -> {
                                    val args = arg.argumentTypes
                                    var changed = false
                                    for (j in args.indices) {
                                        if (args[j].descriptor.contains("Ljava/lang/Record;")) {
                                            args[j] = Type.getType(args[j].descriptor.replace("Ljava/lang/Record;", "Ljava/lang/Object;"))
                                            changed = true
                                        }
                                    }
                                    val returnType = if (arg.returnType.descriptor.contains("Ljava/lang/Record;")) {
                                        changed = true
                                        Type.getType(arg.returnType.descriptor.replace("Ljava/lang/Record;", "Ljava/lang/Object;"))
                                    } else {
                                        arg.returnType
                                    }
                                    if (changed) {
                                        insn.bsmArgs[i] = Type.getMethodType(returnType, *args)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}