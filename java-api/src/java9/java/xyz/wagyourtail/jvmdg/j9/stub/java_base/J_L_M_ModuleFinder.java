package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.j9.intl.module.ModuleFinderImpl;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.Optional;
import java.util.Set;

@Adapter("java/lang/module/ModuleFinder")
public interface J_L_M_ModuleFinder {
    static J_L_M_ModuleFinder ofSystem() {
        return ModuleFinderImpl.EMPTY;
    }

    Optional<J_L_M_ModuleReference> find(String name);

    Set<J_L_M_ModuleReference> findAll();

}
