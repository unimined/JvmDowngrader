package xyz.wagyourtail.jvmdg.j24.stub;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.constant.ClassDesc;

public class J_L_C_ClassDesc {

    @Stub
    public static ClassDesc arrayType(ClassDesc self) {
        return ClassDesc.ofDescriptor("[" + self.descriptorString());
    }

    @Stub
    public static ClassDesc arrayType(ClassDesc self, int dimensions) {
        return ClassDesc.ofDescriptor("[".repeat(dimensions) + self.descriptorString());
    }

    @Stub
    public static String displayName(ClassDesc self) {
        try{
            return self.displayName();
        } catch (AbstractMethodError e) {
            return ClassDesc.ofDescriptor(self.descriptorString()).displayName();
        }
    }

}
