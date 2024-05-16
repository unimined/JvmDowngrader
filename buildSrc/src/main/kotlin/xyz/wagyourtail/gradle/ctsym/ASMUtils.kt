package xyz.wagyourtail.gradle.ctsym

import org.objectweb.asm.tree.*

// compare class nodes for seeing if we can merge with a previous version
fun ClassNode.matches(other: ClassNode): Boolean {
    if (this.name != other.name) return false
    if (this.superName != other.superName) return false
    if (this.interfaces != other.interfaces) return false
    if (this.fields.size != other.fields.size) return false
    if (this.methods.size != other.methods.size) return false
    if (this.innerClasses.size != other.innerClasses.size) return false
    for (i in this.fields.indices) {
        if (!this.fields[i].matches(other.fields[i])) return false
    }
    for (i in this.methods.indices) {
        if (!this.methods[i].matches(other.methods[i])) return false
    }
    for (i in this.innerClasses.indices) {
        if (!this.innerClasses[i].matches(other.innerClasses[i])) return false
    }
    return true
}

fun FieldNode.matches(other: FieldNode): Boolean {
    if (this.name != other.name) return false
    if (this.desc != other.desc) return false
    if (this.access != other.access) return false
    if (this.signature != other.signature) return false
    if (this.value != other.value) return false

    // check annotations
    if (this.visibleAnnotations?.size != other.visibleAnnotations?.size) return false
    if (this.invisibleAnnotations?.size != other.invisibleAnnotations?.size) return false
    if (this.visibleTypeAnnotations?.size != other.visibleTypeAnnotations?.size) return false
    if (this.invisibleTypeAnnotations?.size != other.invisibleTypeAnnotations?.size) return false

    for (i in this.visibleAnnotations?.indices ?: emptyList()) {
        if (!this.visibleAnnotations[i].matches(other.visibleAnnotations[i])) return false
    }

    for (i in this.invisibleAnnotations?.indices ?: emptyList()) {
        if (!this.invisibleAnnotations[i].matches(other.invisibleAnnotations[i])) return false
    }

    for (i in this.visibleTypeAnnotations?.indices ?: emptyList()) {
        if (!this.visibleTypeAnnotations[i].matches(other.visibleTypeAnnotations[i])) return false
    }

    for (i in this.invisibleTypeAnnotations?.indices ?: emptyList()) {
        if (!this.invisibleTypeAnnotations[i].matches(other.invisibleTypeAnnotations[i])) return false
    }

    return true
}

fun AnnotationNode.matches(other: AnnotationNode): Boolean {
    if (this.desc != other.desc) return false
    if (this.values?.size != other.values?.size) return false
    for (i in this.values?.indices ?: emptyList()) {
        if (this.values[i] != other.values[i]) return false
    }
    return true
}

fun MethodNode.matches(other: MethodNode): Boolean {
    if (this.name != other.name) return false
    if (this.desc != other.desc) return false
    if (this.access != other.access) return false
    if (this.signature != other.signature) return false
    if (this.exceptions != other.exceptions) return false

    // check annotations
    if (this.visibleAnnotations?.size != other.visibleAnnotations?.size) return false
    if (this.invisibleAnnotations?.size != other.invisibleAnnotations?.size) return false
    if (this.visibleTypeAnnotations?.size != other.visibleTypeAnnotations?.size) return false
    if (this.invisibleTypeAnnotations?.size != other.invisibleTypeAnnotations?.size) return false
    if (this.visibleParameterAnnotations?.size != other.visibleParameterAnnotations?.size) return false
    if (this.invisibleParameterAnnotations?.size != other.invisibleParameterAnnotations?.size) return false

    for (i in this.visibleAnnotations?.indices ?: emptyList()) {
        if (!this.visibleAnnotations[i].matches(other.visibleAnnotations[i])) return false
    }

    for (i in this.invisibleAnnotations?.indices ?: emptyList()) {
        if (!this.invisibleAnnotations[i].matches(other.invisibleAnnotations[i])) return false
    }

    for (i in this.visibleTypeAnnotations?.indices ?: emptyList()) {
        if (!this.visibleTypeAnnotations[i].matches(other.visibleTypeAnnotations[i])) return false
    }

    for (i in this.invisibleTypeAnnotations?.indices ?: emptyList()) {
        if (!this.invisibleTypeAnnotations[i].matches(other.invisibleTypeAnnotations[i])) return false
    }

    for (i in this.visibleParameterAnnotations?.indices ?: emptyList()) {
        if (this.visibleParameterAnnotations[i]?.size != other.visibleParameterAnnotations[i]?.size) return false
        for (j in this.visibleParameterAnnotations[i]?.indices ?: emptyList()) {
            if (!this.visibleParameterAnnotations[i][j].matches(other.visibleParameterAnnotations[i][j])) return false
        }
    }

    for (i in this.invisibleParameterAnnotations?.indices ?: emptyList()) {
        if (this.invisibleParameterAnnotations[i]?.size != other.invisibleParameterAnnotations[i]?.size) return false
        for (j in this.invisibleParameterAnnotations[i]?.indices ?: emptyList()) {
            if (!this.invisibleParameterAnnotations[i][j].matches(other.invisibleParameterAnnotations[i][j])) return false
        }
    }

    return true
}

fun InnerClassNode.matches(other: InnerClassNode): Boolean {
    if (this.name != other.name) return false
    if (this.outerName != other.outerName) return false
    if (this.innerName != other.innerName) return false
    if (this.access != other.access) return false
    return true
}