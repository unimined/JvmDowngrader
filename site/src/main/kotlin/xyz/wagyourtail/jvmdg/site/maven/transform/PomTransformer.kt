package xyz.wagyourtail.jvmdg.site.maven.transform

import java.io.InputStream
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.io.path.createParentDirectories
import kotlin.io.path.outputStream

object PomTransformer {

    fun transform(pom: InputStream, version: Int, cachePath: Path) {
        val dbf = DocumentBuilderFactory.newInstance()
        val db = dbf.newDocumentBuilder()
        val doc = db.parse(pom)

        // find maven.compiler.target
        val target = doc.getElementsByTagName("maven.compiler.target").item(0)
        if (target != null) {
            target.textContent = if (version < 9) {
                "1.$version"
            } else {
                version.toString()
            }
        }
        MetadataTransformer.transform(doc, version)
        // TODO: dependency transforms?

        val transformer = TransformerFactory.newInstance().newTransformer()
        cachePath.createParentDirectories()
        cachePath.outputStream().use {
            transformer.transform(DOMSource(doc), StreamResult(it))
        }
    }

}