package xyz.wagyourtail.jvmdg.site.maven.html

import io.ktor.server.html.*
import kotlinx.html.*
import xyz.wagyourtail.jvmdg.site.maven.Settings

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
                +"https://jvmdowngrader.wagyourtail.xyz/maven/"
            }
            p {
                +"Packages are prefixed with "
                code {
                    +"jvmdg-\${majorVersion}."
                }
                +" so for example, to get a java 9 version of jvmdowngrader-java-api, you would use "
                br {}
                code {
                    +"jvmdg-9.xyz.wagyourtail.jvmdowngrader:jvmdowngrader-java-api:0.7.1"
                }
                br {}
                +"If the artifact's artifactId starts with its groupId, the artifactId is also transformed in this way."
                br {}
                +"This does not transform transitive dependencies, so you will have to explicitly depend on the downgraded version, "
                +"and exclude the original from the transitive dependencies."
            }
            p {
                +"You do also need to depend on jvmdowngrader's api. this can actually be done through the mirror as well."
                br {}
                +"Or you can depend on one of the "
                a(href = "https://repo1.maven.org/maven2/xyz/wagyourtail/jvmdowngrader/jvmdowngrader-java-api/0.7.1/") {
                    +"pre-downgraded artifacts on central"
                }
                +"."
            }
            code {
                +"xyz.wagyourtail.jvmdowngrader:jvmdowngrader-java-api:0.7.1:downgraded-8"
            }
            p {
                +"It is "
                b {
                    +"strongly"
                }
                +". such as: "
                br {}
                code {
                    +"xyz.wagyourtail.jvmdowngrader:jvmdowngrader-java-api:0.7.1:downgraded-8"
                }
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