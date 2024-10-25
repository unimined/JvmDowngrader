package xyz.wagyourtail.gradle.shadow

import org.objectweb.asm.commons.Remapper

class PackageRelocator(val map: Map<String, String>): Remapper() {

    override fun map(internalName: String): String {
        for ((from, to) in map) {
            if (internalName.startsWith(from)) {
                return to + internalName.substring(from.length)
            }
        }
        return internalName
    }

}
