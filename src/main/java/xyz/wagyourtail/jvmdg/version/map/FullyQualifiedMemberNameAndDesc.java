package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.reflect.Method;
import java.util.Objects;

public class FullyQualifiedMemberNameAndDesc {
    private final Type owner;
    private final String name;
    private final Type desc;

    public FullyQualifiedMemberNameAndDesc(Type owner, String name, Type desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public static FullyQualifiedMemberNameAndDesc of(Method method) {
        return new FullyQualifiedMemberNameAndDesc(Type.getType(method.getDeclaringClass()), method.getName(), Type.getType(method));
    }

    public static FullyQualifiedMemberNameAndDesc of(Type type) {
        return new FullyQualifiedMemberNameAndDesc(type, null, null);
    }

    public static FullyQualifiedMemberNameAndDesc of(Handle handle) {
        return new FullyQualifiedMemberNameAndDesc(Type.getObjectType(handle.getOwner()), handle.getName(), Type.getType(handle.getDesc()));
    }

    public static FullyQualifiedMemberNameAndDesc of(MethodInsnNode min) {
        return new FullyQualifiedMemberNameAndDesc(Type.getObjectType(min.owner), min.name, Type.getMethodType(min.desc));
    }

    public static FullyQualifiedMemberNameAndDesc of(FieldInsnNode mn) {
        return new FullyQualifiedMemberNameAndDesc(Type.getObjectType(mn.owner), mn.name, Type.getType(mn.desc));
    }

    public Type getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public Type getDesc() {
        return desc;
    }

    public boolean isClassRef() {
        return name == null;
    }

    public MemberNameAndDesc toMemberNameAndDesc() {
        if (name == null) return null;
        return new MemberNameAndDesc(name, desc);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(owner.getDescriptor());
        if (name != null) {
            sb.append(name).append(";").append(desc.getDescriptor());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FullyQualifiedMemberNameAndDesc that = (FullyQualifiedMemberNameAndDesc) o;
        return Objects.equals(owner, that.owner) && Objects.equals(name, that.name) && Objects.equals(desc, that.desc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, name, desc);
    }
}
