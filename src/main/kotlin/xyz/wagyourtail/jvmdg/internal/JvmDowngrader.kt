package xyz.wagyourtail.jvmdowngrader.internal

import org.gradle.api.JavaVersion
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import xyz.wagyourtail.jvmdowngrader.forEachInZip
import xyz.wagyourtail.jvmdowngrader.internal.downgraders.MethodReplacer
import xyz.wagyourtail.jvmdowngrader.internal.downgraders.ModuleRemover
import xyz.wagyourtail.jvmdowngrader.internal.downgraders.PrivateInterfaceFixer
import xyz.wagyourtail.jvmdowngrader.internal.downgraders.RecordRemover
import xyz.wagyourtail.jvmdowngrader.openZipFileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class JvmDowngrader(val inputFile: Path, val stubPkg: String, val versionTarget: JavaVersion) {

    private val neededStubs: MutableMap<String, MethodNode> = mutableMapOf()

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

            // add stubs
            val classes = mutableMapOf<String, ClassNode>()
            for ((desc, stub) in neededStubs) {
                // get class from beginning of desc
                val className = desc.substring(1, desc.indexOf(';'))
                val node = classes.getOrPut(className) {
                    ClassNode().apply {
                        version = Opcodes.V1_8 //TODO
                        access = Opcodes.ACC_PUBLIC
                        name = className
                        superName = "java/lang/Object"
                    }
                }
                node.methods.add(stub)
            }
        }
    }

    private fun downgradeClass(clasNode: ClassNode) {
        if (versionTarget < JavaVersion.VERSION_16) {
            RecordRemover.apply(clasNode)
        }

        if (versionTarget < JavaVersion.VERSION_1_9) {
            ModuleRemover.apply(clasNode)
            PrivateInterfaceFixer.apply(clasNode)
        }

        if (versionTarget < JavaVersion.VERSION_1_8) {
            TODO("lambda transformer")
        }

        val stubs = MethodReplacer.apply(clasNode, versionTarget, stubPkg)
        neededStubs.putAll(stubs)

        clasNode.version = Opcodes.V1_8 //TODO dynamic
    }

}