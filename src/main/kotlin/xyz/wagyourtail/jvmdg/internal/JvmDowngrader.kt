package xyz.wagyourtail.jvmdg.internal

import org.gradle.api.JavaVersion
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InnerClassNode
import org.objectweb.asm.tree.MethodNode
import xyz.wagyourtail.jvmdg.forEachInZip
import xyz.wagyourtail.jvmdg.internal.mods.ClassReplacer
import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer
import xyz.wagyourtail.jvmdg.internal.mods._9.ModuleRemover
import xyz.wagyourtail.jvmdg.internal.mods._9.PrivateInterfaceFixer
import xyz.wagyourtail.jvmdg.internal.mods._16.RecordRemover
import xyz.wagyourtail.jvmdg.internal.mods._17.SealRemover
import xyz.wagyourtail.jvmdg.jvToOpc
import xyz.wagyourtail.jvmdg.openZipFileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class JvmDowngrader(val inputFile: Path, val stubPkg: String, val versionTarget: JavaVersion) {

    private val neededStubs: MutableMap<String, Pair<MethodNode, Set<Class<*>>>> = mutableMapOf()
    private val neededReplaceClasses = mutableSetOf<String>()

    init {
        if (stubPkg.contains('.')) throw IllegalArgumentException("stubPkg cannot contain '.', use '/' instead.")
    }

    fun downgrade(outputFile: Path) {
        openZipFileSystem(outputFile, mapOf("create" to true, "mutable" to true)).use { output ->
            // copy all files from input to output with downgrading
            // skip META-INF signing
            forEachInZip(inputFile) { name, stream ->
                if (name.startsWith("META-INF")) {
                    if (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA")) {
                        return@forEachInZip
                    }
                }
                val path = output.getPath(name)
                Files.createDirectories(path.parent)
                if (name.endsWith(".class")) {
                    if (versionTarget < JavaVersion.VERSION_1_9) {
                        // remove module-info.class
                        if (name.endsWith("module-info.class")) return@forEachInZip
                    }
                    val classNode = ClassNode()
                    ClassReader(stream).accept(classNode, 0)
                    downgradeClass(classNode)
                    val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
                    classNode.accept(writer)
                    Files.write(path, writer.toByteArray())
                    return@forEachInZip
                }
                Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING)
            }

            // add replace classes
            run {
                val classes = mutableMapOf<String, ClassNode>()
                while (true) {
                    for (className in neededReplaceClasses.map { it.substring(1, it.length - 1) }) {
                        if (classes.contains(className)) continue
                        val node = classes.computeIfAbsent(className) {
                            MethodReplacer.classToNode(
                                Class.forName(
                                    className.replace('/', '.')
                                )
                            )
                        }
                        downgradeClass(node)
                    }
                    if (classes.size == neededReplaceClasses.size) break
                }
                for ((name, node) in classes) {
                    val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
                    node.accept(writer)
                    output.getPath(name).parent?.let { Files.createDirectories(it) }
                    Files.write(output.getPath("$name.class"), writer.toByteArray())
                }
            }

            // add stubs
            run {
                val classes = mutableMapOf<String, ClassNode>()
                val prevClasses = mutableSetOf<String>()
                while (true) {
                    for ((desc, stub) in neededStubs.toList()) {
                        // get class from beginning of desc
                        val className = desc.substring(1, desc.indexOf(';'))
                        if (prevClasses.contains(className)) continue
                        val node = classes.computeIfAbsent(className) {
                            ClassNode().apply {
                                version = jvToOpc(versionTarget)
                                access = Opcodes.ACC_PUBLIC
                                name = className
                                superName = "java/lang/Object"
                            }
                        }
                        // add method
                        node.methods.add(stub.first)
                        // add inner classes
                        for (inner in stub.second) {
                            val innerNode = classes.computeIfAbsent("L" + inner.canonicalName.replace('.', '/') + ";") {
                                MethodReplacer.classToNode(inner).apply {
                                    outerClass = node.name
                                }
                            }
                            node.innerClasses.add(
                                InnerClassNode(
                                    innerNode.name,
                                    node.name,
                                    innerNode.name.split("$").last(),
                                    innerNode.access
                                )
                            )
                        }
                    }
                    // copy needed stubs
                    val parsedStubs = mutableSetOf<String>()
                    parsedStubs.addAll(neededStubs.keys)
                    for (node in classes.values) {
                        if (prevClasses.contains(node.name)) continue
                        downgradeClass(node)
                    }
                    // get new stubs
                    val newStubs = neededStubs.filterKeys { !parsedStubs.contains(it) }
                    if (newStubs.isEmpty()) break
                    // remove classes that changed
                    for (desc in newStubs.keys) {
                        val className = desc.substring(1, desc.indexOf(';'))
                        classes.remove(className)
                    }
                    prevClasses.clear()
                    prevClasses.addAll(classes.keys)
                }
                // write stubs
                for (node in classes.values) {
                    val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
                    node.accept(writer)
                    val path = output.getPath(node.name + ".class")
                    Files.createDirectories(path.parent)
                    Files.write(path, writer.toByteArray())
                }
            }
        }
    }

    private fun downgradeClass(clasNode: ClassNode) {

        // 17
        SealRemover.apply(clasNode, versionTarget)
        // 16
        RecordRemover.apply(clasNode, versionTarget)

        // 9
        ModuleRemover.apply(clasNode, versionTarget)
        PrivateInterfaceFixer.apply(clasNode, versionTarget)

        // 8
//        TODO("lambda transformer")

        val replaceStubs = ClassReplacer(versionTarget).apply(clasNode)
        val stubs = MethodReplacer(versionTarget).apply(clasNode, stubPkg)
        neededStubs.putAll(stubs)
        neededReplaceClasses.addAll(replaceStubs)

        clasNode.version = jvToOpc(versionTarget)
    }

}