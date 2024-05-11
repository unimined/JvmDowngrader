package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.Optional;

@Adapter("Ljava/lang/ModuleLayer;")
public class J_L_ModuleLayer {

    // TODO: Controller

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
//
//    public List<J_L_ModuleLayer> parents() {
//        // TODO
//    }
//
//    public Set<J_L_Module> modules() {
//        // TODO
//    }

    public Optional<J_L_Module> findModule(String name) {
        // TODO: actually implement
        return Optional.empty();
    }

//    public ClassLoader findLoader(String name) {
//        // TODO
//    }
//
//    @Override
//    public String toString() {
//        // TODO
//    }
//
//    public static J_L_ModuleLayer empty() {
//        // TODO
//    }
//
//    public static J_L_ModuleLayer boot() {
//        // TODO
//    }

}
