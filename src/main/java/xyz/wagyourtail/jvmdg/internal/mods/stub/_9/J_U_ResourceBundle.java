package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Locale;
import java.util.ResourceBundle;

public class J_U_ResourceBundle {

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/ResourceBundle;")
    public static ResourceBundle getBundle(String baseName, J_L_Module module) {
        return ResourceBundle.getBundle(baseName, Locale.getDefault(), module.getClassLoader());
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/ResourceBundle;")
    public static ResourceBundle getBundle(String baseName, Locale locale, J_L_Module module) {
        return ResourceBundle.getBundle(baseName, locale, module.getClassLoader());
    }
}
