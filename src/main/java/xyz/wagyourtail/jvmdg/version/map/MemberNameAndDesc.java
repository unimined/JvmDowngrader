package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public class MemberNameAndDesc {
    private final String name;
    private final Type desc;
    /**
     * commons-asm carries this flag too, but there it defaults to exact
     * matching. jvmdg historically compared methods on name + argument
     * types only (covariant-return stub matching, ctor stubs whose impl
     * returns the instance type), so the two-arg constructor keeps that
     * as the default; pass {@code false} explicitly for exact desc
     * equality.
     */
    private final boolean ignoreReturnValue;
    private int hash;

    public MemberNameAndDesc(String name, Type desc) {
        this(name, desc, true);
    }

    public MemberNameAndDesc(String name, Type desc, boolean ignoreReturnValue) {
        this.name = name;
        this.desc = desc;
        this.ignoreReturnValue = ignoreReturnValue;
    }

    public static MemberNameAndDesc fromMember(Member member) {
        if (member instanceof Method) {
            return new MemberNameAndDesc(member.getName(), Type.getType((Method) member));
        } else if (member instanceof Field) {
            return new MemberNameAndDesc(member.getName(), Type.getType(((Field) member).getType()));
        } else if (member instanceof Constructor) {
            return new MemberNameAndDesc("<init>", Type.getType((Constructor<?>) member));
        } else {
            throw new IllegalArgumentException("Unknown member type: " + member.getClass());
        }
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

    public boolean isIgnoreReturnValue() {
        return ignoreReturnValue;
    }

    /**
     * a copy that compares equal to another ignoring-return-value instance
     * when name and argument types match, regardless of return type.
     */
    public MemberNameAndDesc ignoreReturnValue() {
        if (ignoreReturnValue || desc.getSort() != Type.METHOD) {
            return this;
        }
        return new MemberNameAndDesc(name, desc, true);
    }

    public FullyQualifiedMemberNameAndDesc toFullyQualified(Type owner) {
        return new FullyQualifiedMemberNameAndDesc(owner, name, desc);
    }

    public boolean isMethod() {
        return desc.getSort() == Type.METHOD;
    }

    public boolean isMethodRef() {
        return isMethod();
    }

    public boolean isField() {
        return desc.getSort() != Type.METHOD;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberNameAndDesc that = (MemberNameAndDesc) o;
        if (desc.getSort() != that.desc.getSort()) return false;
        if (!name.equals(that.name)) return false;
        if (ignoreReturnValue && that.ignoreReturnValue && desc.getSort() == Type.METHOD) {
            return Arrays.equals(desc.getArgumentTypes(), that.desc.getArgumentTypes());
        }
        return Objects.equals(desc, that.desc);
    }

    @Override
    public String toString() {
        return name + ";" + desc.getDescriptor();
    }

    @Override
    public int hashCode() {
        // methods always hash name + arg types, not full desc — equal keys
        // must hash alike even when only one side ignores the return type
        // (flagged-vs-exact equality falls back to desc equality, which
        // implies equal args). Immutable, so the descriptor re-parse and
        // rehash are cached; a racy recompute produces the same value.
        int h = hash;
        if (h == 0) {
            if (desc.getSort() == Type.METHOD) {
                h = Objects.hash(name, Arrays.hashCode(desc.getArgumentTypes()));
            } else {
                h = Objects.hash(name, desc);
            }
            hash = h;
        }
        return h;
    }

}
