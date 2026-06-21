package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

@JEP(334)
public class J_L_Long {

    @Stub
    public static Optional<Long> describeConstable(Long self) {
        return Optional.of(self);
    }

    @Stub
    public static Long resolveConstantDesc(Long self, MethodHandles.Lookup lookup) {
        return self;
    }

}
