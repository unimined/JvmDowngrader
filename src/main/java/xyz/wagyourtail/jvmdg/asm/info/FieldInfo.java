package xyz.wagyourtail.jvmdg.asm.info;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;

import java.lang.reflect.Field;
import java.util.Objects;

public class FieldInfo {
    public final int access;
    public final String name;
    public final Type desc;

    public FieldInfo(int access, String name, Type desc) {
        this.access = access;
        this.name = name;
        this.desc = desc;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FieldInfo fieldInfo = (FieldInfo) o;
        return Objects.equals(name, fieldInfo.name) && Objects.equals(desc, fieldInfo.desc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc);
    }

    public static FieldInfo of(Field field) {
        return new FieldInfo(
            field.getModifiers(),
            field.getName(),
            Type.getType(field.getType())
        );
    }

    public static FieldInfo of(FieldNode fieldNode) {
        return new FieldInfo(
            fieldNode.access,
            fieldNode.name,
            Type.getType(fieldNode.desc)
        );
    }

}
