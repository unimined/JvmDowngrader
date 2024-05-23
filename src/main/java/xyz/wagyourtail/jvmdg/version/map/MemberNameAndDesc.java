package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.Objects;

public class MemberNameAndDesc {
    private final String name;
    private final Type desc;

    public MemberNameAndDesc(String name, Type desc) {
        this.name = name;
        this.desc = desc;
    }

    public static MemberNameAndDesc fromNode(MethodNode mNode) {
        return new MemberNameAndDesc(mNode.name, Type.getMethodType(mNode.desc));
    }

    public static MemberNameAndDesc fromNode(FieldNode fNode) {
        return new MemberNameAndDesc(fNode.name, Type.getType(fNode.desc));
    }

    public String getName() {
        return name;
    }

    public Type getDesc() {
        return desc;
    }

    public FullyQualifiedMemberNameAndDesc toFullyQualified(Type owner) {
        return new FullyQualifiedMemberNameAndDesc(owner, name, desc);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberNameAndDesc that = (MemberNameAndDesc) o;
        if (desc.getSort() == Type.METHOD) {
            return Objects.equals(name, that.name) && Arrays.equals(desc.getArgumentTypes(), that.desc.getArgumentTypes());
        } else {
            return Objects.equals(name, that.name) && desc.equals(that.desc);
        }
    }

    @Override
    public String toString() {
        return name + ";" + desc.getDescriptor();
    }

    @Override
    public int hashCode() {
        if (desc.getSort() == Type.METHOD) {
            return Objects.hash(name, Arrays.hashCode(desc.getArgumentTypes()));
        } else {
            return Objects.hash(name, desc);
        }
    }
}
