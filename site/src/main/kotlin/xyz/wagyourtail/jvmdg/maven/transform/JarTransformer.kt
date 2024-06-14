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

    fun transform(jar: InputStream, majorVersion: Int, path: String, targetJar: Path) {
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
            val classifier =
                path.substringAfterLast("/").removePrefix("$artifactName-$versionNumber-").removeSuffix(".jar")
            val dependencies = resolveJarAndDependencies(group, artifactName, versionNumber, classifier).iterator()
            val inputJar = dependencies.next()
            targetJar.createParentDirectories()

            ZipDowngrader.downgradeZip(
                majorVersionToOpc(majorVersion),
                inputJar,
                dependencies.asSequence().map { it.toUri().toURL() }.toSet(),
                targetJar
            )
        } finally {
            lock.unlockAndDecrement()
        }
    }

    private fun resolveJarAndDependencies(group: String, artifactName: String, version: String, classifier: String?, ext: String = "jar", resolved: MutableList<Path> = mutableListOf()): Stream<Path> {
        LOGGER.info { "Resolving ${group.replace('/', '.')}:$artifactName:$version" }
        val artifact = "$group/$artifactName/$version/$artifactName-$version"
        val jar = "$artifact.$ext"
//        val module = "$artifact.module"
        val pom = "$artifact.pom"

        val jarPath = Cache.getOrPut("orig/$jar") { path ->
            MavenClient.get(jar)?.use { stream ->
                path.createParentDirectories()
                path.outputStream().use {
                    it.write(stream.readBytes())
                }
            }
        }
        if (jarPath in resolved) return Stream.empty()
        if (jarPath == null) throw IllegalArgumentException("Failed to get jar: $jar")
        resolved.add(jarPath)

        return if (MavenClient.head(pom)) {
            Stream.concat(Stream.of(jarPath), getDependenciesFromPom(pom, resolved))
        } else {
            Stream.of(jarPath)
        }
    }

    private fun getDependenciesFromPom(pom: String, resolved: MutableList<Path>): Stream<Path> {
        LOGGER.info { "Resolving dependencies from $pom" }
        val dbf = DocumentBuilderFactory.newInstance()
        val db = dbf.newDocumentBuilder()
        val doc = db.parse(MavenClient.get(pom)!!)
        val dependencies = doc.getElementsByTagName("dependencies").item(0) as? Element
        val deps = dependencies?.getElementsByTagName("dependency")

        val propertiesMap = mutableMapOf<String, String>()
        val properties = (doc.getElementsByTagName("properties").item(0) as? Element)?.childNodes
        if (properties != null) {
            for (i in 0 until properties.length) {
                val property = properties.item(i) as? Element ?: continue
                propertiesMap[property.tagName] = property.textContent
            }
        }
        // fill project.version, project.groupId, project.artifactId
        propertiesMap["project.version"] = doc.getElementsByTagName("version").item(0).textContent
        propertiesMap["project.groupId"] = doc.getElementsByTagName("groupId").item(0).textContent
        propertiesMap["project.artifactId"] = doc.getElementsByTagName("artifactId").item(0).textContent

        if (deps == null) return Stream.empty()
        return IntStream.range(0, deps.length).mapToObj { i ->
            val dependency = deps.item(i) as Element
            val group = dependency.getElementsByTagName("groupId").item(0).textContent.replace(".", "/").fillProperties(propertiesMap)
            val artifact = dependency.getElementsByTagName("artifactId").item(0).textContent.fillProperties(propertiesMap)
            var version = dependency.getElementsByTagName("version").item(0)?.textContent?.fillProperties(propertiesMap)
            val classifier = dependency.getElementsByTagName("classifier").item(0)?.textContent?.fillProperties(propertiesMap)
            val ext = (dependency.getElementsByTagName("type").item(0)?.textContent ?: "jar").fillProperties(propertiesMap)
            val scope = dependency.getElementsByTagName("scope").item(0)?.textContent

            if (scope == "test") {
                return@mapToObj null
            }

            if (version == null) {
                // retrieve maven-metadata.xml to get latest version
                val metadata = MavenClient.get("$group/$artifact/maven-metadata.xml")!!
                val versioning = db.parse(metadata).documentElement.getElementsByTagName("versioning").item(0) as Element
                val latest = versioning.getElementsByTagName("latest").item(0).textContent
                version = latest!!
            }

            resolveJarAndDependencies(group, artifact, version, classifier, ext, resolved)
        }.filter(Objects::nonNull).flatMap { it }
    }

    val regex = Pattern.compile("\\$\\{([^}]+)}")

    fun String.fillProperties(map: Map<String, String>): String {
        val matcher = regex.matcher(this)
        val sb = StringBuffer()
        while (matcher.find()) {
            val key = matcher.group(1)
            val value = map[key]
            matcher.appendReplacement(sb, value ?: throw IllegalArgumentException("Missing property: $key"))
        }
        matcher.appendTail(sb)
        return sb.toString()
    }

//    fun getDependenciesFromModule(module: String): Stream<Path> {
//        val json = Json.parseToJsonElement(MavenClient.get(module)!!.readBytes().decodeToString()).jsonObject
//        val variant = json["variants"]?.jsonArray?.first {
//            it.jsonObject["name"]?.jsonPrimitive?.content == "apiElements"
//        }
//    }

    private fun majorVersionToOpc(version: Int): Int {
        return when (version) {
            1 -> Opcodes.V1_1
            else -> Opcodes.V1_2 + version - 2
        }
    }

}