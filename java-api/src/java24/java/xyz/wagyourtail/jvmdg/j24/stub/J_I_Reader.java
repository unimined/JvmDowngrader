package xyz.wagyourtail.jvmdg.j24.stub;

import xyz.wagyourtail.jvmdg.j24.impl.CharSequenceReader;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.Reader;

public class J_I_Reader {

    @Stub(ref = @Ref("Ljava/io/Reader;"))
    public static Reader of(CharSequence cs) {
        return new CharSequenceReader(cs);
    }


}
