package xyz.wagyourtail.jvmdg.asm.info;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

public class MethodInfo {
    public final int access;
    public final String name;
    public final Type desc;

    public MethodInfo(int access, String name, Type desc) {
        this.access = access;
        this.name = name;
        this.desc = desc;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MethodInfo that = (MethodInfo) o;
        return Objects.equals(name, that.name) && Objects.equals(desc, that.desc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc);
    }

    public static MethodInfo of(Method method) {
        return new MethodInfo(
            method.getModifiers(),
            method.getName(),
            Type.getType(method)
        );
    }

    public static MethodInfo of(Constructor<?> constructor) {
        return new MethodInfo(
            constructor.getModifiers(),
            "<init>",
            Type.getType(constructor)
        );
    }

    public static MethodInfo of(MethodNode methodNode) {
        return new MethodInfo(
            methodNode.access,
            methodNode.name,
            Type.getMethodType(methodNode.desc)
        );
    }

    public static MethodInfo createClinit() {
        return new MethodInfo(
            Opcodes.ACC_STATIC,
            "<clinit>",
            Type.getMethodType("()V")
        );
    }

    public boolean isInheritable() {
        return (access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == 0 && !name.startsWith("<");
    }

}
