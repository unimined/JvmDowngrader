package xyz.wagyourtail.jvmdg.gradle

import org.objectweb.asm.ClassWriter

class ASMClassWriter(flags: Int, private val getSuperType: (String) -> String?): ClassWriter(flags) {
    override fun getCommonSuperClass(type1: String, type2: String): String {
        val l1 = getSuperTypes(type1)
        val l2 = getSuperTypes(type2)
        return l1.intersect(l2.toSet()).first()
    }

    private fun getSuperTypes(type: String): MutableList<String> {
        val l: MutableList<String> = ArrayList()
        var current: String? = type
        while (current != "java/lang/Object") {
            l.add(current!!)
            try {
                val c = Class.forName(current.replace('/', '.'))
                current = c.superclass.getName().replace('.', '/')
            } catch (e: Throwable) {
                current = getSuperType(current)
                if (current == null) {
                    current = "java/lang/Object"
                    System.err.println("Could not find super type for $type")
                }
            }
        }
        l.add("java/lang/Object")
        return l
    }
}