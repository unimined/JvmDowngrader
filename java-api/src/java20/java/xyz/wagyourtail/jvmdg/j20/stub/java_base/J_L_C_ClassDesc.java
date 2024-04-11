package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.constant.ClassDesc;

public class J_L_C_ClassDesc {

    @Stub(ref = @Ref("java/lang/constant/ClassDesc"))
    public static ClassDesc ofInternalName(String internalName) {
        return ClassDesc.of(internalName.replace('/', '.'));
    }

}
