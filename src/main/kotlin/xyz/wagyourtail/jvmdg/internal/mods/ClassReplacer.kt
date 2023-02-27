package xyz.wagyourtail.jvmdg.internal.mods

import org.gradle.api.JavaVersion
import org.gradle.internal.impldep.org.objectweb.asm.Type
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer.Companion.availableReplace

class ClassReplacer(val target: JavaVersion) {
    companion object {
        private val availableReplace = mutableMapOf<JavaVersion, MutableMap<String, Pair<String, Boolean>>>()

        fun registerReplace(version: JavaVersion, desc: String, replace: String, newIsStub: Boolean = false) {
            availableReplace.getOrPut(version) { mutableMapOf() }[desc] = replace to newIsStub
        }

        fun registerReplace(version: JavaVersion, class1: Class<*>, class2: Class<*>, newIsStub: Boolean = false) {
            registerReplace(version, "L" + MethodReplacer.resolveClassPath(class1).substringBefore(".class")+ ";", "L" + MethodReplacer.resolveClassPath(class2).substringBefore(".class") + ";", newIsStub)
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
            if (replaces["L${classNode.superName};"]!!.second) {
                newClasses.add(classNode.superName)
            }
            classNode.superName = replaces["L${classNode.superName};"]!!.first.let { it.substring(1, it.length-1) }
        }
        if (classNode.interfaces != null) {
            for (i in classNode.interfaces.indices) {
                if ("L${classNode.interfaces[i]}" in replaces) {
                    if (replaces["L${classNode.interfaces[i]}"]!!.second) {
                        newClasses.add(classNode.interfaces[i])
                    }
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
                    val arr = args[i].dimensions - if (args[i].descriptor.startsWith("[") ) 0 else 1
                    val desc = args[i].descriptor.substring(arr)
                    if (desc in replaces) {
                        if (replaces[desc]!!.second) {
                            newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                        }
                        args[i] = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                        changed = true
                    }
                }
                val arr = returnType.dimensions - if (returnType.descriptor.startsWith("[") ) 0 else 1
                val desc = returnType.descriptor.substring(arr)
                if (desc in replaces) {
                    if (replaces[desc]!!.second) {
                        newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                    }
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
                                    val arr = args[i].dimensions - if (args[i].descriptor.startsWith("[") ) 0 else 1
                                    val desc = args[i].descriptor.substring(arr)
                                    if (desc in replaces) {
                                        if (replaces[desc]!!.second) {
                                            newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                                        }
                                        args[i] = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                                        changed = true
                                    }
                                }
                                val arr = returnType.dimensions - if (returnType.descriptor.startsWith("[") ) 0 else 1
                                val desc = returnType.descriptor.substring(arr)
                                if (desc in replaces) {
                                    if (replaces[desc]!!.second) {
                                        newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                                    }
                                    returnType = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                                    changed = true
                                }
                                if (changed) {
                                    insn.bsmArgs[i] = Handle(arg.tag, arg.owner, arg.name, Type.getMethodDescriptor(returnType, *args), arg.isInterface)
                                }
                            } else {
                                val type = Type.getType(arg.desc)
                                val arr = type.dimensions - if (type.descriptor.startsWith("[") ) 0 else 1
                                val desc = type.descriptor.substring(arr)
                                if (desc in replaces) {
                                    if (replaces[desc]!!.second) {
                                        newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                                    }
                                    insn.bsmArgs[i] = Handle(arg.tag, arg.owner, arg.name, "[".repeat(arr) + replaces[desc]!!.first, arg.isInterface)
                                }
                            }
                        } else if (arg is Type) {
                            when (arg.sort) {
                                Type.OBJECT, Type.ARRAY -> {
                                    val arr = arg.dimensions - if (arg.descriptor.startsWith("[") ) 0 else 1
                                    val desc = arg.descriptor.substring(arr)
                                    if (desc in replaces) {
                                        if (replaces[desc]!!.second) {
                                            newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                                        }
                                        insn.bsmArgs[i] = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                                    }
                                }
                                Type.METHOD -> {
                                    val args = arg.argumentTypes
                                    var returnType = arg.returnType
                                    var changed = false
                                    for (i in args.indices) {
                                        val arr = args[i].dimensions - if (args[i].descriptor.startsWith("[") ) 0 else 1
                                        val desc = args[i].descriptor.substring(arr)
                                        if (desc in replaces) {
                                            if (replaces[desc]!!.second) {
                                                newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                                            }
                                            args[i] = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                                            changed = true
                                        }
                                    }
                                    val arr = returnType.dimensions - if (returnType.descriptor.startsWith("[") ) 0 else 1
                                    val desc = returnType.descriptor.substring(arr)
                                    if (desc in replaces) {
                                        if (replaces[desc]!!.second) {
                                            newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                                        }
                                        returnType = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                                        changed = true
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
                        val arr = args[i].dimensions - if (args[i].descriptor.startsWith("[") ) 0 else 1
                        val desc = args[i].descriptor.substring(arr)
                        if (desc in replaces) {
                            if (replaces[desc]!!.second) {
                                newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                            }
                            args[i] = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                            changed = true
                        }
                    }
                    val arr = returnType.dimensions - if (returnType.descriptor.startsWith("[") ) 0 else 1
                    val desc = returnType.descriptor.substring(arr)
                    if (desc in replaces) {
                        if (replaces[desc]!!.second) {
                            newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                        }
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
                        if (replaces[desc]!!.second) {
                            newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                        }
                        insn.desc = replaces[desc]!!.first.let { it.substring(1, it.length-1) }
                    }
                } else if (insn is MethodInsnNode) {
                    if ("L" + insn.owner + ";" in replaces) {
                        if (replaces["L" + insn.owner + ";"]!!.second) {
                            newClasses.add(replaces["L" + insn.owner + ";"]!!.first.let { it.substring(1, it.length-1) })
                        }
                        insn.owner = replaces["L" + insn.owner + ";"]!!.first.let { it.substring(1, it.length-1) }
                    }
                    // split up desc
                    val args = Type.getArgumentTypes(insn.desc)
                    var returnType = Type.getReturnType(insn.desc)
                    var changed = false
                    for (i in args.indices) {
                        val arr = args[i].dimensions - if (args[i].descriptor.startsWith("[") ) 0 else 1
                        val desc = args[i].descriptor.substring(arr)
                        if (desc in replaces) {
                            if (replaces[desc]!!.second) {
                                newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                            }
                            args[i] = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                            changed = true
                        }
                    }
                    val arr = returnType.dimensions - if (returnType.descriptor.startsWith("[") ) 0 else 1
                    val desc = returnType.descriptor.substring(arr)
                    if (desc in replaces) {
                        if (replaces[desc]!!.second) {
                            newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                        }
                        returnType = Type.getType("[".repeat(arr) + replaces[desc]!!.first)
                        changed = true
                    }
                    if (changed) {
                        insn.desc = Type.getMethodDescriptor(returnType, *args)
                    }
                } else if (insn is MultiANewArrayInsnNode) {
                    val type = Type.getType(insn.desc)
                    val arr = type.dimensions - if (type.descriptor.startsWith("[") ) 0 else 1
                    val desc = type.descriptor.substring(arr)
                    if (desc in replaces) {
                        if (replaces[desc]!!.second) {
                            newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                        }
                        insn.desc = "[".repeat(arr) + replaces[desc]!!.first
                    }
                }
            }
        }
        for (field in classNode.fields.toList()) {
            val type = Type.getType(field.desc)
            val arr = type.dimensions - if (type.descriptor.startsWith("[") ) 0 else 1
            val desc = type.descriptor.substring(arr)
            if (desc in replaces) {
                if (replaces[desc]!!.second) {
                    newClasses.add(replaces[desc]!!.first.let { it.substring(1, it.length-1) })
                }
                field.desc = "[".repeat(arr) + replaces[desc]!!.first
            }
        }
        return newClasses
    }
}