package xyz.wagyourtail.jvmdg.internal.mods.replace._16

import org.gradle.api.JavaVersion
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import xyz.wagyourtail.jvmdg.internal.mods.replace.Replace

object RecordStub {
    @Replace(value = JavaVersion.VERSION_16, desc = "Ljava/lang/Record;<init>()V")
    @JvmStatic
    fun init(cnode: ClassNode?, mnode: MethodNode, insn: Int) {
        (mnode.instructions[insn] as MethodInsnNode).owner = "java/lang/Object"
    }

    @Replace(value = JavaVersion.VERSION_16, desc = "Ljava/lang/Record;equals(Ljava/lang/Object;)Z")
    @JvmStatic
    fun equals(cnode: ClassNode?, mnode: MethodNode, insn: Int) {
        (mnode.instructions[insn] as MethodInsnNode).owner = "java/lang/Object"
    }

    @Replace(value = JavaVersion.VERSION_16, desc = "Ljava/lang/Record;hashCode()I")
    @JvmStatic
    fun hashCode(cnode: ClassNode?, mnode: MethodNode, insn: Int) {
        (mnode.instructions[insn] as MethodInsnNode).owner = "java/lang/Object"
    }

    @Replace(value = JavaVersion.VERSION_16, desc = "Ljava/lang/Record;toString()Ljava/lang/String;")
    @JvmStatic
    fun toString(cnode: ClassNode?, mnode: MethodNode, insn: Int) {
        (mnode.instructions[insn] as MethodInsnNode).owner = "java/lang/Object"
    }
}