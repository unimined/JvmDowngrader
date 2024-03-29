package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Locale;
import java.util.ResourceBundle;

public class J_U_ResourceBundle {

    @Stub(ref = @Ref("Ljava/util/ResourceBundle;"))
    public static ResourceBundle getBundle(String baseName, J_L_Module module) {
        return ResourceBundle.getBundle(baseName, Locale.getDefault(), module.getClassLoader());
    }

    @Stub(ref = @Ref("Ljava/util/ResourceBundle;"))
    public static ResourceBundle getBundle(String baseName, Locale locale, J_L_Module module) {
        return ResourceBundle.getBundle(baseName, locale, module.getClassLoader());
    }

}
