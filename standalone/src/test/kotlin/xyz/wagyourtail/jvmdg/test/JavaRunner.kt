package xyz.wagyourtail.jvmdg.test

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.OutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.util.zip.GZIPInputStream
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

object JavaRunner {

    private val os: String by lazy {
        val os = System.getProperty("os.name").toLowerCase()
        when {
            os.contains("darwin") -> "mac"
            os.contains("win") -> "windows"
            os.contains("mac") -> "mac"
            os.contains("nix") || os.contains("nux") || os.contains("aix") -> "linux"
            else -> os
        }
    }

    private val arch: String by lazy {
        val arch = System.getProperty("os.arch")
        when {
            arch.contains("64") -> "x64"
            arch.contains("86") -> "x86"
            arch.contains("arm") -> "arm"
            else -> arch
        }
    }

    fun getJavaHome(vers: JavaVersion): Path {
        val jvmdir = Path.of("./build/test/jvm/${vers.majorVersion}")
        try {
            if (!jvmdir.toFile().exists()) {
                jvmdir.toFile().mkdirs()
                val download = URI.create("https://api.adoptium.net/v3/binary/latest/${vers.majorVersion}/ga/$os/$arch/jre/hotspot/normal/eclipse")
                // download to jvmdir
                TarArchiveInputStream(GZIPInputStream(download.toURL().openStream())).use { archiver ->
                    var entry = archiver.nextTarEntry
                    while (entry != null) {
                        // remove first directory
                        val name = entry.name.split("/").drop(1).joinToString("/")
                        if (entry.isDirectory) {
                            jvmdir.resolve(name).toFile().mkdirs()
                        } else {
                            jvmdir.resolve(name).toFile().outputStream().use { output ->
                                archiver.copyTo(output)
                            }
                        }
                        entry = archiver.nextTarEntry
                    }
                }
            }
        } catch (t: Throwable) {
            jvmdir.deleteIfExists()
            throw t
        }
        return jvmdir
    }

    fun getJava(vers: Int): Path {
        val p = getJavaHome(JavaVersion.fromClassVers(vers)).resolve("bin").resolve("java${if (os == "windows") ".exe" else ""}")
        if (!p.exists()) {
            throw IllegalStateException("java binary not found at $p")
        }
        if (os != "windows") Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rwxr-xr-x"))
        return p
    }

    fun runJarInSubprocess(
        jar: Path,
        vararg args: String,
        mainClass: String? = null,
        classPath: Set<Path> = emptySet(),
        workingDir: Path = Paths.get("."),
        env: Map<String, String> = mapOf(),
        wait: Boolean = true,
        jvmArgs: List<String> = listOf(),
        javaVersion: Int = Opcodes.V1_8,
        output: (line: String) -> Unit = { println(it) },
        error: (line: String) -> Unit = { println(it) },
    ): Int? {
        val javaBin = getJava(javaVersion)
        if (!javaBin.exists()) {
            throw IllegalStateException("java binary not found at $javaBin")
        }
        val processArgs = if (mainClass == null) {
            if (classPath.isEmpty()) {
                arrayOf("-jar", jar.toString())
            } else {
                arrayOf("-cp", classPath.joinToString(File.pathSeparator), "-jar", jar.toString())
            }
        } else {
            arrayOf("-cp", (classPath + jar.toString()).joinToString(File.pathSeparator), mainClass);
        } + args
        val processBuilder = ProcessBuilder(
            javaBin.toString(),
            *jvmArgs.toTypedArray(),
            *processArgs,
        )

        processBuilder.directory(workingDir.toFile())
        processBuilder.environment().putAll(env)

        println("Running: ${processBuilder.command().joinToString(" ")}")
        val process = processBuilder.start()

        val inputStream = process.inputStream
        val errorStream = process.errorStream

        val outputThread = Thread {
            inputStream.copyTo(object : OutputStream() {
                // buffer and write lines
                private var line: String? = null

                override fun write(b: Int) {
                    if (b == '\r'.code) {
                        return
                    }
                    line = if (b == '\n'.code) {
                        output(line!!)
                        ""
                    } else {
                        (line ?: "") + b.toChar()
                    }
                }
            })
        }

        val errorThread = Thread {
            errorStream.copyTo(object : OutputStream() {
                // buffer and write lines
                private var line: String? = null

                override fun write(b: Int) {
                    if (b == '\r'.code) {
                        return
                    }
                    line = if (b == '\n'.code) {
                        error(line!!)
                        ""
                    } else {
                        (line ?: "") + b.toChar()
                    }
                }
            })
        }

        outputThread.start()
        errorThread.start()

        if (wait) {
            process.waitFor()
            outputThread.join()
            errorThread.join()
            return process.exitValue()
        }
        return null
    }

    enum class JavaVersion {
        V1_1,
        V1_2,
        V1_3,
        V1_4,
        V1_5,
        V1_6,
        V1_7,
        V1_8,
        V1_9,
        V10,
        V11,
        V12,
        V13,
        V14,
        V15,
        V16,
        V17,
        V18,
        V19,
        V20;

        val majorVersion: Int
            get() = this.ordinal + 1

        companion object {
            fun fromClassVers(vers: Int): JavaVersion = JavaVersion.values()[vers - 45]
        }
    }
}