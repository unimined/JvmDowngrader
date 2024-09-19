package xyz.wagyourtail.jvmdg.site.maven.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.eclipse.aether.RepositoryEvent
import org.eclipse.aether.RepositoryListener

class ConsoleRepositoryListener: RepositoryListener {
    val LOGGER = KotlinLogging.logger { }

    override fun artifactDescriptorInvalid(p0: RepositoryEvent) {
        LOGGER.error { "Invalid artifact descriptor for: ${p0.artifact}: ${p0.exception.message}" }
    }

    override fun artifactDescriptorMissing(p0: RepositoryEvent) {
        LOGGER.error { "Missing artifact descriptor for: ${p0.artifact}" }
    }

    override fun metadataInvalid(p0: RepositoryEvent) {
        LOGGER.error { "Invalid metadata for: ${p0.artifact}: ${p0.exception.message}" }
    }

    override fun artifactResolving(p0: RepositoryEvent) {
        LOGGER.debug { "Resolving artifact: ${p0.artifact} from ${p0.repository}" }
    }

    override fun artifactResolved(p0: RepositoryEvent) {
        LOGGER.debug { "Resolved artifact: ${p0.artifact} from ${p0.repository}" }
    }

    override fun metadataResolving(p0: RepositoryEvent) {
        LOGGER.debug { "Resolving metadata: ${p0.artifact} from ${p0.repository}" }
    }

    override fun metadataResolved(p0: RepositoryEvent) {
        LOGGER.debug { "Resolved metadata: ${p0.artifact} from ${p0.repository}" }
    }

    override fun artifactDownloading(p0: RepositoryEvent) {
        LOGGER.debug { "Downloading artifact: ${p0.artifact} from ${p0.repository}" }
    }

    override fun artifactDownloaded(p0: RepositoryEvent) {
        LOGGER.debug { "Downloaded artifact: ${p0.artifact} from ${p0.repository}" }
    }

    override fun metadataDownloading(p0: RepositoryEvent) {
        LOGGER.debug { "Downloading metadata: ${p0.artifact} from ${p0.repository}" }
    }

    override fun metadataDownloaded(p0: RepositoryEvent) {
        LOGGER.debug { "Downloaded metadata: ${p0.artifact} from ${p0.repository}" }
    }

    override fun artifactInstalling(p0: RepositoryEvent) {
        LOGGER.debug { "Installing artifact: ${p0.artifact} to ${p0.file}" }
    }

    override fun artifactInstalled(p0: RepositoryEvent) {
        LOGGER.debug { "Installed artifact: ${p0.artifact} to ${p0.file}" }
    }

    override fun metadataInstalling(p0: RepositoryEvent) {
        LOGGER.debug { "Installing metadata: ${p0.artifact} to ${p0.file}" }
    }

    override fun metadataInstalled(p0: RepositoryEvent) {
        LOGGER.debug { "Installed metadata: ${p0.artifact} to ${p0.file}" }
    }

    override fun artifactDeploying(p0: RepositoryEvent) {
        LOGGER.debug { "Deploying artifact: ${p0.artifact} to ${p0.repository}" }
    }

    override fun artifactDeployed(p0: RepositoryEvent) {
        LOGGER.debug { "Deployed artifact: ${p0.artifact} to ${p0.repository}" }
    }

    override fun metadataDeploying(p0: RepositoryEvent) {
        LOGGER.debug { "Deploying metadata: ${p0.artifact} to ${p0.repository}" }
    }

    override fun metadataDeployed(p0: RepositoryEvent) {
        LOGGER.debug { "Deployed metadata: ${p0.artifact} to ${p0.repository}" }
    }

}