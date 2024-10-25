package xyz.wagyourtail.jvmdg.j9.intl.module;

import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_M_ModuleDescriptor;
import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_Module;
import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_ModuleLayer;

import java.util.*;

public class ModuleConstantHelper {
    private static final Map<String, J_L_Module> MODULES = new HashMap<>();
    public static final J_L_ModuleLayer BOOT_LAYER =
        // Note: This field is in this class as it's initialization is execution order sensitive as it need
        // to be loaded after "MODULES" HashMap is initialized, but before initializing any boot "Module"
        new J_L_ModuleLayer(Collections.emptyList(), MODULES);
    private static final Map<String, J_L_Module> MODULES_FROM_PACKAGE = new HashMap<>();
    private static final ClassLoader ORIGINAL_SYSTEM_CLASS_LOADER = ClassLoader.getSystemClassLoader();

    static {
        // TODO: Auto generate this data in a separate "ModuleConstants" class
        registerBootModule("java.base", "java.io", "java.lang", "java.math",
            "java.net", "java.nio", "java.security", "java.text", "java.time", "java.util",
            "javax.crypto", "javax.net", "javax.security");
        registerBootModule("java.logging", "java.util.logging");
        registerBootModule("java.net.http", "java.net.http");
        registerBootModule("jdk.httpserver", "com.sun.net.httpserver");
        registerBootModule("jdk.jfr", "jdk.jfr");
    }

    private static void registerBootModule(String name, String... packages) {
        J_L_M_ModuleDescriptor descriptor = new J_L_M_ModuleDescriptor.Builder(name, false, Collections.emptySet())
            .packages(new HashSet<>(Arrays.asList(packages))).build();
        J_L_Module module = new J_L_Module(ORIGINAL_SYSTEM_CLASS_LOADER, BOOT_LAYER, descriptor);
        MODULES.put(name, module);
        for (String packageName : packages) {
            MODULES_FROM_PACKAGE.put(packageName, module);
        }
    }

    public static J_L_Module bootModuleFromClass(Class<?> clazz) {
        boolean bootClassLoader;
        if ((bootClassLoader = clazz.getClassLoader() == null) ||
            clazz.getClassLoader() == ORIGINAL_SYSTEM_CLASS_LOADER) {
            J_L_Module module = bootModuleFromClassName(clazz.getName());
            // Unnamed modules must have a class loader
            // but on java8, the boot class loader is null
            // Making Object.class.getClassLoader() return null on java8
            if (module == null && bootClassLoader) {
                return MODULES.get("java.base");
            }
            return module;
        }
        return null;
    }

    // Return null to make JvmDowngrader use
    public static J_L_Module bootModuleFromClassName(String className) {
        String packageName = className;
        while (true) {
            int pkgEnd = packageName.lastIndexOf('.');
            if (pkgEnd == -1) return null;
            packageName = packageName.substring(0, pkgEnd);
            J_L_Module module = MODULES_FROM_PACKAGE.get(packageName);
            if (module != null) return module;
        }
    }

}
