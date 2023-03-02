package xyz.wagyourtail.jvmdg.internal.mods

import io.github.classgraph.ClassGraph
import org.gradle.api.JavaVersion
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import xyz.wagyourtail.jvmdg.internal.mods.replace.Replace
import xyz.wagyourtail.jvmdg.internal.mods.replace._16.J_L_R_ObjectMethods
import xyz.wagyourtail.jvmdg.internal.mods.replace._16.J_L_Record
import xyz.wagyourtail.jvmdg.internal.mods.replace._9.J_L_I_StringConcatFactory
import xyz.wagyourtail.jvmdg.internal.mods.stub.*
import java.io.File

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

        val classgraph by lazy {
            val classpath = System.getProperty("java.class.path")
            // split to paths
            val classPaths = classpath.split(File.pathSeparator).map { File(it).toURI() }.toMutableList()
            // add java base into classpath
            if (JavaVersion.current().isJava9Compatible()) {
                // add all in java home / lib
                File(System.getProperty("java.home"), "jmods").listFiles()?.filter { it.extension == "jmod" }?.map { it.toURI() }?.let { classPaths.addAll(it) }
            } else {
                // add rt.jar
                classPaths.add(File(System.getProperty("java.home"), "lib/rt.jar").toURI())
            }
            val graphInfo = ClassGraph()
                .enableClassInfo()
                .rejectPackages(
                    "net.java", "com.sun", "com.jcraft", "com.intellij", "jdk", "akka", "ibxm", "scala", "*.jetty.*"
                )
                .overrideClasspath(classPaths)
                .disableModuleScanning()
//            graphInfo.overrideClasspath()
              graphInfo.scan()
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
                        if (stub.subtypes) {
                            for (c in classgraph.allClasses.filter { it.superclasses.any { it.name.replace('.', '/') == arg.internalName } || it.interfaces.any { it.name.replace('.', '/') == arg.internalName } }) {
                                val cdesc = "L" + c.name.replace('.', '/') + ";"
                                val ddesc = Type.getMethodDescriptor(
                                    Type.getType(cdesc),
                                    *Type.getArgumentTypes(mnode.desc).drop(1).toTypedArray()
                                )
                                availableStubs.getOrPut(stub.value) { mutableMapOf() }[cdesc + mnode.name + ddesc] = mnode to incl
                            }
                        }
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
            Java15Stubs.apply()
            Java14Stubs.apply()
            Java13Stubs.apply()
            Java12Stubs.apply()
            Java11Stubs.apply()
        }

    }

    val stubs = availableStubs.filterKeys { !target.isCompatibleWith(it) }.values.flatMap { it.entries }.associate { it.key to it.value }
    val replaces = availableReplace.filterKeys { !target.isCompatibleWith(it) }.values.flatMap { it.entries }.associate { it.key to it.value }

    fun apply(classNode: ClassNode, shadePkg: String): Map<String, Pair<MethodNode, Set<Class<*>>>> {
        val stubbed = mutableMapOf<String, Pair<MethodNode, Set<Class<*>>>>()
        for (method in classNode.methods.toList()) {
            if (method.instructions == null) continue
            var i = -1
            while (i < method.instructions.size() - 1) {
                i++
                val insn = method.instructions.get(i)
                if (insn is MethodInsnNode) {
                    val desc = if (insn.name == "<init>") {
                        insn.desc.replace(")V", ")L${insn.owner};")
                    } else {
                        insn.desc
                    }
                    val stub = stubs["L" + insn.owner + ";" + insn.name + desc]
                    if (stub != null) {
                        if (insn.name == "<init>") {
                            // remove new and dup
                            // search backwards for new
                            var j = i - 1
                            var skip = 0
                            while (j >= 0) {
                                val prev = method.instructions.get(j)
                                if (prev is TypeInsnNode && prev.opcode == Opcodes.NEW && prev.desc == insn.owner && skip-- == 0) {
                                    method.instructions.remove(prev)
                                    // ensure and remove dup
                                    val dup = method.instructions.get(j)
                                    if (dup is InsnNode && dup.opcode == Opcodes.DUP) {
                                        method.instructions.remove(dup)
                                    } else {
                                        throw IllegalStateException("Could not find dup for stubbed constructor ${insn.owner}.${insn.name}${insn.desc}")
                                    }
                                    break
                                } else if (prev is MethodInsnNode && prev.opcode == Opcodes.INVOKESPECIAL && prev.owner == insn.owner && prev.name == "<init>") {
                                    skip++
                                }
                                assert(skip > -1)
                                j--
                            }
                            if (j < 0) throw IllegalStateException("Could not find new for stubbed constructor ${insn.owner}.${insn.name}${insn.desc}")
                            i -= 2
                        }
                        // check if return type of stub is different, if so we need to insert a checkcast
                        if (insn.name != "<init>" && Type.getReturnType(stub.first.desc) != Type.getReturnType(insn.desc)) {
                            method.instructions.insert(insn, TypeInsnNode(Opcodes.CHECKCAST, Type.getReturnType(insn.desc).internalName))
                            i++
                        }
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