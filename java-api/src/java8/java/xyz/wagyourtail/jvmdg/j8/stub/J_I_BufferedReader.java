package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j8.stub.stream.J_U_S_Stream;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.BufferedReader;

public class J_I_BufferedReader {

    @Stub
    public static J_U_S_Stream<String> lines(BufferedReader reader) {
        throw MissingStubError.create();
    }

}
