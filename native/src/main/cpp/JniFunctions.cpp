#include "jnitest_JniFunctions.h"

JNIEXPORT jobject JNICALL Java_jnitest_JniFunctions_getImplLookup(JNIEnv* env, jclass self) {
	jclass cls = env->FindClass("java/lang/invoke/MethodHandles$Lookup");
	if (cls == NULL) return NULL;
	jfieldID lookupId = env->GetStaticFieldID(cls, "IMPL_LOOKUP", "Ljava/lang/invoke/MethodHandles$Lookup;");
	if (lookupId == NULL) return NULL;
	return env->GetStaticObjectField(cls, lookupId);
}