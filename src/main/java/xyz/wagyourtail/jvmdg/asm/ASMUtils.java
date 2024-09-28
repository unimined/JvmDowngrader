package xyz.wagyourtail.jvmdg.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class ASMUtils {

    public static void boxType(MethodVisitor visitor, Type type) {
        Type box = getBoxFor(type);
        if (box != null) {
            visitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                box.getInternalName(),
                "valueOf",
                Type.getMethodDescriptor(box, type),
                false
            );
        }
    }

    public static void unboxType(MethodVisitor visitor, Type type) {
        switch (type.getInternalName()) {
            case "java/lang/Boolean":
                visitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Boolean",
                    "booleanValue",
                    "()Z",
                    false
                );
                break;
            case "java/lang/Byte":
                visitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Byte",
                    "byteValue",
                    "()B",
                    false
                );
                break;
            case "java/lang/Character":
                visitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Character",
                    "charValue",
                    "()C",
                    false
                );
                break;
            case "java/lang/Short":
                visitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Short",
                    "shortValue",
                    "()S",
                    false
                );
                break;
            case "java/lang/Integer":
                visitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Integer",
                    "intValue",
                    "()I",
                    false
                );
                break;
            case "java/lang/Long":
                visitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Long",
                    "longValue",
                    "()J",
                    false
                );
                break;
            case "java/lang/Float":
                visitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Float",
                    "floatValue",
                    "()F",
                    false
                );
                break;
            case "java/lang/Double":
                visitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Double",
                    "doubleValue",
                    "()D",
                    false
                );
                break;
            default:
        }
    }

    public static Type getPrimitiveFor(Type type) {
        switch (type.getInternalName()) {
            case "java/lang/Boolean":
                return Type.BOOLEAN_TYPE;
            case "java/lang/Byte":
                return Type.BYTE_TYPE;
            case "java/lang/Character":
                return Type.CHAR_TYPE;
            case "java/lang/Short":
                return Type.SHORT_TYPE;
            case "java/lang/Integer":
                return Type.INT_TYPE;
            case "java/lang/Long":
                return Type.LONG_TYPE;
            case "java/lang/Float":
                return Type.FLOAT_TYPE;
            case "java/lang/Double":
                return Type.DOUBLE_TYPE;
            default:
                return null;
        }
    }

    public static Type getBoxFor(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return Type.getObjectType("java/lang/Boolean");
            case Type.BYTE:
                return Type.getObjectType("java/lang/Byte");
            case Type.CHAR:
                return Type.getObjectType("java/lang/Character");
            case Type.SHORT:
                return Type.getObjectType("java/lang/Short");
            case Type.INT:
                return Type.getObjectType("java/lang/Integer");
            case Type.LONG:
                return Type.getObjectType("java/lang/Long");
            case Type.FLOAT:
                return Type.getObjectType("java/lang/Float");
            case Type.DOUBLE:
                return Type.getObjectType("java/lang/Double");
            default:
                return null;
        }
    }

    public static ClassNode bytesToClassNode(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    public static ClassNode bytesToClassNode(byte[] bytes, int flags) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, flags);
        return node;
    }

}
