@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.LazyMutable
import javax.inject.Inject

abstract class ShadeAPI @Inject constructor(@Internal val jvmdg: JVMDowngraderExtension) : Jar() {

    @get:Input
    @get:Optional
    var downgradeTo by FinalizeOnRead(JavaVersion.VERSION_1_8)

    @get:Input
    @get:Optional
    var shadePath by FinalizeOnRead(LazyMutable { archiveBaseName.get().replace(Regex("[ -]"), "_") + ".jvmdg.api" })

    /**
     * must already be downgraded
     */
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    init {
        group = "JVMDowngrader"
        description = "Downgrades the jar to the specified version"
    }

    @TaskAction
    fun doShade() {
        val downgradedApi = jvmdg.downgradedApi[downgradeTo]


        TODO()
    }

    fun getApiParts(node: ClassNode, apiClasses: Map<String, ClassNode>, apiParts: MutableMap<String, ApiPart>, getNode: (String) -> ClassNode) {
        val classPart = ApiPart(node.name, mutableSetOf())
        apiParts[node.name] = classPart
        // determine dependencies
        // superclass
        if (node.superName in apiClasses) {
            if (node.superName !in apiParts) {
                getApiParts(getNode(node.superName), apiClasses, apiParts, getNode)
            }
            classPart.dependencies.add(apiParts[node.superName]!!)
        }
        // interfaces
        for (intf in node.interfaces) {
            if (intf in apiClasses) {
                if (intf !in apiParts) {
                    getApiParts(getNode(intf), apiClasses, apiParts, getNode)
                }
                classPart.dependencies.add(apiParts[intf]!!)
            }
        }
        // fields
        for (field in node.fields) {
            // non-static fields
            if (field.access and Opcodes.ACC_STATIC == 0) {
                if (field.desc in apiClasses) {
                    if (field.desc !in apiParts) {
                        getApiParts(getNode(field.desc), apiClasses, apiParts, getNode)
                    }
                    classPart.dependencies.add(apiParts[field.desc]!!)
                }
            } else {
                // static fields, (don't get added to classPart)
                val dep: MutableSet<ApiPart> = if (field.desc in apiClasses) {
                    if (field.desc !in apiParts) {
                        getApiParts(getNode(field.desc), apiClasses, apiParts, getNode)
                    }
                    mutableSetOf(apiParts[field.desc]!!)
                } else {
                    mutableSetOf()
                }
                // do get added as their own
                apiParts["${node.name}.${field.name}"] = ApiPart("${node.name}.${field.name}", dep)
            }
        }
        // methods
        for (method in node.methods) {
            val deps = mutableSetOf<ApiPart>()
            val type = Type.getMethodType(method.desc)
            // return type
            if (type.returnType.internalName in apiClasses) {
                if (type.returnType.internalName !in apiParts) {
                    getApiParts(getNode(type.returnType.internalName), apiClasses, apiParts, getNode)
                }
                deps.add(apiParts[type.returnType.internalName]!!)
            }
            // parameters
            for (param in type.argumentTypes) {
                if (param.internalName in apiClasses) {
                    if (param.internalName !in apiParts) {
                        getApiParts(getNode(param.internalName), apiClasses, apiParts, getNode)
                    }
                    deps.add(apiParts[param.internalName]!!)
                }
            }
            // contents
            for (insn in method.instructions) {
                if (insn is FieldInsnNode) {
                    if (insn.owner in apiClasses) {
                        if (insn.owner !in apiParts) {
                            getApiParts(getNode(insn.owner), apiClasses, apiParts, getNode)
                        }
                        // determine if field is static by seeing if it's in the apiParts
                        if (apiParts.containsKey("${insn.owner}.${insn.name}")) {
                            deps.add(apiParts["${insn.owner}.${insn.name}"]!!)
                        } else {
                            deps.add(apiParts[insn.owner]!!)
                        }
                    }
                } else if (insn is MethodInsnNode) {
                    if (insn.owner in apiClasses) {
                        if (insn.owner !in apiParts) {
                            getApiParts(getNode(insn.owner), apiClasses, apiParts, getNode)
                        }
                        // determine if method is static by seeing if it's in the apiParts
                        if (apiParts.containsKey("${insn.owner}.${insn.name}${insn.desc}")) {
                            deps.add(apiParts["${insn.owner}.${insn.name}${insn.desc}"]!!)
                        } else {
                            deps.add(apiParts[insn.owner]!!)
                        }
                    }
                } else if (insn is InvokeDynamicInsnNode) {
                    if (insn.bsm.owner in apiClasses) {
                        if (insn.bsm.owner !in apiParts) {
                            getApiParts(getNode(insn.bsm.owner), apiClasses, apiParts, getNode)
                        }
                        // determine if method is static by seeing if it's in the apiParts
                        if (apiParts.containsKey("${insn.bsm.owner}.${insn.bsm.name}${insn.bsm.desc}")) {
                            deps.add(apiParts["${insn.bsm.owner}.${insn.bsm.name}${insn.bsm.desc}"]!!)
                        } else {
                            deps.add(apiParts[insn.bsm.owner]!!)
                        }
                    }
                    for (arg in insn.bsmArgs) {
                        if (arg is Type) {
                            if (arg.internalName in apiClasses) {
                                if (arg.internalName !in apiParts) {
                                    getApiParts(getNode(arg.internalName), apiClasses, apiParts, getNode)
                                }
                                deps.add(apiParts[arg.internalName]!!)
                            }
                        } else if (arg is Handle) {

                        }
                    }
                }
            }

        }


    }

    data class ApiPart(val desc: String, val dependencies: MutableSet<ApiPart>) {

        fun getAll(): Set<String> {
            val all = mutableSetOf<String>()
            all.add(desc)
            for (dep in dependencies) {
                all.addAll(dep.getAll())
            }
            return all
        }

    }



}
