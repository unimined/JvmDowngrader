package xyz.wagyourtail.jvmdg.asm;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

public class ASMUtils {

    public static Handle boxType(Type type) {
        Type box = getBoxFor(type);
        if (box != null) {
            return new Handle(
                Opcodes.H_INVOKESTATIC,
                box.getInternalName(),
                "valueOf",
                Type.getMethodDescriptor(box, type),
                false
            );
        }
        return null;
    }

    public static void boxType(MethodVisitor visitor, Type type) {
        Handle box = boxType(type);
        if (box != null) {
            visitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                box.getOwner(),
                box.getName(),
                box.getDesc(),
                false
            );
        }
    }

    public static Handle unboxType(Type type) {
        switch (type.getInternalName()) {
            case "java/lang/Boolean":
                return new Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    "java/lang/Boolean",
                    "booleanValue",
                    "()Z",
                    false
                );
            case "java/lang/Byte":
                return new Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    "java/lang/Byte",
                    "byteValue",
                    "()B",
                    false
                );
            case "java/lang/Character":
                return new Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    "java/lang/Character",
                    "charValue",
                    "()C",
                    false
                );
            case "java/lang/Short":
                return new Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    "java/lang/Short",
                    "shortValue",
                    "()S",
                    false
                );
            case "java/lang/Integer":
                return new Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    "java/lang/Integer",
                    "intValue",
                    "()I",
                    false
                );
            case "java/lang/Long":
                return new Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    "java/lang/Long",
                    "longValue",
                    "()J",
                    false
                );
            case "java/lang/Float":
                return new Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    "java/lang/Float",
                    "floatValue",
                    "()F",
                    false
                );
            case "java/lang/Double":
                return new Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    "java/lang/Double",
                    "doubleValue",
                    "()D",
                    false
                );
            default:
                return null;
        }
    }

    public static void unboxType(MethodVisitor visitor, Type type) {
        Handle h = unboxType(type);
        if (h != null) {
            visitor.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                h.getOwner(),
                h.getName(),
                h.getDesc(),
                false
            );
        }
    }

    public static int toHandleTag(int opcode) {
        switch (opcode) {
            case Opcodes.GETFIELD:
                return Opcodes.H_GETFIELD;
            case Opcodes.GETSTATIC:
                return Opcodes.H_GETSTATIC;
            case Opcodes.PUTFIELD:
                return Opcodes.H_PUTFIELD;
            case Opcodes.PUTSTATIC:
                return Opcodes.H_PUTSTATIC;

            case Opcodes.INVOKEVIRTUAL:
                return Opcodes.H_INVOKEVIRTUAL;
            case Opcodes.INVOKESTATIC:
                return Opcodes.H_INVOKESTATIC;
            case Opcodes.INVOKESPECIAL:
                return Opcodes.H_INVOKESPECIAL;
            case Opcodes.INVOKEINTERFACE:
                return Opcodes.H_INVOKEINTERFACE;
        }
        if (opcode >= Opcodes.H_GETFIELD && opcode <= Opcodes.H_INVOKEINTERFACE) {
            return opcode;
        }
        throw new IllegalArgumentException("Not a valid opcode. " + opcode);
    }

    public static int fromHandleTag(int h_tag) {
        switch (h_tag) {
            case Opcodes.H_GETFIELD:
                return Opcodes.GETFIELD;
            case Opcodes.H_GETSTATIC:
                return Opcodes.GETSTATIC;
            case Opcodes.H_PUTFIELD:
                return Opcodes.PUTFIELD;
            case Opcodes.H_PUTSTATIC:
                return Opcodes.PUTSTATIC;

            case Opcodes.H_INVOKEVIRTUAL:
                return Opcodes.INVOKEVIRTUAL;
            case Opcodes.H_INVOKESTATIC:
                return Opcodes.INVOKESTATIC;
            case Opcodes.H_INVOKESPECIAL:
            case Opcodes.H_NEWINVOKESPECIAL:
                return Opcodes.INVOKESPECIAL;
            case Opcodes.H_INVOKEINTERFACE:
                return Opcodes.H_INVOKEINTERFACE;
        }
        throw new IllegalArgumentException("Not a valid handle tag. " + h_tag);
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
            case Type.VOID:
                return Type.getObjectType("java/lang/Void");
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

    public static byte[] classNodeToBytes(@NotNull final ClassNode node) {
        ClassWriter cw = new ClassWriter(0);
        node.accept(cw);
        return cw.toByteArray();
    }

    public static byte[] classNodeToBytes(@NotNull final ClassNode node, int flags) {
        ClassWriter cw = new ClassWriter(flags);
        node.accept(cw);
        return cw.toByteArray();
    }

}
