package xyz.wagyourtail.jvmdg;

import org.objectweb.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.util.Function;

import java.util.ArrayList;
import java.util.List;

public class ASMClassWriter extends ClassWriter {
    private final Function<String, String> getSuperType;

    public ASMClassWriter(int flags, Function<String, String> getSuperType) {
        super(flags);
        this.getSuperType = getSuperType;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        List<String> l1 = getSuperTypes(type1);
        List<String> l2 = getSuperTypes(type2);
        // get intersection
        l1.retainAll(l2);
        // get first element
        return l1.get(0);
    }

    public List<String> getSuperTypes(String type) {
        List<String> l = new ArrayList<>();
        String current = type;
        while (!current.equals("java/lang/Object")) {
            l.add(current);
            try {
                Class<?> c = Class.forName(current.replace('/', '.'));
                current = c.getSuperclass().getName().replace('.', '/');
            } catch (ClassNotFoundException e) {
                current = getSuperType.apply(current);
                if (current == null) {
                    current = "java/lang/Object";
                    System.err.println("Could not find super type for " + type);
                }
            }
        }
        l.add("java/lang/Object");
        return l;
    }
}
