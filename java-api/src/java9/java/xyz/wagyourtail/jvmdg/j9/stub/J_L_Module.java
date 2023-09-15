package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

@Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/lang/Module;"))
public class J_L_Module {

    private final ClassLoader classLoader;
    private final J_L_ModuleLayer layer = new J_L_ModuleLayer();

    public J_L_Module() {
        // TODO: determine better way to get classloader, caller.getClass().getClassLoader()?
        this.classLoader = Thread.currentThread().getContextClassLoader();
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
