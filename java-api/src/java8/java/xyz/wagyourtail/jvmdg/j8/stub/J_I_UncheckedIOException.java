package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.util.Objects;

@Stub(ref = @Ref("java/io/UncheckedIOException"))
public class J_I_UncheckedIOException extends RuntimeException {

    public J_I_UncheckedIOException(String message, IOException cause) {
        super(message, Objects.requireNonNull(cause));
    }

    public J_I_UncheckedIOException(IOException cause) {
        super(Objects.requireNonNull(cause));
    }

    public IOException getCause() {
        return (IOException) super.getCause();
    }

    // private void readObject(ObjectInputStream)

}
