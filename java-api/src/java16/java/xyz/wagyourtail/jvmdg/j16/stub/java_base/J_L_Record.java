package xyz.wagyourtail.jvmdg.j16.stub.java_base;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

@Adapter("Ljava/lang/Record;")
public abstract class J_L_Record {

    public J_L_Record() {
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

}
