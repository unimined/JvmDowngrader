package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.constant.ClassDesc;

public class J_L_C_ClassDesc {

    @Stub(ref = @Ref("java/lang/constant/ClassDesc"))
    public static ClassDesc ofInternalName(String internalName) {
        // ConstantUtils.validateInternalClassName: reject empty names and
        // anything that is not '/'-separated binary-name segments or an
        // array descriptor
        if (internalName.isEmpty()) {
            throw new IllegalArgumentException("empty class name");
        }
        if (internalName.charAt(0) != '[') {
            for (int i = 0, off = 0; i <= internalName.length(); i++) {
                if (i == internalName.length() || internalName.charAt(i) == '/') {
                    if (i == off) {
                        throw new IllegalArgumentException("empty segment in " + internalName);
                    }
                    off = i + 1;
                } else {
                    char c = internalName.charAt(i);
                    if (c == '.' || c == ';' || c == '[') {
                        throw new IllegalArgumentException("invalid character '" + c + "' in " + internalName);
                    }
                }
            }
        } else {
            // array internal names are already field descriptors
            return ClassDesc.ofDescriptor(internalName);
        }
        return ClassDesc.of(internalName.replace('/', '.'));
    }

}
