package xyz.wagyourtail.jvmdg.maven.html

import io.ktor.server.html.*
import kotlinx.html.*
import xyz.wagyourtail.jvmdg.maven.Settings

class Maven : Template<HTML> {

    override fun HTML.apply() {
        head {
            title("JvmDowngrader Maven")
            style {
                +"""
                    body {
                        max-width: 800px;
                        margin:0 auto;
                    }
                    h1 {
                        text-align: center;
                    }
                    
                """.trimIndent()
            }
        }
        body {
            h1 {
                +"JvmDowngrader Maven"
            }
            p {
                +"This is a maven mirror that downgrades the bytecode of the jars requested from it. "
                +"This is done by using the "
                a(href = "https://github.com/unimined/JvmDowngrader") {
                    +"JvmDowngrader"
                }
                +" library to modify the bytecode of the jars before they are sent to the client."
            }
            p {
                +"To use this mirror, simply add the maven to your project's repositories: "
            }
            code {
                +"https://jvmdowngrader.wagyourtail.xyz/maven/8/"
            }
            p {
                +"Obviously you can replace the 8 with other java major versions if needed."
                br {}
            }
            p {
                +"It is "
                b {
                    +"strongly"
                }
                + " recommended that you use "
                a(href = "https://docs.gradle.org/current/userguide/declaring_repositories.html#sec:repository-content-filtering") {
                    +"repository content filtering"
                }
                +" to only use this mirror for the specific artifacts that you need downgraded."
            }
            p {
                +"The following mavens are curently mirrored: "
                ul {
                    for (maven in Settings.mavens) {
                        li {
                            a(href = maven.second) {
                                +maven.first
                            }
                        }
                    }
                }
            }
            p {
                +"If you would like to add your maven to this mirror, if there are any issues, "
                br {}
                +"or if you would like your content removed and blacklisted, "
                br {}
                +"please contact me at "
                a(href = "mailto:${Settings.contact}") {
                    +Settings.contact
                }
                +", open an issue on the "
                a(href = "https://github.com/unimined/JvmDowngrader") {
                    +"JvmDowngrader Github"
                }
                +" or message me on "
                a(href = "https://discord.gg/P6W58J8") {
                    +"Discord"
                }
                +"."
            }
            p {
                +"Please note that not all maven adding requests will be accepted, "
                +"and that I reserve the right to blacklist any artifact for any reason."
            }
            p {
                +"The source code for this mirror can be found on "
                a(href = "https://github.com/unimined/JvmDowngrader/tree/lts/site") {
                    +"the JvmDowngrader Github"
                }
                +"."
            }
            p {
                +"THIS SERVICE  IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, "
                +"INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND "
                +"NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR "
                +"OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN "
                +"CONNECTION WITH THE SERVICE OR THE USE OR OTHER DEALINGS IN THE SERVICE."
            }
        }
    }

}