package xyz.wagyourtail.jvmdg.internal.mods.replace._9

import org.gradle.api.JavaVersion
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import xyz.wagyourtail.jvmdg.internal.mods.replace.Replace

object J_L_I_StringConcatFactory {

    @Replace(
        value = JavaVersion.VERSION_1_9,
        desc = "Ljava/lang/invoke/StringConcatFactory;makeConcatWithConstants(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
        idBSM = true
    )
    @JvmStatic
    fun makeConcatWithConstants(cnode: ClassNode, mnode: MethodNode, insn: Int) {
        val idin = mnode.instructions[insn] as InvokeDynamicInsnNode
        val argTypes = Type.getArgumentTypes(idin.desc)
        var args = idin.bsmArgs[0] as String
        val insns = makeConcatInternal(args, argTypes)
        mnode.instructions.insertBefore(idin, insns)
        mnode.instructions.remove(idin)
    }

    @Replace(
        value = JavaVersion.VERSION_1_9,
        desc = "Ljava/lang/invoke/StringConcatFactory;makeConcat(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
        idBSM = true
    )
    @JvmStatic
    fun makeConcat(cnode: ClassNode, mnode: MethodNode, insn: Int) {
        val idin = mnode.instructions[insn] as InvokeDynamicInsnNode
        // this one doesn't have any args, we want to generate a new one.
        val argTypes = Type.getArgumentTypes(idin.desc)
        val args = 0.until(argTypes.size).joinToString("") { "\u0001" }
        val insns = makeConcatInternal(args, argTypes)
        mnode.instructions.insertBefore(idin, insns)
        mnode.instructions.remove(idin)
    }

    //TODO: make synthetic method, this is like 2-4x slower than string builder
    private fun makeConcatInternal(args: String, types: Array<Type>): InsnList {
        val list = InsnList()
        var args = args
        val types = types.toMutableList()
        // current stack = [...types]
        if (args.endsWith('\u0001')) {
            val last = types.removeLast()
            list.add(
                MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/lang/String",
                    "valueOf",
                    "(" + (if (last.descriptor.length == 1) last.descriptor else "Ljava/lang/Object;") + ")Ljava/lang/String;",
                    false
                )
            )
            args = args.substring(0, args.length - 1)
        } else {
            // last is literal
            val literal = args.substring(args.lastIndexOf('\u0001') + 1)
            list.add(LdcInsnNode(literal))
            args = args.substring(0, args.lastIndexOf('\u0001') + 1)
        }
        // current stack = [...remainingtypes, string]
        while (args.isNotEmpty()) {
            if (args.endsWith('\u0001')) {
                // last is type
                val last = types.removeLast()
                // current stack = [...remainingtypes, type, string]
                // get size
                when (last.size) {
                    1 -> list.add(InsnNode(Opcodes.SWAP))
                    2 -> {
                        list.add(InsnNode(Opcodes.DUP_X2))
                        list.add(InsnNode(Opcodes.POP))
                    }
                    else -> throw IllegalStateException("Invalid type size: ${last.size}")
                }
                // current stack = [...remainingtypes, string, type]
                list.add(
                    MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "java/lang/String",
                        "valueOf",
                        "(" + (if (last.descriptor.length == 1) last.descriptor else "Ljava/lang/Object;") + ")Ljava/lang/String;",
                        false
                    )
                )
                // current stack = [...remainingtypes, string, string(type)]
                // string(type).concat(string)
                list.add(InsnNode(Opcodes.SWAP))
                // current stack = [...remainingtypes, string(type), string]
                list.add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false))
                // current stack = [...remainingtypes, string]
                args = args.substring(0, args.length - 1)
            } else {
                // last is literal, create on top of stack
                val literal = args.substring(args.lastIndexOf('\u0001') + 1)
                list.add(LdcInsnNode(literal))
                // current stack = [...remainingtypes, string, literal]
                // literal.concat(string)
                list.add(InsnNode(Opcodes.SWAP))
                // current stack = [...remainingtypes, literal, string]
                list.add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false))
                // current stack = [...remainingtypes, string]
                args = args.substring(0, args.lastIndexOf('\u0001') + 1)
            }
        }
        if (types.isNotEmpty()) {
            throw IllegalStateException("Types not empty: $types")
        }

        return list
    }
}