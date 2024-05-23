package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j8.stub.stream.J_U_S_IntStream;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_U_BitSet {

    @Stub(ref = @Ref("java/util/BitSet"))
    public static J_U_S_IntStream stream(java.util.BitSet bitSet) {
        throw MissingStubError.create();
    }

}
