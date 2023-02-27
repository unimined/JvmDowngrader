package xyz.wagyourtail.jvmdg.internal.mods

import org.gradle.api.JavaVersion
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import xyz.wagyourtail.jvmdg.internal.mods.replace.Replace
import xyz.wagyourtail.jvmdg.internal.mods.replace._16.J_L_R_ObjectMethods
import xyz.wagyourtail.jvmdg.internal.mods.replace._16.J_L_Record
import xyz.wagyourtail.jvmdg.internal.mods.replace._9.J_L_I_StringConcatFactory
import xyz.wagyourtail.jvmdg.internal.mods.stub.Java16Stubs
import xyz.wagyourtail.jvmdg.internal.mods.stub.Java17Stubs
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub

class MethodReplacer(target: JavaVersion) {
    companion object {
        val availableStubs: MutableMap<JavaVersion, MutableMap<String, Pair<MethodNode, Set<Class<*>>>>> = mutableMapOf()
        val availableReplace: MutableMap<JavaVersion, MutableMap<String, (ClassNode, MethodNode, Int) -> Unit>> = mutableMapOf()

        private fun splitMethod(desc: String): Triple<String, String?, String?> {
            val owner = desc.substring(0, desc.indexOf(';') + 1)
            if (owner == desc) return Triple(owner, null, null)
            if (desc.indexOf('(') == -1) return Triple(owner, desc.substring(desc.indexOf(';') + 1), null)
            val name = desc.substring(desc.indexOf(';') + 1, desc.indexOf('('))
            val mdesc = desc.substring(desc.indexOf('('))
            return Triple(owner, name, mdesc)
        }

        fun resolveClassPath(`class`: Class<*>): String {
            if (`class`.enclosingClass != null) {
                return resolveClassPath(`class`.enclosingClass).substringBefore(".class") + "$" + `class`.simpleName + ".class"
            }
            return `class`.canonicalName.replace('.', '/') + ".class"
        }

        fun classToNode(`class`: Class<*>): ClassNode {
            // get class node
            return `class`.classLoader.getResourceAsStream(resolveClassPath(`class`)).use {
                if (it == null) throw IllegalStateException("Could not find class ${`class`.name} in classloader ${`class`.classLoader} at path ${resolveClassPath(`class`)}.")
                val reader = ClassReader(it)
                val node = ClassNode()
                reader.accept(node, 0)
                node
            }
        }

        fun addClass(`class`: Class<*>) {
            // check if stub is on class itself
            val stub = `class`.getAnnotation(Stub::class.java)
            if (stub != null) {
                // send to classreplacer instead
                ClassReplacer.registerReplace(stub.value, Class.forName(stub.desc.replace('/', '.').let { it.substring(1, it.length - 1) }), `class`, true)
                return
            }
            val node = classToNode(`class`)
            for (method in `class`.declaredMethods) {
                if (method.modifiers and Opcodes.ACC_STATIC == 0) continue
                if (method.modifiers and Opcodes.ACC_PUBLIC == 0) continue
                val mnode = node.methods.firstOrNull { it.name == method.name && it.desc == Type.getMethodDescriptor(method) } ?: throw IllegalStateException("Could not find method ${method.name}${Type.getMethodDescriptor(method)} in class ${`class`.name}")
                val stub = method.getAnnotation(Stub::class.java)
                if (stub != null) {
                    val incl = stub.include.map { it.java }.toSet()
                    if (stub.desc == "") {
                        // get first arg
                        val arg = Type.getArgumentTypes(mnode.desc).firstOrNull()
                            ?: throw IllegalStateException("Method ${method.name}${Type.getMethodDescriptor(method)} in class ${`class`.name} is missing an argument")
                        // remove first arg from method desc
                        val desc = Type.getMethodDescriptor(
                            Type.getReturnType(mnode.desc),
                            *Type.getArgumentTypes(mnode.desc).drop(1).toTypedArray()
                        )
                        availableStubs.getOrPut(stub.value) { mutableMapOf() }["L" + arg.internalName + ";" + mnode.name + desc] = mnode to incl
                    } else if (stub.desc.contains('(')) {
                        availableStubs.getOrPut(stub.value) { mutableMapOf() }[stub.desc] = mnode to incl
                    } else {
                        val (owner, name, mdesc) = splitMethod(stub.desc)
                        val tdesc = Type.getMethodDescriptor(
                            Type.getReturnType(mnode.desc),
                            *Type.getArgumentTypes(mnode.desc)
                        )
                        availableStubs.getOrPut(stub.value) { mutableMapOf() }[owner + (name ?: mnode.name) + (mdesc ?: tdesc)] = mnode to incl
                    }
                }
                val replace = method.getAnnotation(Replace::class.java)
                if (replace != null) {
                    availableReplace.getOrPut(replace.value) { mutableMapOf() }[replace.desc] = { c, m, i -> method(null, c, m, i)}
                }
            }
        }

        init {
            addClass(J_L_R_ObjectMethods::class.java)
            addClass(J_L_Record::class.java)
            addClass(J_L_I_StringConcatFactory::class.java)

            Java17Stubs.apply()
            Java16Stubs.apply()
        }

    }

    val stubs = availableStubs.filterKeys { !target.isCompatibleWith(it) }.values.flatMap { it.entries }.associate { it.key to it.value }
    val replaces = availableReplace.filterKeys { !target.isCompatibleWith(it) }.values.flatMap { it.entries }.associate { it.key to it.value }

    fun apply(classNode: ClassNode, shadePkg: String): Map<String, Pair<MethodNode, Set<Class<*>>>> {
        val stubbed = mutableMapOf<String, Pair<MethodNode, Set<Class<*>>>>()
        for (method in classNode.methods.toList()) {
            if (method.instructions == null) continue
            for (i in 0 until method.instructions.size()) {
                val insn = method.instructions.get(i)
                if (insn is MethodInsnNode) {
                    val stub = stubs["L" + insn.owner + ";" + insn.name + insn.desc]
                    if (stub != null) {
                        //TODO: check cast args, idk if needed, but if there's issues...
                        insn.owner = "$shadePkg/${insn.owner}"
                        insn.name = stub.first.name
                        insn.desc = stub.first.desc
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
                                    stub.first.name,
                                    stub.first.desc,
                                    false
                                )
                                stubbed["L" + handle.owner + ";" + stub.first.name + stub.first.desc] = stub
                            }
                        }
                    }
                }
            }
        }
        return stubbed
    }

}