package xyz.wagyourtail.jvmdg.internal.mods.replace._16

import org.gradle.api.JavaVersion
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import xyz.wagyourtail.jvmdg.Constants
import xyz.wagyourtail.jvmdg.internal.mods.replace.Replace

object J_L_R_ObjectMethods {
    @Replace(
        value = JavaVersion.VERSION_16,
        desc = "Ljava/lang/runtime/ObjectMethods;bootstrap(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;",
        idBSM = true
    )
    @JvmStatic
    fun bootstrap(classNode: ClassNode, method: MethodNode, insn: Int) {
        val idin = method.instructions[insn] as InvokeDynamicInsnNode
        val recordClass = idin.bsmArgs[0] as Type
        val fieldNames = idin.bsmArgs[1] as String?
        val getters = idin.bsmArgs.copyOfRange(2, idin.bsmArgs.size).map { it as Handle }
        // check if already has equals
        val mname = "${idin.name}\$jvmdowngrader"
        var exists = false
        for (m in classNode.methods) {
            if (m.name == mname && m.desc == idin.desc) exists = true
        }
        if (!exists) {
            when (idin.name) {
                "equals" -> makeEquals(classNode, mname, idin.desc, recordClass, getters)
                "hashCode" -> makeHashCode(classNode, mname, idin.desc, recordClass, getters)
                "toString" -> makeToString(classNode, mname, idin.desc, recordClass, fieldNames!!, getters)
            }
        }
        // replace idin with call to new method
        method.instructions.insertBefore(idin, MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, mname, idin.desc, false))
        method.instructions.remove(idin)
    }

    private fun makeEquals(classNode: ClassNode, mname: String, desc: String, recordClass: Type, getters: List<Handle>) {
        classNode.visitMethod(Constants.synthetic(Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC), mname, desc, null, null).apply {
            visitCode()
            visitVarInsn(Opcodes.ALOAD, 0)
            visitVarInsn(Opcodes.ALOAD, 1)
            // check if ==
            val neq = Label()
            visitJumpInsn(Opcodes.IF_ACMPNE, neq)
            visitInsn(Opcodes.ICONST_1)
            visitInsn(Opcodes.IRETURN)
            visitLabel(neq)
            // check if 2nd null
            val notEqual = Label()
            visitVarInsn(Opcodes.ALOAD, 1)
            visitJumpInsn(Opcodes.IFNULL, notEqual)
            // check if is instance of
            visitVarInsn(Opcodes.ALOAD, 1)
            visitTypeInsn(Opcodes.INSTANCEOF, classNode.name)
            visitJumpInsn(Opcodes.IFEQ, notEqual)
            // store 2nd as this class
            visitVarInsn(Opcodes.ALOAD, 1)
            visitTypeInsn(Opcodes.CHECKCAST, classNode.name)
            visitVarInsn(Opcodes.ASTORE, 2)
            // check if fields are equal using handles provided in idin {
            getters.forEach { getter ->
                if (getter.tag != Opcodes.H_GETFIELD) throw IllegalStateException("expected H_GETFIELD got ${getter.tag}")
                visitVarInsn(Opcodes.ALOAD, 0)
                visitFieldInsn(Opcodes.GETFIELD, recordClass.internalName, getter.name, getter.desc)
                visitVarInsn(Opcodes.ALOAD, 2)
                visitFieldInsn(Opcodes.GETFIELD, recordClass.internalName, getter.name, getter.desc)
                when (getter.desc) {
                    "I", "Z", "C", "S", "B" -> {
                        visitJumpInsn(Opcodes.IF_ICMPEQ, notEqual)
                    }
                    "J" -> {
                        visitInsn(Opcodes.LCMP)
                        visitJumpInsn(Opcodes.IFEQ, notEqual)
                    }
                    "F" -> {
                        visitInsn(Opcodes.FCMPG)
                        visitJumpInsn(Opcodes.IFEQ, notEqual)
                    }
                    "D" -> {
                        visitInsn(Opcodes.DCMPG)
                        visitJumpInsn(Opcodes.IFEQ, notEqual)
                    }
                    else -> {
                        if (getter.desc.length == 1) throw IllegalStateException("unknown type ${getter.desc} for ${getter.name} in ${classNode.name}")
                        visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false)
                        visitJumpInsn(Opcodes.IFEQ, notEqual)
                    }
                }
            }
            visitInsn(Opcodes.ICONST_1)
            visitInsn(Opcodes.IRETURN)
            visitLabel(notEqual)
            visitInsn(Opcodes.ICONST_0)
            visitInsn(Opcodes.IRETURN)
            visitMaxs(0, 0)
            visitEnd()
        }
    }

    private fun makeHashCode(classNode: ClassNode, mname: String, desc: String, recordClass: Type, getters: List<Handle>) {
        classNode.visitMethod(Constants.synthetic(Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC), mname, desc, null, null).apply {
            visitCode()
            // create array for Objects.hashCode()
            visitLdcInsn(getters.size)
            visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object")
            visitVarInsn(Opcodes.ASTORE, 1)
            // store each field in array
            getters.forEachIndexed { i, getter ->
                visitVarInsn(Opcodes.ALOAD, 1)
                visitLdcInsn(i)
                visitVarInsn(Opcodes.ALOAD, 0)
                visitFieldInsn(Opcodes.GETFIELD, recordClass.internalName, getter.name, getter.desc)
                when (getter.desc) {
                    "I" -> {
                        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
                    }
                    "Z" -> {
                        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)
                    }
                    "C" -> {
                        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false)
                    }
                    "S" -> {
                        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false)
                    }
                    "B" -> {
                        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false)
                    }
                    "J" -> {
                        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
                    }
                    "F" -> {
                        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false)
                    }
                    "D" -> {
                        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
                    }
                    else -> {
                        if (getter.desc.length == 1) throw IllegalStateException("unknown type ${getter.desc} for ${getter.name} in ${classNode.name}")
                    }
                }
                visitInsn(Opcodes.AASTORE)
            }
            // call Objects.hashCode()
            visitVarInsn(Opcodes.ALOAD, 1)
            visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "hash", "([Ljava/lang/Object;)I", false)
            visitInsn(Opcodes.IRETURN)
            visitMaxs(0, 0)
            visitEnd()
        }
    }

    private fun makeToString(classNode: ClassNode, mname: String, desc: String, recordClass: Type, fieldNames: String, getters: List<Handle>) {
        val fields = fieldNames.split(";")
        classNode.visitMethod(Constants.synthetic(Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC), mname, desc, null, null).apply {
            visitCode()
            // create StringBuilder
            visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
            visitInsn(Opcodes.DUP)
            visitLdcInsn("${recordClass.internalName.split("/").last()}[")
            visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false)
            // append each field
            getters.forEachIndexed { i, getter ->
                visitLdcInsn("${fields[i]}=")
                visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                    false
                )
                visitVarInsn(Opcodes.ALOAD, 0)
                visitFieldInsn(Opcodes.GETFIELD, recordClass.internalName, getter.name, getter.desc)
                when (getter.desc) {
                    "I" -> {
                        visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(I)Ljava/lang/StringBuilder;",
                            false
                        )
                    }

                    "Z" -> {
                        visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(Z)Ljava/lang/StringBuilder;",
                            false
                        )
                    }

                    "C" -> {
                        visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(C)Ljava/lang/StringBuilder;",
                            false
                        )
                    }

                    "S" -> {
                        visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(S)Ljava/lang/StringBuilder;",
                            false
                        )
                    }

                    "B" -> {
                        visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(B)Ljava/lang/StringBuilder;",
                            false
                        )
                    }

                    "J" -> {
                        visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(J)Ljava/lang/StringBuilder;",
                            false
                        )
                    }

                    "F" -> {
                        visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(F)Ljava/lang/StringBuilder;",
                            false
                        )
                    }

                    "D" -> {
                        visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(D)Ljava/lang/StringBuilder;",
                            false
                        )
                    }

                    else -> {
                        if (getter.desc.length == 1) throw IllegalStateException("unknown type ${getter.desc} for ${getter.name} in ${classNode.name}")
                        visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                            false
                        )
                    }
                }
                if (i != getters.size - 1) {
                    visitLdcInsn(", ")
                    visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/StringBuilder",
                        "append",
                        "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                        false
                    )
                }
            }
            // append ']'
            visitLdcInsn("]")
            visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false
            )
            // call StringBuilder.toString()
            visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false
            )
            visitInsn(Opcodes.ARETURN)
            visitMaxs(0, 0)
            visitEnd()
        }
    }
}