package xyz.wagyourtail.jvmdg.maven

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import xyz.wagyourtail.jvmdg.maven.html.About
import xyz.wagyourtail.jvmdg.maven.html.Maven
import xyz.wagyourtail.jvmdg.maven.html.FolderContents
import xyz.wagyourtail.jvmdg.maven.transform.JarTransformer
import xyz.wagyourtail.jvmdg.maven.transform.ModuleTransformer
import xyz.wagyourtail.jvmdg.maven.transform.PomTransformer
import kotlin.io.path.inputStream
import kotlin.time.Duration.Companion.days

private val LOGGER = KotlinLogging.logger {  }

fun main() {
    embeddedServer(Netty, port = Settings.port) {
        install(AutoHeadResponse)
//        install(CachingHeaders) {
//            options { call, _ ->
//                if (call.request.path().startsWith("mirror")) {
//                    CachingOptions(CacheControl.MaxAge((1.days.inWholeSeconds * 365).toInt()))
//                } else {
//                    CachingOptions(CacheControl.NoCache(visibility = CacheControl.Visibility.Public))
//                }
//            }
//        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
            }
            status(HttpStatusCode.NotFound) { call, status ->
                call.respondText(text = "404: Page Not Found", status = status)
            }
        }
        routing {
            get(Regex("/(?:index.html)?")) {
                call.respondHtmlTemplate(About()) {}
            }

            get(Regex("/maven(?:/(?:index.html)?)?")) {
                call.respondHtmlTemplate(Maven()) {}
            }
            // list existing mirrored content
            get(Regex("maven/(?<major>\\d+)/(?<mirrorPath>(?:.+/)?)")) {
                val major = call.parameters["major"]!!.toInt()
                val mirrorPath = call.parameters["mirrorPath"]!!
                if (Settings.blacklist.any { mirrorPath.startsWith(it) }) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    val files = Cache.listContents("${major}/${mirrorPath}")
                    if (files != null) {
                        call.respondHtmlTemplate(FolderContents(major, mirrorPath, files)) {}
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            // maven pom transformer
            get(Regex("maven/(?<major>\\d+)/(?<mirrorPath>.+\\.pom)")) {
                val major = call.parameters["major"]!!.toInt()
                val mirrorPath = call.parameters["mirrorPath"]!!
                if (Settings.blacklist.any { mirrorPath.startsWith(it) }) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    val content = Cache.getOrPut("${major}/${mirrorPath}") { path ->
                        MavenClient.get(mirrorPath)?.let {
                            PomTransformer.transform(it, major, path)
                        }
                    }
                    if (content != null) {
                        call.respondOutputStream {
                            content.inputStream().use { it.copyTo(this) }
                            headers {
                                cacheControl(CacheControl.MaxAge(7.days.inWholeSeconds.toInt()))
                            }
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            // gradle module transformer
            get(Regex("maven/(?<major>\\d+)/(?<mirrorPath>.+\\.module)")) {
                val major = call.parameters["major"]!!.toInt()
                val mirrorPath = call.parameters["mirrorPath"]!!
                if (Settings.blacklist.any { mirrorPath.startsWith(it) }) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    val content = Cache.getOrPut("${major}/${mirrorPath}") { path ->
                        MavenClient.get(mirrorPath)?.let {
                            ModuleTransformer.transform(it, major, path)
                        }
                    }
                    if (content != null) {
                        call.respondOutputStream {
                            content.inputStream().use { it.copyTo(this) }
                            headers {
                                cacheControl(CacheControl.MaxAge(7.days.inWholeSeconds.toInt()))
                            }
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            // maven metadata, don't cache
            get(Regex("maven/(?<major>\\d+)/(?<mirrorPath>.+\\.xml)")) {
                val mirrorPath = call.parameters["mirrorPath"]!!
                if (Settings.blacklist.any { mirrorPath.startsWith(it) }) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    val content = MavenClient.get(mirrorPath)
                    if (content != null) {
                        call.respondOutputStream {
                            content.copyTo(this)
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            // don't include signature or checksums
            get(Regex("maven/(?<major>\\d+)/(?<mirrorPath>.+\\.(?:md5|sha1|sha256|sha512))")) {
                call.respond(HttpStatusCode.NotFound)
            }
            get(Regex("maven/(?<major>\\d+)/(?<mirrorPath>.+\\.asc)")) {
                call.respond(HttpStatusCode.NotFound)
            }

            // jar transformer
            get(Regex("maven/(?<major>\\d+)/(?<mirrorPath>.+\\.jar)")) {
                try {
                    val major = call.parameters["major"]!!.toInt()
                    val mirrorPath = call.parameters["mirrorPath"]!!
                    if (Settings.blacklist.any { mirrorPath.startsWith(it) }) {
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        val content = Cache.getOrPut("${major}/${mirrorPath}") { path ->
                            MavenClient.get(mirrorPath)?.let {
                                JarTransformer.transform(it, major, mirrorPath, path)
                            }
                        }
                        if (content != null) {
                            call.respondOutputStream {
                                content.inputStream().use { it.copyTo(this) }
                                headers {
                                    cacheControl(CacheControl.MaxAge(7.days.inWholeSeconds.toInt()))
                                }
                            }
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                } catch (e: Exception) {
                    LOGGER.error(e) { "Failed to transform jar" }
                    call.respondHtml(status = HttpStatusCode.InternalServerError) {
                        body {
                            h1 { +"Failed to transform jar" }
                            pre { +e.stackTraceToString() }
                        }
                        headers {
                            cacheControl(CacheControl.MaxAge(1.days.inWholeSeconds.toInt()))
                        }
                    }
                }
            }

        }
    }.start(wait = true)
}