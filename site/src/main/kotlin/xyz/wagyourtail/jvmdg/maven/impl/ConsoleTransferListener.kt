package xyz.wagyourtail.jvmdg.maven.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.eclipse.aether.transfer.TransferEvent
import org.eclipse.aether.transfer.TransferListener

class ConsoleTransferListener : TransferListener {
    private val LOGGER = KotlinLogging.logger {  }

    override fun transferInitiated(p0: TransferEvent) {
        val message = if (p0.requestType == TransferEvent.RequestType.PUT) "Uploading" else "Downloading"
        LOGGER.info { "$message: ${p0.resource.repositoryUrl}${p0.resource.resourceName}" }
    }

    override fun transferStarted(p0: TransferEvent) {
        val message = if (p0.requestType == TransferEvent.RequestType.PUT) "Uploading" else "Downloading"
        val resource = p0.resource
        LOGGER.debug { "$message started: ${resource.repositoryUrl}${resource.resourceName}" }
    }

    override fun transferProgressed(p0: TransferEvent) {
        val message = if (p0.requestType == TransferEvent.RequestType.PUT) "Uploading" else "Downloading"
        val resource = p0.resource
        LOGGER.trace { "$message: ${resource.resourceName} - ${p0.transferredBytes * 100 / resource.contentLength}%\n" }
    }

    override fun transferCorrupted(p0: TransferEvent) {
        val message = if (p0.requestType == TransferEvent.RequestType.PUT) "Uploading" else "Downloading"
        LOGGER.error(p0.exception) { "$message corrupted: ${p0.resource.repositoryUrl}${p0.resource.resourceName}" }
    }

    override fun transferSucceeded(p0: TransferEvent) {
        val message = if (p0.requestType == TransferEvent.RequestType.PUT) "Uploading" else "Downloading"
        val resource = p0.resource
        LOGGER.debug { "$message succeeded: ${resource.repositoryUrl}${resource.resourceName}" }
    }

    override fun transferFailed(p0: TransferEvent) {
        val message = if (p0.requestType == TransferEvent.RequestType.PUT) "Uploading" else "Downloading"
        val resource = p0.resource
        LOGGER.error(p0.exception) { "$message failed: ${resource.repositoryUrl}${resource.resourceName}" }
    }
}