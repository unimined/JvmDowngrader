@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.jvm.tasks.Jar
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.*
import xyz.wagyourtail.jvmdg.gradle.*
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.LazyMutable
import xyz.wagyourtail.jvmdg.util.defaultedMapOf
import java.net.URLClassLoader
import java.nio.file.StandardOpenOption
import java.util.*
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

abstract class ShadeAPI @Inject constructor(@Internal val jvmdg: JVMDowngraderExtension): Jar() {

    @get:Input
    @get:Optional
    var downgradeTo by FinalizeOnRead(JavaVersion.VERSION_1_8)


    @get:Internal
    var sourceSet: SourceSet by FinalizeOnRead(LazyMutable {
        project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
    })

    @get:Input
    @get:Optional
    var shadePath by FinalizeOnRead(LazyMutable { archiveBaseName.get().replace(Regex("[ -]"), "_") + "/jvmdg/api" })

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
        val tempOutput = project.projectDir.resolve("build/tmp").resolve(name).resolve("shaded.jar")
        tempOutput.deleteIfExists()
        val dependencies =
            URLClassLoader((sourceSet.compileClasspath.files.map { it.toURI().toURL() } + inputFile.get().asFile.toURI()
                .toURL()).toTypedArray())
        val downgradedApi = jvmdg.downgradedApi[downgradeTo]
        val apiClasses = mutableMapOf<Type, ClassNode>()
        forEachInZip(downgradedApi.toPath()) { name, stream ->
            if (name.endsWith(".class")) {
                val reader = ClassReader(stream)
                val node = ClassNode()
                reader.accept(node, 0)
                apiClasses[Type.getObjectType(node.name)] = node
            }
        }
        val apiParts = mutableMapOf<ExtendedType, ApiPart>()
        val rootParts = mutableSetOf<ApiPart>()
        val inputNodes = mutableMapOf<Type, ClassNode>()
        forEachInZip(inputFile.get().asFile.toPath()) { name, stream ->
            if (name.endsWith(".class")) {
                val reader = ClassReader(stream)
                val node = ClassNode()
                reader.accept(node, 0)
                inputNodes[Type.getObjectType(node.name)] = node
            }
        }
        for (node in inputNodes.values) {
            if (apiParts.containsKey(ExtendedType(Type.getObjectType(node.name)))) {
                rootParts.add(apiParts.getValue(ExtendedType(Type.getObjectType(node.name))))
            } else {
                rootParts += getApiParts(node, apiClasses, apiParts) {
                    when (it) {
                        in apiClasses -> {
                            apiClasses.getValue(it)
                        }

                        in inputNodes -> {
                            inputNodes.getValue(it)
                        }

                        else -> {
                            val path = it.internalName + ".class"
                            val url = dependencies.getResource(path)!!
                            val reader2 = ClassReader(url.openStream())
                            val node2 = ClassNode()
                            reader2.accept(node2, 0)
                            node2
                        }
                    }
                }
            }
        }
        val flattenedParts = rootParts.flatMap { it.dependencies }.flatMap { it.getAll() }.toSet()
        project.logger.lifecycle("Shading ${flattenedParts.size} items")
        // print parts in tree
        rootParts.forEach { printPart(it, 0) }
        // shade
        val strippedApi = defaultedMapOf<Type, ClassNode> {
            val inputNode = apiClasses.getValue(it)
            val outputNode = ClassNode()
            // copy everything but methods/fields
            inputNode.accept(outputNode)
            outputNode.methods.clear()
            outputNode.fields.clear()
            outputNode
        }
        val strippedClasses = mutableSetOf<Type>()
        for (part in flattenedParts) {
            if (!part.contains(".")) {
                // class
                val inputNode = apiClasses.getValue(Type.getObjectType(part))
                val type = Type.getObjectType(part)
                // copy only non-static methods/fields
                for (method in inputNode.methods) {
                    if (method.access and Opcodes.ACC_STATIC == 0) {
                        strippedApi[type].methods.add(method)
                    }
                }
                for (field in inputNode.fields) {
                    if (field.access and Opcodes.ACC_STATIC == 0) {
                        strippedApi[type].fields.add(field)
                    }
                }
                strippedClasses.add(type)
            } else if (part.contains("(")) {
                // method
                val inputNode = apiClasses.getValue(Type.getObjectType(part.substringBefore(".")))
                val name = part.substringAfter(".").substringBeforeLast("(")
                val desc = "(" + part.substringAfterLast("(")
                // copy this method
                val method = inputNode.methods.first { it.name == name && it.desc == desc }
                strippedApi[Type.getObjectType(inputNode.name)].methods.add(method)
                strippedClasses.add(Type.getObjectType(inputNode.name))
            } else {
                // field
                val inputNode = apiClasses.getValue(Type.getObjectType(part.substringBefore(".")))
                val name = part.substringAfter(".")
                // copy this field
                val field = inputNode.fields.first { it.name == name }
                strippedApi[Type.getObjectType(inputNode.name)].fields.add(field)
                strippedClasses.add(Type.getObjectType(inputNode.name))
            }
        }
        // remap
        val remapper = SimpleRemapper(strippedClasses.map { it.internalName }.associateWith {
            "$shadePath/$it"
        })
        val outputNodes = mutableMapOf<String, ClassNode>()
        for (node in strippedApi.values) {
            val outputNode = ClassNode()
            node.accept(ClassRemapper(outputNode, remapper))
            outputNodes[outputNode.name] = outputNode
        }
        for (node in inputNodes) {
            val outputNode = ClassNode()
            node.value.accept(ClassRemapper(outputNode, remapper))
            outputNodes[outputNode.name] = outputNode
        }
        openZipFileSystem(tempOutput.toPath(), mapOf("create" to true)).use { fs ->
            for (node in outputNodes.values) {
                val path = fs.getPath(node.name + ".class")
                path.parent.createDirectories()
                path.outputStream(StandardOpenOption.CREATE).use { os ->
                    val writer = ASMClassWriter(0) {
                        if (outputNodes.containsKey(it)) {
                            outputNodes.getValue(it).superName
                        } else {
                            dependencies.getResource("$it.class")?.openStream().use { stream ->
                                val reader = ClassReader(stream)
                                val superNode = ClassNode()
                                reader.accept(superNode, 0)
                                superNode.superName
                            }
                        }
                    }
                    node.accept(writer)
                    os.write(writer.toByteArray())
                }
            }
            forEachInZip(inputFile.get().asFile.toPath()) { name, stream ->
                if (!name.endsWith(".class")) {
                    val path = fs.getPath(name)
                    path.parent?.createDirectories()
                    path.outputStream(StandardOpenOption.CREATE).use { os ->
                        stream.copyTo(os)
                    }
                }
            }
        }
        from(project.zipTree(tempOutput))
        copy()
    }

    private fun printPart(part: ApiPart, depth: Int) {
        project.logger.debug(" ".repeat(depth) + part.desc)
        part.dependencies.forEach { printPart(it, depth + 1) }
    }

    private fun getApiParts(
        node: ClassNode,
        apiClasses: Map<Type, ClassNode>,
        apiParts: MutableMap<ExtendedType, ApiPart>,
        getNode: (Type) -> ClassNode
    ): Set<ApiPart> {
        project.logger.info("Getting api parts for ${node.name}")
        val thisClassParts = mutableSetOf<ApiPart>()
        val classPart = ApiPart(node.name, mutableSetOf())
        thisClassParts.add(classPart)
        val type = Type.getObjectType(node.name)
        apiParts[ExtendedType(type)] = classPart
        // determine dependencies
        // superclass
        val superType = Type.getObjectType(node.superName)
        if (superType in apiClasses) {
            if (ExtendedType(superType) !in apiParts) {
                getApiParts(getNode(superType), apiClasses, apiParts, getNode)
            }
            classPart.dependencies.add(apiParts.getValue(ExtendedType(superType)))
        }
        // interfaces
        for (intf in node.interfaces) {
            val intfType = Type.getObjectType(intf)
            if (intfType in apiClasses) {
                if (ExtendedType(intfType) !in apiParts) {
                    getApiParts(getNode(intfType), apiClasses, apiParts, getNode)
                }
                classPart.dependencies.add(apiParts.getValue(ExtendedType(intfType)))
            }
        }
        // methods
        for (method in node.methods) {
            if (method.access and Opcodes.ACC_STATIC != 0) {
                apiParts[ExtendedType(type, method.name + method.desc)] =
                    ApiPart("${node.name}.${method.name}${method.desc}", mutableSetOf())
                thisClassParts.add(apiParts.getValue(ExtendedType(type, method.name + method.desc)))
            }
        }
        // fields
        for (field in node.fields) {
            val fieldType = Type.getType(field.desc)
            // non-static fields
            if (field.access and Opcodes.ACC_STATIC == 0) {
                if (fieldType in apiClasses) {
                    if (ExtendedType(fieldType) !in apiParts) {
                        getApiParts(getNode(fieldType), apiClasses, apiParts, getNode)
                    }
                    classPart.dependencies.add(apiParts.getValue(ExtendedType(fieldType)))
                }
            } else {
                // static fields, (don't get added to classPart)
                val dep: MutableSet<ApiPart> = if (Type.getType(field.desc) in apiClasses) {
                    if (ExtendedType(fieldType) !in apiParts) {
                        getApiParts(getNode(fieldType), apiClasses, apiParts, getNode)
                    }
                    mutableSetOf(apiParts.getValue(ExtendedType(fieldType)))
                } else {
                    mutableSetOf()
                }
                // do get added as their own
                apiParts[ExtendedType(type, field.name)] = ApiPart("${node.name}.${field.name}", dep)
                thisClassParts.add(apiParts.getValue(ExtendedType(type, field.name)))
                // check if has <clinit> and add that as a dependency if in api
                if (field.value == null && Type.getObjectType(node.name) in apiClasses) {
                    val clinit = apiParts[ExtendedType(type, "<clinit>()V")]
                    if (clinit != null) {
                        clinit.dependencies.add(
                            apiParts.getValue(ExtendedType(type, field.name))
                        )
                        apiParts.getValue(ExtendedType(type, field.name)).dependencies.add(clinit)
                    }
                }

            }
        }
        // method contents
        for (method in node.methods) {
            val deps = mutableSetOf<ApiPart>()
            val methodType = Type.getMethodType(method.desc)
            // return type
            if (methodType.returnType in apiClasses) {
                if (ExtendedType(methodType.returnType) !in apiParts) {
                    getApiParts(getNode(methodType.returnType), apiClasses, apiParts, getNode)
                }
                deps.add(apiParts.getValue(ExtendedType(methodType.returnType)))
            }
            // parameters
            for (param in methodType.argumentTypes) {
                if (param in apiClasses) {
                    if (ExtendedType(param) !in apiParts) {
                        getApiParts(getNode(param), apiClasses, apiParts, getNode)
                    }
                    deps.add(apiParts.getValue(ExtendedType(param)))
                }
            }
            // contents
            for (insn in method.instructions) {
                if (insn is TypeInsnNode) {
                    val insnType = Type.getObjectType(insn.desc)
                    if (insnType in apiClasses) {
                        if (ExtendedType(insnType) !in apiParts) {
                            getApiParts(getNode(insnType), apiClasses, apiParts, getNode)
                        }
                        deps.add(apiParts.getValue(ExtendedType(insnType)))
                    }
                } else if (insn is FieldInsnNode) {
                    val fieldType = Type.getObjectType(insn.owner)
                    if (fieldType in apiClasses) {
                        if (ExtendedType(fieldType) !in apiParts) {
                            getApiParts(getNode(fieldType), apiClasses, apiParts, getNode)
                        }
                        // determine if field is static by seeing if it's in the apiParts
                        if (apiParts.containsKey(ExtendedType(fieldType, insn.name))) {
                            deps.add(apiParts.getValue(ExtendedType(fieldType, insn.name)))
                        } else {
                            if (insn.opcode == Opcodes.GETSTATIC || insn.opcode == Opcodes.PUTSTATIC) {
                                project.logger.warn("Static field ${insn.owner}.${insn.name} is not in the apiParts???")
                            }
                            deps.add(apiParts.getValue(ExtendedType(fieldType)))
                        }
                    }
                } else if (insn is MethodInsnNode) {
                    val invokeMethodType = Type.getObjectType(insn.owner)
                    if (invokeMethodType in apiClasses) {
                        if (ExtendedType(invokeMethodType) !in apiParts) {
                            getApiParts(getNode(invokeMethodType), apiClasses, apiParts, getNode)
                        }
                        // determine if method is static by seeing if it's in the apiParts
                        if (apiParts.containsKey(ExtendedType(invokeMethodType, insn.name + insn.desc))) {
                            deps.add(apiParts.getValue(ExtendedType(invokeMethodType, insn.name + insn.desc)))
                        } else {
                            if (insn.opcode == Opcodes.INVOKESTATIC) {
                                project.logger.warn("Static method ${insn.owner}.${insn.name}${insn.desc} is not in the apiParts???")
                            }
                            deps.add(apiParts.getValue(ExtendedType(invokeMethodType)))
                        }
                    }
                } else if (insn is InvokeDynamicInsnNode) {
                    val bsmType = Type.getObjectType(insn.bsm.owner)
                    if (bsmType in apiClasses) {
                        if (ExtendedType(bsmType) !in apiParts) {
                            getApiParts(getNode(bsmType), apiClasses, apiParts, getNode)
                        }
                        // determine if method is static by seeing if it's in the apiParts
                        if (apiParts.containsKey(ExtendedType(bsmType, insn.bsm.name + insn.bsm.desc))) {
                            deps.add(apiParts.getValue(ExtendedType(bsmType, insn.bsm.name + insn.bsm.desc)))
                        } else {
                            deps.add(apiParts.getValue(ExtendedType(bsmType)))
                        }
                    }
                    for (arg in insn.bsmArgs) {
                        if (arg is Type) {
                            if (arg in apiClasses) {
                                if (ExtendedType(arg) !in apiParts) {
                                    getApiParts(getNode(arg), apiClasses, apiParts, getNode)
                                }
                                deps.add(apiParts.getValue(ExtendedType(arg)))
                            }
                        } else if (arg is Handle) {
                            val ownerType = Type.getObjectType(arg.owner)
                            if (ownerType in apiClasses) {
                                if (ExtendedType(ownerType) !in apiParts) {
                                    getApiParts(getNode(ownerType), apiClasses, apiParts, getNode)
                                }
                                when (arg.tag) {
                                    Opcodes.H_GETFIELD, Opcodes.H_GETSTATIC, Opcodes.H_PUTFIELD, Opcodes.H_PUTSTATIC -> {
                                        if (apiParts.containsKey(ExtendedType(ownerType, arg.name))) {
                                            deps.add(apiParts.getValue(ExtendedType(ownerType, arg.name)))
                                        } else {
                                            deps.add(apiParts.getValue(ExtendedType(ownerType)))
                                        }
                                    }

                                    Opcodes.H_INVOKEINTERFACE, Opcodes.H_INVOKESPECIAL, Opcodes.H_INVOKESTATIC, Opcodes.H_INVOKEVIRTUAL -> {
                                        if (apiParts.containsKey(ExtendedType(ownerType, arg.name + arg.desc))) {
                                            deps.add(apiParts.getValue(ExtendedType(ownerType, arg.name + arg.desc)))
                                        } else {
                                            deps.add(apiParts.getValue(ExtendedType(ownerType)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // add method dependencies
            if (method.access and Opcodes.ACC_STATIC == 0) {
                deps.remove(classPart)
                classPart.dependencies.addAll(deps)
            } else {
                apiParts.getValue(ExtendedType(type, method.name + method.desc)).dependencies.addAll(deps)
            }
        }
        return thisClassParts
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

        override fun hashCode(): Int {
            return desc.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is ApiPart && other.desc == desc
        }

        override fun toString(): String {
            return "ApiPart(desc='$desc')"
        }

    }

    data class ExtendedType(val type: Type, val fieldOrMethodName: String? = null)

}
