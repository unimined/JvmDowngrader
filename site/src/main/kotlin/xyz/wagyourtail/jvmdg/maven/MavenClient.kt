package xyz.wagyourtail.jvmdg.maven

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.DefaultDependencyNode
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.metadata.DefaultMetadata
import org.eclipse.aether.metadata.Metadata
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.MetadataRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.repository.AuthenticationBuilder
import xyz.wagyourtail.jvmdg.maven.impl.ConsoleRepositoryListener
import xyz.wagyourtail.jvmdg.maven.impl.ConsoleTransferListener
import java.io.File
import java.io.InputStream

object MavenClient {

    val repo: RepositorySystem = run {
        val locator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
        locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)

        locator.setErrorHandler(object : DefaultServiceLocator.ErrorHandler() {

            override fun serviceCreationFailed(type: Class<*>?, impl: Class<*>?, exception: Throwable?) {
                exception?.printStackTrace()
            }

        })

        locator.getService(RepositorySystem::class.java)
    }

    fun remoteRepository(url: String, user: String? = null, pass: String? = null): RemoteRepository {
        val builder = RemoteRepository.Builder("custom", "default", url)
        if (user != null && pass != null) {
            builder.setAuthentication(AuthenticationBuilder().addUsername(user).addPassword(pass).build())
        }
        return builder.build()
    }

    fun centralRepository() = remoteRepository("https://repo1.maven.org/maven2/")

    fun createSession(system: RepositorySystem, local: LocalRepository): DefaultRepositorySystemSession {
        val session = MavenRepositorySystemUtils.newSession()

        session.localRepositoryManager = system.newLocalRepositoryManager(session, local)

        session.setTransferListener(ConsoleTransferListener())
        session.setRepositoryListener(ConsoleRepositoryListener())

        return session
    }

    val session = createSession(repo, LocalRepository(Cache.cache.resolve("orig").toFile()))
    val repositories = Settings.mavens.map { remoteRepository(it.second) }


    fun resolve(artifact: DefaultArtifact): Pair<File, List<File>> {
        val request = ArtifactRequest()
        request.artifact = artifact
        request.repositories = repositories

        val depRequest = DependencyRequest()
        depRequest.collectRequest = CollectRequest(Dependency(artifact, "compile"), repositories)

        val result = repo.resolveArtifact(session, request).artifact.file
        val deps = repo.resolveDependencies(session, depRequest).artifactResults.map { it.artifact.file }.filter { it != result }

        return result to deps
    }

    fun resolveArtifact(artifact: DefaultArtifact): File {
        val request = ArtifactRequest(artifact, repositories, "")

        return repo.resolveArtifact(session, request).artifact.file
    }

    fun resolveDeps(artifact: DefaultArtifact): List<File> {
        val depRequest = DependencyRequest()
        depRequest.collectRequest = CollectRequest(Dependency(artifact, "compile"), repositories)

        return repo.resolveDependencies(session, depRequest).artifactResults.map { it.artifact.file }
    }

    fun pathToArtifact(path: String): DefaultArtifact {
        // cut up path to turn into artifact
        val folder = path.substringBeforeLast("/")
        val artifactFolder = folder.substringBeforeLast("/")
        val versionNumber = folder.substringAfterLast("/")
        val artifactName = artifactFolder.substringAfterLast("/")
        val group = artifactFolder.substringBeforeLast("/")
        val classifier = path.substringAfterLast("/").let {
            if (!it.startsWith("$artifactName-$versionNumber-")) {
                null
            } else {
                it.removePrefix("$artifactName-$versionNumber-").substringBeforeLast(".")
            }
        }
        val ext = path.substringAfterLast(".")

        return DefaultArtifact(
            group.replace('/', '.'),
            artifactName,
            classifier,
            ext,
            versionNumber
        )
    }

    fun get(path: String): File {
        return resolveArtifact(pathToArtifact(path))
    }

    fun getDependencies(path: String): List<File> {
        return resolveDeps(pathToArtifact(path))
    }

}
