package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.ResourceBundle;
import java.util.logging.Logger;

public class J_L_System {

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/System;")
    public static Logger getLogger(String name) {
        return Logger.getLogger(name);
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/System;")
    public static Logger getLogger(String name, ResourceBundle bundle) {
        var logger = Logger.getLogger(name);
        logger.setResourceBundle(bundle);
        return logger;
    }
}
