package xyz.wagyourtail.jvmdg.j9.intl.module;

import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_M_ModuleDescriptor;
import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_M_ModuleFinder;
import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_M_ModuleReference;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class ModuleFinderImpl implements J_L_M_ModuleFinder {
    public static final ModuleFinderImpl EMPTY = new ModuleFinderImpl(Collections.emptySet());
    private final Set<J_L_M_ModuleReference> moduleReferences;

    public ModuleFinderImpl(Set<J_L_M_ModuleReference> moduleReferences) {
        this.moduleReferences = moduleReferences;
    }

    @Override
    public Optional<J_L_M_ModuleReference> find(String name) {
        for (J_L_M_ModuleReference moduleReference : this.moduleReferences) {
            J_L_M_ModuleDescriptor descriptor = moduleReference.descriptor();
            if (descriptor != null && descriptor.name().equals(name)) {
                return Optional.of(moduleReference);
            }
        }
        return Optional.empty();
    }

    @Override
    public Set<J_L_M_ModuleReference> findAll() {
        return this.moduleReferences;
    }

}
