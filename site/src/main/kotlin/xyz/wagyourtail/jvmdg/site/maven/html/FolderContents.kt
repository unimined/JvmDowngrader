package xyz.wagyourtail.jvmdg.site.maven.html

import io.ktor.server.html.*
import kotlinx.html.*

class FolderContents(
    val majorVersion: Int,
    val folder: String,
    val files: List<String>
) : Template<HTML> {

    override fun HTML.apply() {
        head {
            title("JvmDowngrader Maven Mirror: jvmdg-${majorVersion}/$folder")
        }
        body {
            h1 {
                +folder
            }
            for (file in files) {
                if (file.startsWith(folder.removeSuffix("/").replace("/", "."))) {
                    p {
                        a(href = "/maven/jvmdg-$majorVersion/${folder}jvmdg-$majorVersion.$file") {
                            +"jvmdg-$majorVersion.$file"
                        }
                    }
                }
                p {
                    a(href = "/maven/jvmdg-$majorVersion/$folder$file") {
                        +file
                    }
                }
            }
        }
    }

}