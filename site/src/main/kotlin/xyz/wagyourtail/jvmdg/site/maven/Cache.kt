package xyz.wagyourtail.jvmdg.site.maven

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import xyz.wagyourtail.jvmdg.site.maven.lock.StringKeyLock
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.measureTime

@OptIn(ExperimentalPathApi::class)
object Cache {
    private val LOGGER = KotlinLogging.logger { }
    private val lock = StringKeyLock()
    val cache = Path.of("./cache").toAbsolutePath().also {
        if (!it.exists()) {
            it.createDirectories()
        }
    }

    private val accessTimesFile = Path.of("accessTimes.json")
    private val accessTimes: MutableMap<String, Long> = Collections.synchronizedMap(run {
        if (accessTimesFile.exists()) {
            accessTimesFile.inputStream().use {
                TreeMap(Json.decodeFromString(serializer<Map<String, Long>>(), it.readAllBytes().decodeToString()))
            }
        } else {
            TreeMap(cache.walk().filter { it.isRegularFile() }.associate {
                it.relativeTo(cache).toString() to it.getLastModifiedTime().toMillis()
            })
        }
    })

    init {
        Thread(Cache::pruner).also {
            it.isDaemon = true
            it.name = "Cache Pruner"
        }.start()
    }

    fun updateAccessTime(path: String) {
        accessTimes[path] = System.currentTimeMillis()
        synchronized(accessTimes) {
            accessTimesFile.writeText(Json.encodeToString(serializer<Map<String, Long>>(), accessTimes))
        }
    }

    fun recordPath(path: Path): Path {
        val key = path.relativeTo(cache).toString()
        updateAccessTime(key)
        return path
    }

    fun listContents(path: String): List<String>? {
        val pth = cache.resolve(path)
        if (!pth.exists()) {
            return null
        }
        return Files.list(pth).sorted { o1, o2 ->
            if (o1.isDirectory() && !o2.isDirectory()) -1
            else if (!o1.isDirectory() && o2.isDirectory()) 1
            else o1.fileName.toString().compareTo(o2.fileName.toString())
        }.map {
            buildString {
                append(it.fileName.toString())
                if (it.isDirectory()) {
                    append('/')
                }
            }
        }.toList()
    }

    private fun putStream(path: String, content: InputStream): Path {
        updateAccessTime(path)
        val file = cache.resolve(path)
        file.createParentDirectories()
        content.use {
            file.outputStream().use { os ->
                it.copyTo(os)
            }
        }
        return file
    }

    fun getOrPut(path: String, getter: (Path) -> Unit): Path? {
        lock.locked(path) {
            val target = cache.resolve(path)
            if (target.exists()) {
                updateAccessTime(path)
                return cache.resolve(path)
            }
            getter(target)
            if (target.exists()) {
                updateAccessTime(path)
                return target
            }
            return null
        }
    }

    private fun pruner() {
        while (true) {
            LOGGER.info { "Beginning cache prune task." }
            val time = measureTime {
                // calculate folder size
                // delete files older than maxAge
                // delete oldest accessed files until folder size is less than maxSize
                val oldestAllowed = System.currentTimeMillis() - Settings.maxAge.inWholeMilliseconds
                val files = cache.walk().toList()
                val size = files.sumOf { it.fileSize() }
                LOGGER.info { "Current cache size: $size bytes of ${Settings.maxSize} allowed" }
                LOGGER.info { "Current cache file count: ${files.size}" }
                // search for expired files
                var expiredCount = 0
                for (file in files.filter { it.isRegularFile() }) {
                    val key = file.relativeTo(cache).toString()
                    val lastAccess = accessTimes[key] ?: oldestAllowed
                    if (lastAccess < oldestAllowed) {
                        file.deleteIfExists()
                        accessTimes.remove(key)
                        expiredCount++
                    }
                }
                if (expiredCount > 0) LOGGER.info { "Deleted $expiredCount expired files." }

                // remove remaining files until size is less than maxSize
                val maxSize = Settings.maxSize
                val current = System.currentTimeMillis()
                val filesToDelete = files.filter { it.isRegularFile() }
                    .sortedBy { accessTimes[it.relativeTo(cache).toString()] ?: current }
                var currentSize = size
                var deletedCount = 0
                for (file in filesToDelete) {
                    if (currentSize < maxSize) {
                        break
                    }
                    currentSize -= file.fileSize()
                    file.deleteIfExists()
                    accessTimes.remove(file.relativeTo(cache).toString())
                    deletedCount++
                }
                if (deletedCount > 0) LOGGER.info { "Deleted $deletedCount files to reduce cache size to $currentSize bytes." }

                // remove empty folders
                files.reversed().filter { it.isDirectory() && it.listDirectoryEntries().isEmpty() }.forEach {
                    it.deleteIfExists()
                }

                synchronized(accessTimes) {
                    accessTimesFile.writeText(Json.encodeToString(serializer<Map<String, Long>>(), accessTimes))
                }
            }
            LOGGER.info { "Cache prune task completed in $time." }
            Thread.sleep(1.hours.inWholeMilliseconds)
        }
    }

}