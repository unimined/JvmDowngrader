package xyz.wagyourtail.jvmdg.maven.transform

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Semaphore
import org.objectweb.asm.Opcodes
import org.w3c.dom.Element
import xyz.wagyourtail.jvmdg.compile.PathDowngrader
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader
import xyz.wagyourtail.jvmdg.maven.Cache
import xyz.wagyourtail.jvmdg.maven.MavenClient
import xyz.wagyourtail.jvmdg.maven.lock.StringKeyLock
import java.io.InputStream
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern
import java.util.stream.IntStream
import java.util.stream.Stream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.createParentDirectories
import kotlin.io.path.outputStream

object JarTransformer {
    val LOGGER = KotlinLogging.logger {  }
    val lock = StringKeyLock.LockEntry()

    fun transform(jar: Path, majorVersion: Int, path: String, targetJar: Path) {
        LOGGER.info { "Adding $path to Queue" }
        lock.incrementAndLock {
            LOGGER.info { "Queue Length: $it" }
        }
        try {
            LOGGER.info { "Transforming $path to $majorVersion" }
            // retrieve dependencies from module or pom
            val folder = path.substringBeforeLast("/")
            val artifactFolder = folder.substringBeforeLast("/")
            val versionNumber = folder.substringAfterLast("/")
            val artifactName = artifactFolder.substringAfterLast("/")
            val group = artifactFolder.substringBeforeLast("/")
            val classifier = path.substringAfterLast("/").removePrefix("$artifactName-$versionNumber-").removeSuffix(".jar")
            targetJar.createParentDirectories()

            val deps = MavenClient.getDependencies(path).filter { it != jar }
            for (dep in deps) {
                Cache.recordPath(dep.toPath())
            }

            ZipDowngrader.downgradeZip(
                majorVersionToOpc(majorVersion),
                jar,
                deps.map { it.toURI().toURL() }.toSet(),
                targetJar
            )
        } finally {
            lock.unlockAndDecrement()
        }
    }

    private fun majorVersionToOpc(version: Int): Int {
        return when (version) {
            1 -> Opcodes.V1_1
            else -> Opcodes.V1_2 + version - 2
        }
    }

}