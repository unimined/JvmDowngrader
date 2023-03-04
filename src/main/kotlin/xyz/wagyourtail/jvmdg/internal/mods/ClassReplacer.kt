package xyz.wagyourtail.jvmdg.internal.mods

import org.gradle.api.JavaVersion
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TypeInsnNode

class ClassReplacer(val target: JavaVersion) {
    companion object {
        private val availableReplace = mutableMapOf<JavaVersion, MutableMap<String, Pair<String, Set<String>>>>()

        fun registerReplace(version: JavaVersion, desc: String, replace: String, newIsStub: Boolean = false, otherIncludes: Set<String> = emptySet()) {
            availableReplace.getOrPut(version) { mutableMapOf() }[desc] = replace to if (newIsStub) { setOf(replace) + otherIncludes } else { otherIncludes }
        }

        fun registerReplace(version: JavaVersion, class1: Class<*>, class2: Class<*>, newIsStub: Boolean = false, otherIncludes: Set<String> = emptySet()) {
            registerReplace(version, "L" + MethodReplacer.resolveClassPath(class1).substringBefore(".class")+ ";", "L" + MethodReplacer.resolveClassPath(class2).substringBefore(".class") + ";", newIsStub, otherIncludes)
        }

        init {
            // java.lang.record
            registerReplace(JavaVersion.VERSION_16, "Ljava/lang/Record;", "Ljava/lang/Object;")
        }
    }

    val replaces = availableReplace.filterKeys { !target.isCompatibleWith(it) }.values.flatMap { it.entries }.associate { it.key to it.value }

