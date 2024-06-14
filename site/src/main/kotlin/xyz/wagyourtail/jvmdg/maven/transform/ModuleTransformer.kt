package xyz.wagyourtail.jvmdg.maven.transform

import kotlinx.serialization.json.*
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.outputStream

object ModuleTransformer {

    fun transform(module: InputStream, version: Int, target: Path) {
        val content = Json.parseToJsonElement(module.readBytes().decodeToString()).jsonObject
        val json = buildJsonObject {
            for (entry in content) {
                if (entry.key == "variant") {
                    put(entry.key, buildJsonArray {
                        for (variant in entry.value.jsonArray) {
                            add(buildJsonObject {
                                for (entry in variant.jsonObject) {
                                    if (entry.key == "attributes") {
                                        put(entry.key, buildJsonObject {
                                            for (entry in entry.value.jsonObject) {
                                                if (entry.key == "org.gradle.jvm.version") {
                                                    put(entry.key, version)
                                                } else {
                                                    put(entry.key, entry.value)
                                                }
                                            }
                                        })
                                    } else {
                                        put(entry.key, entry.value)
                                    }
                                }
                            })
                        }
                    })
                } else {
                    put(entry.key, entry.value)
                }
            }
        }
        target.createParentDirectories()
        target.outputStream().use {
            it.write(Json.encodeToString(JsonObject.serializer(), json).toByteArray())
        }
    }

}