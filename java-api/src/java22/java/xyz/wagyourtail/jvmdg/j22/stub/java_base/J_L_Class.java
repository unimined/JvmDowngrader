package xyz.wagyourtail.jvmdg.j22.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Class {

    @Stub(ref = @Ref("java/lang/Class"))
    public static Class<?> forPrimitiveName(String name) {
        return switch (name) {
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "char" -> char.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            default -> throw new IllegalArgumentException("Unknown primitive type: " + name);
        };
    }

}
