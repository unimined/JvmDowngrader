package xyz.wagyourtail.jvmdg.site.maven.transform

import org.w3c.dom.Document

object MetadataTransformer {

    fun transform(document: Document, major: Int) {
        // replace group with jvmdg-<major>.${group}
        val group = document.getElementsByTagName("groupId").item(0).textContent
        val newGroup = "jvmdg-$major.$group"
        document.getElementsByTagName("groupId").item(0).textContent = newGroup
        // replace artifact with jvmdg-<major>.${artifact} if artifact is the same as group
        val artifact = document.getElementsByTagName("artifactId").item(0).textContent
        if (artifact.startsWith(group)) {
            document.getElementsByTagName("artifactId").item(0).textContent = "jvmdg-$major.$artifact"
        }
    }

}
