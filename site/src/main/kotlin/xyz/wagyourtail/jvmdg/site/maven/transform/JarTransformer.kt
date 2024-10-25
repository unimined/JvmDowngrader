package xyz.wagyourtail.jvmdg.site.maven.transform

import io.github.oshai.kotlinlogging.KotlinLogging
import org.objectweb.asm.Opcodes
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader
import xyz.wagyourtail.jvmdg.site.maven.Cache
import xyz.wagyourtail.jvmdg.site.maven.MavenClient
import xyz.wagyourtail.jvmdg.site.maven.lock.StringKeyLock
import java.nio.file.Path
import kotlin.io.path.createParentDirectories

object JarTransformer {
    val LOGGER = KotlinLogging.logger { }
    val lock = StringKeyLock.LockEntry()

    fun transform(jar: Path, majorVersion: Int, path: String, targetJar: Path) {
        LOGGER.info { "Adding $path to Queue" }
        lock.incrementAndLock {
            LOGGER.info { "Queue Length: $it" }
        }
        try {
            LOGGER.info { "Transforming $path to $majorVersion" }
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