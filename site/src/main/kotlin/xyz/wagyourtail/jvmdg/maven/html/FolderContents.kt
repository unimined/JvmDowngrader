package xyz.wagyourtail.jvmdg.maven.html

import io.ktor.server.html.*
import kotlinx.html.*

class FolderContents(
    val majorVersion: Int,
    val folder: String,
    val files: List<String>
) : Template<HTML> {

    override fun HTML.apply() {
        head {
            title("JvmDowngrader Maven Mirror: ${majorVersion}/$folder")
        }
        body {
            h1 {
                +folder
            }
            for (file in files) {
                p {
                    a(href = "/maven/$majorVersion/$folder$file") {
                        +file
                    }
                }
            }
        }
    }

}