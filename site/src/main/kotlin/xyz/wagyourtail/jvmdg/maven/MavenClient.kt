package xyz.wagyourtail.jvmdg.maven

import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object MavenClient {

    val client = HttpClient.newBuilder()
        .connectTimeout(5.seconds.toJavaDuration())
        .build()

    fun createRequest(url: String) = HttpRequest.newBuilder(URI.create(url))
        .headers("User-Agent", "JvmDowngrader Maven Mirror/1.0 <admin@wagyourtail.xyz>")

    fun get(path: String): InputStream? {
        for (maven in Settings.mavens) {
            val request = createRequest(maven.second + path)
            val response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream())
            if (response.statusCode() == 200) {
                return response.body()
            }
        }
        return null
    }

    fun head(path: String): Boolean {
        for (maven in Settings.mavens) {
            val request = createRequest(maven.second + path)
            val response = client.send(request.method("HEAD", BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding())
            if (response.statusCode() == 200) {
                return true
            }
        }
        return false
    }

}