package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j25.stub.java_base.*;
import xyz.wagyourtail.jvmdg.j25.stub.java_net_http.J_N_H_HttpResponse;

public class Java25Downgrader extends AllVersionDowngrader {

    public Java25Downgrader() {
        super(Opcodes.V25, Opcodes.V24, 0);
    }

    @Override
    public void init() {
        // -- java.base --
        stub(J_I_Reader.class);
        stub(J_L_CharSequence.class);
        stub(J_L_IO.class);
        stub(J_L_Math.class);
        stub(J_L_R_AccessFlag.class);
        stub(J_L_ScopedValue.class);
        stub(J_L_StrictMath.class);
        stub(J_U_Currency.class);
        stub(J_U_TimeZone.class);
        stub(J_U_Z_Deflater.class);
        stub(J_U_Z_Inflater.class);

        // -- java.net.http --
        stub(J_N_H_HttpResponse.class);

    }

}
