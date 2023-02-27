package xyz.wagyourtail.jvmdg.internal.downgraders

import org.gradle.api.JavaVersion
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import xyz.wagyourtail.jvmdg.internal.downgraders.replace.Replace
import xyz.wagyourtail.jvmdg.internal.downgraders.replace.java.lang.runtime.ObjectMethods
import xyz.wagyourtail.jvmdg.internal.downgraders.stubs.Stub
import xyz.wagyourtail.jvmdg.internal.downgraders.replace.java.lang.RecordStub
import xyz.wagyourtail.jvmdg.internal.downgraders.replace.java.lang.invoke.StringConcatFactory

object MethodReplacer {
    val availableStubs: MutableMap<JavaVersion, MutableMap<String, MethodNode>> = mutableMapOf()
    val availableReplace: MutableMap<JavaVersion, MutableMap<String, (ClassNode, MethodNode, Int) -> Unit>> = mutableMapOf()

    init {
        addClass(ObjectMethods::class.java)
        addClass(RecordStub::class.java)
        addClass(StringConcatFactory::class.java)
    }

    private fun splitMethod(desc: String): Triple<String, String?, String?> {
        val owner = desc.substring(0, desc.indexOf(';') + 1)
        if (owner == desc) return Triple(owner, null, null)
        if (desc.indexOf('(') == -1) return Triple(owner, desc.substring(desc.indexOf(';') + 1), null)
        val name = desc.substring(desc.indexOf(';') + 1, desc.indexOf('('))
        val mdesc = desc.substring(desc.indexOf('('))
        return Triple(owner, name, mdesc)
    }

    fun addClass(`class`: Class<*>) {
        val internalClassName = `class`.name.replace('.', '/')
        // get class node
        val node = `class`.classLoader.getResourceAsStream("$internalClassName.class").use {
            val reader = ClassReader(it)
            val node = ClassNode()
            reader.accept(node, 0)
            node
        }
        for (method in `class`.declaredMethods) {
            if (method.modifiers and Opcodes.ACC_STATIC == 0) continue
            if (method.modifiers and Opcodes.ACC_PUBLIC == 0) continue
            val mnode = node.methods.firstOrNull { it.name == method.name && it.desc == Type.getMethodDescriptor(method) } ?: throw IllegalStateException("Could not find method ${method.name}${Type.getMethodDescriptor(method)} in class ${`class`.name}")
            val stub = method.getAnnotation(Stub::class.java)
            if (stub != null) {
                if (stub.desc == "") {
                    // get first arg
                    val arg = Type.getArgumentTypes(mnode.desc).firstOrNull()
                        ?: throw IllegalStateException("Method ${method.name}${Type.getMethodDescriptor(method)} in class ${`class`.name} is missing an argument")
                    // remove first arg from method desc
                    val desc = Type.getMethodDescriptor(
                        Type.getReturnType(mnode.desc),
                        *Type.getArgumentTypes(mnode.desc).drop(1).toTypedArray()
                    )
                    availableStubs.getOrPut(stub.value) { mutableMapOf() }["L" + arg.internalName + ";" + mnode.name + desc] = mnode
                } else if (stub.desc.contains('(')) {
                    availableStubs.getOrPut(stub.value) { mutableMapOf() }[stub.desc] = mnode
                } else {
                    val (owner, name, mdesc) = splitMethod(stub.desc)
                    val tdesc = Type.getMethodDescriptor(
                        Type.getReturnType(mnode.desc),
                        *Type.getArgumentTypes(mnode.desc).drop(1).toTypedArray()
                    )
                    availableStubs.getOrPut(stub.value) { mutableMapOf() }[owner + (name ?: mnode.name) + (mdesc ?: tdesc)] = mnode

                }
            }
            val replace = method.getAnnotation(Replace::class.java)
            if (replace != null) {
                availableReplace.getOrPut(replace.value) { mutableMapOf() }[replace.desc] = { c, m, i -> method(null, c, m, i)}
            }
        }
    }

    fun apply(classNode: ClassNode, target: JavaVersion, shadePkg: String): Map<String, MethodNode> {
        val stubs = availableStubs.filterKeys { !target.isCompatibleWith(it) }.values.flatMap { it.entries }.associate { it.key to it.value }
        val replaces = availableReplace.filterKeys { !target.isCompatibleWith(it) }.values.flatMap { it.entries }.associate { it.key to it.value }
        val stubbed = mutableMapOf<String, MethodNode>()
        for (method in classNode.methods.toList()) {
            if (method.instructions == null) continue
            for (i in 0 until method.instructions.size()) {
                val insn = method.instructions.get(i)
                if (insn is MethodInsnNode) {
                    val stub = stubs["L" + insn.owner + ";" + insn.name + insn.desc]
                    if (stub != null) {
                        //TODO: check cast args, idk if needed, but if there's issues...
                        insn.owner = "$shadePkg/${insn.owner}"
                        insn.name = stub.name
                        insn.desc = stub.desc
                        insn.opcode = Opcodes.INVOKESTATIC
                        stubbed["L" + insn.owner + ";" + insn.name + insn.desc] = stub
                    }
                    val replace = replaces["L" + insn.owner + ";" + insn.name + insn.desc]
                    if (replace != null) {
                        replace(classNode, method, i)
                    }
                } else if (insn is InvokeDynamicInsnNode) {
                    val replace = replaces["L" + insn.bsm.owner + ";" + insn.bsm.name + insn.bsm.desc]
                    if (replace != null) {
                        replace(classNode, method, i)
                    }
                    for (j in insn.bsmArgs.indices) {
                        if (insn.bsmArgs[j] is Handle) {
                            val handle = insn.bsmArgs[j] as Handle
                            val stub = stubs["L" + handle.owner + ";" + handle.name + handle.desc]
                            if (stub != null) {
                                insn.bsmArgs[j] = Handle(
                                    Opcodes.H_INVOKESTATIC,
                                    "$shadePkg/${handle.owner}",
                                    stub.name,
                                    stub.desc,
                                    false
                                )
                                stubbed["L" + handle.owner + ";" + stub.name + stub.desc] = stub
                            }
                        }
                    }
                }
            }
        }
        return stubbed
    }

}