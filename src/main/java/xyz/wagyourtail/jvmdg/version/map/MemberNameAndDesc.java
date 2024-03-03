package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Objects;

public class MemberNameAndDesc {
    private final String name;
    private final Type desc;

    public MemberNameAndDesc(String name, Type desc) {
        this.name = name;
        this.desc = desc;
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
        return Objects.equals(name, that.name) && Arrays.equals(desc.getArgumentTypes(), that.desc.getArgumentTypes());
    }

    @Override
    public String toString() {
        return name + ";" + desc.getDescriptor();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, Arrays.hashCode(desc.getArgumentTypes()));
    }
}
