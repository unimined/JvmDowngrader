package xyz.wagyourtail.jvmdg.site.maven.transform

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

operator fun NodeList.iterator(): Iterator<Node> {
    return object : Iterator<Node> {
        private var index = 0

        override fun hasNext(): Boolean {
            return index < length
        }

        override fun next(): Node {
            return item(index++)
        }
    }
}

object MetadataTransformer {

    fun transform(document: Document, major: Int) {
        // replace group with jvmdg-<major>.${group}
        var group: String? = null
        for (groupNode in document.getElementsByTagName("groupId")) {
            val parent = groupNode.parentNode as? Element
            if (parent == null || parent.tagName !in setOf("parent", "project")) {
                continue
            }
            if (parent.tagName == "parent") {
                if (group == null) {
                    group = groupNode.textContent
                }
            } else {
                group = groupNode.textContent
            }
            groupNode.textContent = "jvmdg-$major.${groupNode.textContent}"
        }

        // replace artifact with jvmdg-<major>.${artifact} if artifact is the same as group
        if (group != null) {
            for (artifact in document.getElementsByTagName("artifactId")) {
                val parent = artifact.parentNode as? Element
                if (parent == null || parent.tagName !in setOf("parent", "project")) {
                    continue
                }
                if (artifact.textContent.startsWith(group)) {
                    artifact.textContent = "jvmdg-$major.${artifact.textContent}"
                }
            }
        }
    }

}