    fun apply(classNode: ClassNode): Set<String> {
        val newClasses = mutableSetOf<String>()
        if ("L${classNode.superName};" in replaces) {
            newClasses.addAll(replaces["L${classNode.superName};"]!!.second)
            classNode.superName = replaces["L${classNode.superName};"]!!.first.let { it.substring(1, it.length-1) }
        }
        if (classNode.interfaces != null) {
            for (i in classNode.interfaces.indices) {
                if ("L${classNode.interfaces[i]}" in replaces) {
                    newClasses.addAll(replaces["L${classNode.interfaces[i]}"]!!.second)
                    classNode.interfaces[i] = replaces["L${classNode.interfaces[i]}"]!!.first.let { it.substring(1, it.length-1) }
                }
            }
        }
        for (method in classNode.methods.toList()) {
            run {
                val args = Type.getArgumentTypes(method.desc)
                var returnType = Type.getReturnType(method.desc)
                var changed = false
                for (i in args.indices) {
                    if (!args[i].descriptor.contains("L")) continue
                    val arr = if (args[i].descriptor.startsWith("[")) args[i].dimensions else 0
                    val desc = args[i].descriptor.substring(arr)
                    if (desc in replaces) {
                        newClasses.addAll(replaces[desc]!!.second)
                        args[i] = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                        changed = true
                    }
                }
                val arr = if (returnType.descriptor.startsWith("[")) returnType.dimensions else 0
                val desc = returnType.descriptor.substring(arr)
                if (desc in replaces) {
                    newClasses.addAll(replaces[desc]!!.second)
                    returnType = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                    changed = true
                }
                if (changed) {
                    method.desc = Type.getMethodDescriptor(returnType, *args)
                }
            }

            for (insn in method.instructions.toList()) {
                if (insn.opcode == Opcodes.INVOKEDYNAMIC && insn is InvokeDynamicInsnNode) {
                    for (i in 0 until insn.bsmArgs.size) {
                        val arg = insn.bsmArgs[i]
                        if (arg is Handle) {
                            if (arg.desc.contains("(")) {
                                val args = Type.getArgumentTypes(arg.desc)
                                var returnType = Type.getReturnType(arg.desc)
                                var changed = false
                                for (i in args.indices) {
                                    if (!args[i].descriptor.contains("L")) continue
                                    val arr = if (args[i].descriptor.startsWith("[")) args[i].dimensions else 0
                                    val desc = args[i].descriptor.substring(arr)
                                    if (desc in replaces) {
                                        newClasses.addAll(replaces[desc]!!.second)
                                        args[i] = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                                        changed = true
                                    }
                                }
                                if (returnType.descriptor.contains("L")) {
                                    val arr = if (returnType.descriptor.startsWith("[")) returnType.dimensions else 0
                                    val desc = returnType.descriptor.substring(arr)
                                    if (desc in replaces) {
                                        newClasses.addAll(replaces[desc]!!.second)
                                        returnType = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                                        changed = true
                                    }
                                }
                                if (changed) {
                                    insn.bsmArgs[i] = Handle(arg.tag, arg.owner, arg.name, Type.getMethodDescriptor(returnType, *args), arg.isInterface)
                                }
                            } else {
                                val type = Type.getType(arg.desc)
                                val arr = if (type.descriptor.startsWith("[")) type.dimensions else 0
                                val desc = type.descriptor.substring(arr)
                                if (desc in replaces) {
                                    newClasses.addAll(replaces[desc]!!.second)
                                    insn.bsmArgs[i] = Handle(arg.tag, arg.owner, arg.name, "[".repeat(arr) + replaces[desc]!!.first, arg.isInterface)
                                }
                            }
                        } else if (arg is Type) {
                            when (arg.sort) {
                                Type.OBJECT, Type.ARRAY -> {
                                    val arr = if (arg.descriptor.startsWith("[")) arg.dimensions else 0
                                    val desc = arg.descriptor.substring(arr)
                                    if (desc in replaces) {
                                        newClasses.addAll(replaces[desc]!!.second)
                                        insn.bsmArgs[i] = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                                    }
                                }
                                Type.METHOD -> {
                                    val args = arg.argumentTypes
                                    var returnType = arg.returnType
                                    var changed = false
                                    for (i in args.indices) {
                                        if (!args[i].descriptor.contains("L")) continue
                                        val arr = if (args[i].descriptor.startsWith("[")) args[i].dimensions else 0
                                        val desc = args[i].descriptor.substring(arr)
                                        if (desc in replaces) {
                                            newClasses.addAll(replaces[desc]!!.second)
                                            args[i] = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                                            changed = true
                                        }
                                    }
                                    if (returnType.descriptor.contains("L")) {
                                        val arr = if (returnType.descriptor.startsWith("[")) returnType.dimensions else 0
                                        val desc = returnType.descriptor.substring(arr)
                                        if (desc in replaces) {
                                            newClasses.addAll(replaces[desc]!!.second)
                                            returnType = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                                            changed = true
                                        }
                                    }
                                    if (changed) {
                                        insn.bsmArgs[i] = Type.getMethodType(returnType, *args)
                                    }
                                }
                            }
                        }
                    }
                    // split up desc
                    val args = Type.getArgumentTypes(insn.desc)
                    var returnType = Type.getReturnType(insn.desc)
                    var changed = false
                    for (i in args.indices) {
                        val arr = if (args[i].descriptor.startsWith("[")) args[i].dimensions else 0
                        val desc = args[i].descriptor.substring(arr)
                        if (desc in replaces) {
                            newClasses.addAll(replaces[desc]!!.second)
                            args[i] = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                            changed = true
                        }
                    }
                    val arr = if (returnType.descriptor.startsWith("[")) returnType.dimensions else 0
                    val desc = returnType.descriptor.substring(arr)
                    if (desc in replaces) {
                        newClasses.addAll(replaces[desc]!!.second)
                        returnType = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                        changed = true
                    }
                    if (changed) {
                        insn.desc = Type.getMethodDescriptor(returnType, *args)
                    }
                } else if (insn is TypeInsnNode) {
                    val type = Type.getType("L" + insn.desc + ";")
                    val desc = type.descriptor
                    if (desc in replaces) {
                        newClasses.addAll(replaces[desc]!!.second)
                        insn.desc = replaces[desc]!!.first.let { it.substring(1, it.length-1) }
                    }
                } else if (insn is MethodInsnNode) {
                    if ("L" + insn.owner + ";" in replaces) {
                        newClasses.addAll(replaces["L" + insn.owner + ";"]!!.second)
                        insn.owner = replaces["L" + insn.owner + ";"]!!.first.let { it.substring(1, it.length-1) }
                    }
                    // split up desc
                    val args = Type.getArgumentTypes(insn.desc)
                    var returnType = Type.getReturnType(insn.desc)
                    var changed = false
                    for (i in args.indices) {
                        if (!args[i].descriptor.contains("L")) continue
                        val arr = if (args[i].descriptor.startsWith("[")) args[i].dimensions else 0
                        val desc = args[i].descriptor.substring(arr)
                        if (desc in replaces) {
                            newClasses.addAll(replaces[desc]!!.second)
                            args[i] = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                            changed = true
                        }
                    }
                    if (returnType.descriptor.contains("L")) {
                        val arr = if (returnType.descriptor.startsWith("[")) returnType.dimensions else 0
                        val desc = returnType.descriptor.substring(arr)
                        if (desc in replaces) {
                            newClasses.addAll(replaces[desc]!!.second)
                            returnType = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                            changed = true
                        }
                    }
                    if (changed) {
                        insn.desc = Type.getMethodDescriptor(returnType, *args)
                    }
                } else if (insn is MultiANewArrayInsnNode) {
                    val type = Type.getType(insn.desc)
                    val arr = if (type.descriptor.startsWith("[")) type.dimensions else 0
                    val desc = type.descriptor.substring(arr)
                    if (desc in replaces) {
                        newClasses.addAll(replaces[desc]!!.second)
                        insn.desc = "[".repeat(arr) + replaces[desc]!!.first
                    }
                }
            }

            // this is just for the benefit of decompilers
            if (method.localVariables != null)
            for (lv in method.localVariables) {
                val type = Type.getType(lv.desc)
                if (!type.descriptor.contains("L")) continue
                val arr = if (type.descriptor.startsWith("[")) type.dimensions else 0
                val desc = type.descriptor.substring(arr)
                if (desc in replaces) {
                    newClasses.addAll(replaces[desc]!!.second)
                    lv.desc = "[".repeat(arr) + replaces[desc]!!.first
                }
                // fix sig as well
                if (lv.signature != null) {
                    var sig = lv.signature
                    var newSig = ""
                    while (sig.isNotEmpty()) {
                        if (sig.startsWith("L") || sig.startsWith("[")) {
                            val end = sig.first { it == ';' || it == '<' }
                            val type = Type.getType(sig.substringBefore(end) + ";")
                            sig = sig.substringAfter(end)
                            val arr = if (type.descriptor.startsWith("[")) type.dimensions else 0
                            val desc = type.descriptor.substring(arr)
                            if (desc in replaces) {
                                newClasses.addAll(replaces[desc]!!.second)
                                newSig += "[".repeat(arr) + replaces[desc]!!.first.let{ it.substring(0, it.length - 1) } + end
                            } else {
                                newSig += type.descriptor.let{ it.substring(0, it.length - 1) } + end
                            }
                        } else {
                            val next = sig.substringBefore("L")
                            newSig += next
                            sig = sig.substring(next.length)
                        }
                    }
                    lv.signature = newSig
                }
            }
        }
        for (field in classNode.fields.toList()) {
            val type = Type.getType(field.desc)
            val arr = if (type.descriptor.startsWith("[")) type.dimensions else 0
            val desc = type.descriptor.substring(arr)
            if (desc in replaces) {
                newClasses.addAll(replaces[desc]!!.second)
                field.desc = "[".repeat(arr) + replaces[desc]!!.first
            }
        }
        return newClasses
    }
}