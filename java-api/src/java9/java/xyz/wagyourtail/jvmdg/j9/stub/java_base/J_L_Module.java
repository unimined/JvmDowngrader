package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

@Adapter("Ljava/lang/Module;")
public class J_L_Module {

    private final ClassLoader classLoader;
    private final J_L_ModuleLayer layer = new J_L_ModuleLayer();

    public J_L_Module() {
        J_L_StackWalker walker = J_L_StackWalker.getInstance(J_L_StackWalker.Option.RETAIN_CLASS_REFERENCE);
        J_L_StackWalker.StackFrame sf = walker.walk(stream -> stream.skip(2).findFirst().get());
        // require caller to be <clinit> of a class
        if (!sf.getMethodName().equals("<clinit>")) {
            throw new IllegalStateException("Module must be created from a class <clinit>");
        }
        this.classLoader = sf.getDeclaringClass().getClassLoader();
    }

    public J_L_Module(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

//    public boolean isNamed() {
//        // TODO
//    }
//
//    public String getName() {
//        // TODO
//    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

//    public ModuleDescriptor getDescriptor() {
//        // TODO
//    }
//
    public J_L_ModuleLayer getLayer() {
        return layer;
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
//
//    public Set<String> getPackages() {
//        // TODO
//    }
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
