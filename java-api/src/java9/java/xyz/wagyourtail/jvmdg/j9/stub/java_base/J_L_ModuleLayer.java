package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.j9.intl.module.ModuleConstantHelper;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.CoverageIgnore;

import java.util.*;

@Adapter("Ljava/lang/ModuleLayer;")
public class J_L_ModuleLayer {
    static final J_L_ModuleLayer EMPTY_MODULE_LAYER =
        new J_L_ModuleLayer(Collections.emptyList(), Collections.emptyMap());

    private final List<J_L_ModuleLayer> parents;
    private final Map<String, J_L_Module> modules;
    private volatile Set<J_L_Module> moduleSet;
    private volatile String strValue;

    @CoverageIgnore
    public J_L_ModuleLayer(List<J_L_ModuleLayer> parents, Map<String, J_L_Module> modules) {
        this.parents = parents;
        this.modules = modules;
    }

//    public J_L_ModuleLayer defineModulesWithOneLoader(Configuration cf, ClassLoader parentLoader) {
//        // TODO
//    }
//
//    public J_L_ModuleLayer defineModulesWithManyLoaders(Configuration cf, ClassLoader parentLoader) {
//        // TODO
//    }
//
//    public J_L_ModuleLayer defineModules(Configuration cf, Function<String, ClassLoader> clf) {
//        // TODO
//    }
//
//    public static ModuleLayer.Controller defineModulesWithOneLoader(Configuration cf, List<J_L_ModuleLayer> parentLayers, ClassLoader parentLoader) {
//        // TODO
//    }
//
//    public static ModuleLayer.Controller defineModulesWithManyLoaders(Configuration cf, List<J_L_ModuleLayer> parentLayers, ClassLoader parentLoader) {
//        // TODO
//    }
//
//    public static ModuleLayer.Controller defineModules(Configuration cf, List<J_L_ModuleLayer> parentLayers, Function<String, ClassLoader> clf) {
//        // TODO
//    }
//
//    public Configuration configuration() {
//        // TODO
//    }

    public static J_L_ModuleLayer empty() {
        return EMPTY_MODULE_LAYER;
    }

    public static J_L_ModuleLayer boot() {
        return ModuleConstantHelper.BOOT_LAYER;
    }

    public List<J_L_ModuleLayer> parents() {
        return this.parents;
    }

    public Set<J_L_Module> modules() {
        if (this.moduleSet == null) {
            this.moduleSet = Collections.unmodifiableSet(new HashSet<>(this.modules.values()));
        }
        return this.moduleSet;
    }

    public Optional<J_L_Module> findModule(String name) {
        Objects.requireNonNull(name);
        return Optional.ofNullable(this.modules.get(name));
    }

    public ClassLoader findLoader(String name) {
        Optional<J_L_Module> optionalModule = this.findModule(name);
        if (optionalModule.isPresent()) {
            return optionalModule.get().getClassLoader();
        }
        throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
        if (this.strValue == null) {
            StringJoiner stringJoiner = new StringJoiner(", ");
            // Note: Java9+ prepopulate modules when toString is called.
            for (J_L_Module module : this.modules()) {
                stringJoiner.add(module.getName());
            }
            this.strValue = stringJoiner.toString();
        }
        return this.strValue;
    }

}
