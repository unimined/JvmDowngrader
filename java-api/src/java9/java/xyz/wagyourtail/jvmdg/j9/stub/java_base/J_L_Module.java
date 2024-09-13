package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.CoverageIgnore;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Set;

@Adapter("Ljava/lang/Module;")
public class J_L_Module {

    private final WeakReference<ClassLoader> classLoader;
    private final J_L_ModuleLayer layer;
    private final J_L_M_ModuleDescriptor descriptor;
    private final String name;

    @CoverageIgnore
    public J_L_Module() {
        J_L_StackWalker walker = J_L_StackWalker.getInstance(J_L_StackWalker.Option.RETAIN_CLASS_REFERENCE);
        J_L_StackWalker.StackFrame sf = walker.walk(stream -> stream.skip(2).findFirst().get());
        // require caller to be <clinit> of a class
        if (!sf.getMethodName().equals("<clinit>")) {
            throw new IllegalStateException("Module must be created from a class <clinit>");
        }
        this.classLoader = new WeakReference<>(sf.getDeclaringClass().getClassLoader());
        this.layer = null;
        this.descriptor = null;
        this.name = null;
    }

    @CoverageIgnore
    public J_L_Module(ClassLoader classLoader) {
        this.classLoader = new WeakReference<>(classLoader);
        this.layer = null;
        this.descriptor = null;
        this.name = null;
    }

    @CoverageIgnore
    public J_L_Module(ClassLoader classLoader, J_L_ModuleLayer layer, J_L_M_ModuleDescriptor descriptor) {
        this.classLoader = classLoader == null ? null : new WeakReference<>(classLoader);
        this.layer = layer;
        this.descriptor = descriptor;
        this.name = descriptor.name();
    }

    public boolean isNamed() {
        return this.name != null;
    }

    public String getName() {
        return this.name;
    }

    public ClassLoader getClassLoader() {
        return this.classLoader == null ? null : // If ClassLoader has been freed, it's not a valid state
                Objects.requireNonNull(this.classLoader.get(), "ClassLoader has been freed");
    }

    public J_L_M_ModuleDescriptor getDescriptor() {
        return this.descriptor;
    }

    public J_L_ModuleLayer getLayer() {
        return this.layer;
    }
//
//    public boolean canRead(J_L_Module other) {
//        // TODO
//    }
//
//    public J_L_Module addReads(J_L_Module other) {
//        // TODO
//    }
//
//    public boolean isExported(String pn, J_L_Module other) {
//        // TODO
//    }
//
//    public boolean isOpen(String pn, J_L_Module other) {
//        // TODO
//    }
//
//    public boolean isExported(String pn) {
//        // TODO
//    }
//
//    public boolean isOpen(String pn) {
//        // TODO
//    }
//
//    public J_L_Module addExports(String pn, J_L_Module other) {
//        // TODO
//    }
//
//    public J_L_Module addOpens(String pn, J_L_Module other) {
//        // TODO
//    }
//
//    public J_L_Module addUses(Class<?> service) {
//        // TODO
//    }
//
//    public boolean canUse(Class<?> service) {
//        // TODO
//    }

    public Set<String> getPackages() {
        return this.descriptor.packages();
    }
//
//    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
//        // TODO
//    }
//
//    public Annotation[] getAnnotations() {
//        // TODO
//    }
//
//    public Annotation[] getDeclaredAnnotations() {
//        // TODO
//    }
//
//    public InputStream getResourceAsStream(String name) {
//        // TODO
//    }
//
//    @Override
//    public String toString() {
//        // TODO
//    }

}
