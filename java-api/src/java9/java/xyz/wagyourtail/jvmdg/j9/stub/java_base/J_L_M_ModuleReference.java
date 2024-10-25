package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Adapter("java/lang/module/ModuleReference")
public abstract class J_L_M_ModuleReference {
    private final J_L_M_ModuleDescriptor descriptor;
    private final URI location;

    protected J_L_M_ModuleReference(J_L_M_ModuleDescriptor descriptor, URI location) {
        this.descriptor = descriptor;
        this.location = location;
    }

    public final J_L_M_ModuleDescriptor descriptor() {
        return descriptor;
    }

    public final Optional<URI> location() {
        return Optional.ofNullable(location);
    }

    public abstract J_L_M_ModuleReader open() throws IOException;

}
