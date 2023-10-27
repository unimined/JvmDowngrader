package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.ResourceBundle;
import java.util.logging.Logger;

public class J_L_System {

    @Stub(ref = @Ref("Ljava/lang/System;"))
    public static Logger getLogger(String name) {
        return Logger.getLogger(name);
    }

    @Stub(ref = @Ref("Ljava/lang/System;"))
    public static Logger getLogger(String name, ResourceBundle bundle) {
        Logger logger = Logger.getLogger(name);
        logger.setResourceBundle(bundle);
        return logger;
    }

}
