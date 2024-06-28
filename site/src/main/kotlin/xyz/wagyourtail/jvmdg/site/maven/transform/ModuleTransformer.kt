package xyz.wagyourtail.jvmdg.site.maven.transform

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
                when (entry.key) {
                    "variants" -> {
                        put(entry.key, buildJsonArray {
                            for (variant in entry.value.jsonArray) {
                                add(buildJsonObject {
                                    for (varkey in variant.jsonObject) {
                                        if (varkey.key == "attributes") {
                                            put(varkey.key, buildJsonObject {
                                                for (attr in varkey.value.jsonObject) {
                                                    if (attr.key == "org.gradle.jvm.version") {
                                                        put(attr.key, version)
                                                    } else {
                                                        put(attr.key, attr.value)
                                                    }
                                                }
                                            })
                                            // TODO: dependency transforms?
            //                                    } else if (varkey.key == "dependencies") {
                                        } else {
                                            put(varkey.key, varkey.value)
                                        }
                                    }
                                })
                            }
                        })
                    }
                    "component" -> {
                        put(entry.key, buildJsonObject {
                            for ((key, value) in entry.value.jsonObject) {
                                if (key == "group") {
                                    put(key, "jvmdg-$version.${value.jsonPrimitive.content}")
                                } else if (key == "module" && entry.value.jsonObject["group"]?.jsonPrimitive?.content?.let { value.jsonPrimitive.content.startsWith(it) } == true) {
                                    put(key, "jvmdg-$version.${value.jsonPrimitive.content}")
                                } else {
                                    put(key, value)
                                }
                            }
                        })
                    }
                    else -> {
                        put(entry.key, entry.value)
                    }
                }
            }
        }
        target.createParentDirectories()
        target.outputStream().use {
            it.write(Json.encodeToString(JsonObject.serializer(), json).toByteArray())
        }
    }

}