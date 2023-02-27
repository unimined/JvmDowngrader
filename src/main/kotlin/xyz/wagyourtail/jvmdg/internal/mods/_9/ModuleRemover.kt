package xyz.wagyourtail.jvmdg.internal.mods._9

import org.gradle.api.JavaVersion
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

object ModuleRemover {

    private fun assertMethod(owner: String, name: String, desc: String, insn: AbstractInsnNode) {
        if (insn.type == AbstractInsnNode.METHOD_INSN) {
            val min = insn as MethodInsnNode
            if (owner == min.owner && name == min.name && desc == min.desc) {
                return
            }
        }
        throw IllegalStateException(
            "Expected $owner#$name$desc call but found " + describeNode(
                insn,
                false
            )
        )
    }

    private fun describeNode(node: AbstractInsnNode?, listFormat: Boolean): String? {
        if (node == null) {
            return if (listFormat) String.format("   %-14s ", "null") else "null"
        }
        if (node is LabelNode) {
            return String.format("[%s]", node.label)
        }
        var out: String? = String.format(
            if (listFormat) "   %-14s " else "%s ",
            node.javaClass.simpleName.replace("Node", "")
        )
        if (node is JumpInsnNode) {
            out += String.format("[%s] [%s]", getOpcodeName(node), node.label.label)
        } else if (node is VarInsnNode) {
            out += String.format("[%s] %d", getOpcodeName(node), node.`var`)
        } else if (node is MethodInsnNode) {
            val mth = node
            out += String.format("[%s] %s::%s%s", getOpcodeName(node), mth.owner, mth.name, mth.desc)
        } else if (node is FieldInsnNode) {
            val fld = node
            out += String.format("[%s] %s::%s:%s", getOpcodeName(node), fld.owner, fld.name, fld.desc)
        } else if (node is InvokeDynamicInsnNode) {
            val idc = node
            out += String.format(
                "[%s] %s%s { %s %s::%s%s }", getOpcodeName(node), idc.name, idc.desc,
                getOpcodeName(idc.bsm.tag, "H_GETFIELD", 1), idc.bsm.owner, idc.bsm.name, idc.bsm.desc
            )
        } else if (node is LineNumberNode) {
            val ln = node
            out += String.format("LINE=[%d] LABEL=[%s]", ln.line, ln.start.label)
        } else if (node is LdcInsnNode) {
            out += node.cst
        } else if (node is IntInsnNode) {
            out += node.operand
        } else if (node is FrameNode) {
            out += String.format("[%s] ", getOpcodeName(node.type, "H_INVOKEINTERFACE", -1))
        } else if (node is TypeInsnNode) {
            out += String.format("[%s] %s", getOpcodeName(node), node.desc)
        } else {
            out += String.format("[%s] ", getOpcodeName(node))
        }
        return out
    }

    private fun getOpcodeName(node: AbstractInsnNode?): String? {
        return if (node != null) getOpcodeName(node.opcode) else ""
    }

    fun getOpcodeName(opcode: Int): String? {
        return getOpcodeName(opcode, "UNINITIALIZED_THIS", 1)
    }

    private fun getOpcodeName(opcode: Int, start: String, min: Int): String? {
        if (opcode >= min) {
            var found = false
            try {
                for (f in Opcodes::class.java.declaredFields) {
                    if (!found && f.name != start) {
                        continue
                    }
                    found = true
                    if (f.type == Integer.TYPE && f.getInt(null) == opcode) {
                        return f.name
                    }
                }
            } catch (ex: Exception) {
                // derp
            }
        }
        return if (opcode >= 0) opcode.toString() else "UNKNOWN"
    }

    fun apply(classNode: ClassNode, target: JavaVersion) {
        if (target > JavaVersion.VERSION_1_8) return
        // find <clinit> method
        val clinit = classNode.methods.find { it.name == "<clinit>" } ?: return

        var stage = -1

        val it: MutableListIterator<AbstractInsnNode> = clinit.instructions.iterator()
        out@ while (it.hasNext()) {
            val insn = it.next()
            when (stage) {
                -1 -> if (insn.type == AbstractInsnNode.LDC_INSN) {
                    val lin = insn as LdcInsnNode
                    if (lin.cst is Type && "Ljava/lang/Runtime;" == (lin.cst as Type).descriptor) {
                        stage = 1
                        it.remove()
                    }
                }

                1 -> {
                    assertMethod("java/lang/Class", "getModule", "()Ljava/lang/Module;", insn)
                    stage++
                    it.remove()
                }

                2 -> {
                    assertMethod("java/lang/Module", "getLayer", "()Ljava/lang/ModuleLayer;", insn)
                    stage++
                    it.remove()
                }

                3 -> {
                    if (insn.type == AbstractInsnNode.LDC_INSN) {
                        val lin = insn as LdcInsnNode
                        if ("D" == lin.cst) {
                            stage++
                            it.remove()
                            continue
                        }
                    }
                    throw IllegalStateException("Expected jdk.jfr load but found " + describeNode(insn, false))
                }

                4 -> {
                    assertMethod(
                        "java/lang/ModuleLayer",
                        "findModule",
                        "(Ljava/lang/String;)Ljava/util/Optional;",
                        insn
                    )
                    stage++
                    it.remove()
                }

                5 -> {
                    assertMethod("java/util/Optional", "isPresent", "()Z", insn)
                    stage++
                    it.set(InsnNode(Opcodes.ICONST_0))
                    break@out
                }
            }
        }
        check(stage == 6) { "Failed to find injection: $stage" }
    }
}