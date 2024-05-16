package xyz.wagyourtail.gradle.ctsym

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.gradle.api.JavaVersion
import org.gradle.api.internal.ConventionTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import xyz.wagyourtail.gradle.*
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipFile
import kotlin.io.path.*

abstract class GenerateCtSymTask : ConventionTask() {

    @Optional
    @OutputFile
    var ctSym = temporaryDir.resolve("jvmdg").resolve("ct.sym")

    @get:Input
    @get:Optional
    abstract val lowerVersion: Property<JavaVersion>

    @get:Input
    abstract val upperVersion: Property<JavaVersion>

    init {
        outputs.upToDateWhen { ctSym.exists() }
        lowerVersion.set(JavaVersion.VERSION_1_6)
        upperVersion.set(JavaVersion.VERSION_22)
    }

    private fun ZipArchiveOutputStream.write(ci: ClassInfo) {
        val cName = ci.cls.name
        val jv = ci.versions.sorted()
        val outPth = jv.joinToString("") { it.sym() } + "/" + ci.mod + "/" + cName + ".sig"
        // write file
        val writer = ClassWriter(0)
        ci.cls.version = jv.first().toOpcode()
        ci.cls.accept(writer)
        val ze = ZipArchiveEntry(outPth)
        ze.time = CONSTANT_TIME_FOR_ZIP_ENTRIES
        putArchiveEntry(ze)
        writer.toByteArray().let { write(it, 0, it.size) }
        closeArchiveEntry()
    }

    private fun JavaToolchainService.getJavaHome(version: JavaVersion): Path {
        val toolchain = project.extensions.getByType(JavaToolchainService::class.java)
        val compiler = toolchain.compilerFor { spec: JavaToolchainSpec ->
            spec.languageVersion.set(JavaLanguageVersion.of(version.majorVersion))
        }
        return compiler.get().metadata.installationPath.asFile.toPath()
    }

    @TaskAction
    @OptIn(ExperimentalPathApi::class)
    fun run() {
        val packageByMod = mutableMapOf<String, String>()

        val toolchain = project.extensions.getByType(JavaToolchainService::class.java)


        ZipArchiveOutputStream(ctSym.toPath().outputStream(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)).use { zos ->
            val prevJava = mutableMapOf<String, ClassInfo>()
            for (java in (JavaVersion.VERSION_1_6..JavaVersion.VERSION_22).reversed()) {
                val home = toolchain.getJavaHome(java)
                project.logger.lifecycle("[ct.sym] Processing $java at $home")
                for (path in home.walk().filter { it.exists() && it.isRegularFile() && it.extension in setOf("jar", "jmod") }) {
                    if (path.extension == "jar" && path.nameWithoutExtension != "rt") continue
                    // for each jar/jmod list its contents
                    project.logger.info("[ct.sym]   Found ${path.fileName}")
                    val modName = path.fileName.toString().removeSuffix(".jmod")
                    ZipFile(path.toFile()).use { zip ->
                        for (entry in zip.entries().toList().sortedBy { it.name }) {
                            if (entry.name.endsWith(".class") && !entry.isDirectory) {
                                val cls = zip.getInputStream(entry).readClass()
                                if (cls.name in prevJava) {
                                    if (prevJava[cls.name]!!.cls.matches(cls)) {
                                        prevJava[cls.name] =
                                            prevJava[cls.name]!!.copy(versions = prevJava[cls.name]!!.versions + java)
                                    } else {
                                        // write to fs path
                                        val ci = prevJava[cls.name]!!
                                        zos.write(ci)
                                        prevJava[cls.name] = ClassInfo(ci.mod, cls, setOf(java))
                                    }
                                } else {
                                    val pkgName = cls.name.substringBeforeLast('/')
                                    val clsModName = if (path.fileName.extension == "jmod") {
                                        packageByMod[pkgName] = modName
                                        modName
                                    } else {
                                        packageByMod[pkgName] ?: modName
                                    }
                                    prevJava[cls.name] = ClassInfo(clsModName, cls, setOf(java))
                                }
                            }
                        }
                    }
                }
                for ((cls, ci) in prevJava.entries.toList()) {
                    if (ci.versions.contains(java)) continue
                    zos.write(ci)
                    prevJava.remove(cls)
                }
            }
            for ((_, ci) in prevJava) {
                zos.write(ci)
            }
        }
    }

    data class ClassInfo(val mod: String, val cls: ClassNode, val versions: Set<JavaVersion>)
}