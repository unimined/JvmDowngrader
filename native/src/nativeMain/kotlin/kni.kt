package xyz.wagyourtail.jvmdg.native

import kotlinx.cinterop.*
import java.native.JNIEnvVar
import java.native.jobject
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("Java_jnitest_JniFunctions_getImplLookup")
fun getImplLookup(env: CPointer<JNIEnvVar>, self: jobject): jobject? {
    val java = env.pointed.pointed!!
    val arena = Arena()
    try {
        val cls =
            java.FindClass!!.invoke(env, "java/lang/invoke/MethodHandles\$Lookup".cstr.getPointer(arena)) ?: return null
        val lookupId = java.GetStaticFieldID!!.invoke(
            env,
            cls,
            "IMPL_LOOKUP".cstr.getPointer(arena),
            "Ljava/lang/invoke/MethodHandles\$Lookup;".cstr.getPointer(arena)
        ) ?: return null
        return java.GetStaticObjectField!!.invoke(env, cls, lookupId)
    } finally {
        arena.clear()
    }
}
