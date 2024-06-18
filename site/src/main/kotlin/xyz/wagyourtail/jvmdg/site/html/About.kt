package xyz.wagyourtail.jvmdg.site.html

import io.ktor.server.html.*
import kotlinx.html.*
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

class About : Template<HTML> {
    companion object {
        val flavor = GFMFlavourDescriptor()
        val src = About::class.java.getResourceAsStream("/README.md")!!.bufferedReader().lines().skip(3).toList().joinToString("\n")
        val parsed = MarkdownParser(flavor).buildMarkdownTreeFromString(src)
        val html = HtmlGenerator(src, parsed, flavor, false).generateHtml()
    }

    override fun HTML.apply() {
        head {
            title("JvmDowngrader")
            style {
                unsafe {
                    +"""
                        body {
                            max-width: 800px;
                            margin:0 auto;
                        }
                        h1 {
                            text-align: center;
                        }
                        .user-del {
                            text-decoration: line-through;
                        }
                    """.trimIndent()
                }
            }
            link(rel = "stylesheet", href = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/default.min.css")
            script(src = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js") {}
            script(src = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/gradle.min.js") {}
            script {
                defer = true
                unsafe {
                    +"""
                        hljs.highlightAll();
                    """.trimIndent()
                }
            }
        }
        body {
            h1 {
                +"JvmDowngrader"
            }
            p {
                +"Downgrades modern java bytecode to older versions. at either compile or runtime."
            }
            p {
                a(href = "https://github.com/unimined/JvmDowngrader") {
                    +"Github"
                }
            }
            p {
                a(href = "/maven") {
                    +"Downgrading maven mirror"
                }
            }
            h2 {
                +"About"
            }
            unsafe {
                +html
            }
        }
    }

}