package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

public class J_L_I_StringConcatFactory {

    @Modify(ref = @Ref(value = "java/lang/invoke/StringConcatFactory", member = "makeConcat", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"))
    public static void makeConcat(MethodNode mnode, int i, ClassNode cnode) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) mnode.instructions.get(i);
        Type[] args = Type.getArgumentTypes(indy.desc);
        char[] chars = new char[args.length];
        for (int j = 0; j < args.length; j++) {
            chars[j] = '\u0001';
        }
        InsnList list = makeConcatInternal3(cnode, new String(chars), new LinkedList<>(Arrays.asList(args)));
        mnode.instructions.insertBefore(indy, list);
        mnode.instructions.remove(indy);
    }

    @Modify(ref = @Ref(value = "java/lang/invoke/StringConcatFactory", member = "makeConcatWithConstants", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void makeConcatWithConstants(MethodNode mnode, int i, ClassNode cnode) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) mnode.instructions.get(i);
        Type[] args = Type.getArgumentTypes(indy.desc);
        String chars = (String) indy.bsmArgs[0];
        InsnList list = makeConcatInternal3(cnode, chars, new LinkedList<>(Arrays.asList(args)));
        mnode.instructions.insertBefore(indy, list);
        mnode.instructions.remove(indy);
    }

    /*
     * this method makes the following code:
     *  LDC "A"
     *  LDC 0L
     *  LDC 1.0D
     *  INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;JD)Ljava/lang/String; [
     *     java/lang/invoke/StringConcatFactory,
     *     "\u0001concat\u0001\u0001",
     *  ]
     *  ARETURN
     *
     *  into:
     *  LDC "A"
     *  LDC 0J
     *  LDC 1.0D
     *  INVOKESTATIC java/lang/String.valueOf (D)Ljava/lang/String;
     *  DUP_X2
     *  POP
     *  INVOKESTATIC java/lang/String.valueOf (J)Ljava/lang/String;
     *  SWAP
     *  INVOKEVIRTUAL java/lang/String.concat (Ljava/lang/String;)Ljava/lang/String;
     *  SWAP
     *  LDC "concat"
     *  SWAP
     *  INVOKEVIRTUAL java/lang/String.concat (Ljava/lang/String;)Ljava/lang/String;
     *  INVOKEVIRTUAL java/lang/String.concat (Ljava/lang/String;)Ljava/lang/String;
     *  ARETURN
     */
    private static InsnList makeConcatInternal(String args, Deque<Type> types) {
        InsnList list = new InsnList();
        if (types.isEmpty()) {
            list.add(new LdcInsnNode(args));
            return list;
        }
        // stack = [...types]
        if (args.endsWith("\u0001")) {
            Type last = types.removeLast();
            String type = last.getDescriptor();
            switch (last.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY:
                    // last is object
                    if (!type.equals("Ljava/lang/String;")) {
                        list.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "java/lang/String",
                            "valueOf",
                            "(Ljava/lang/Object;)Ljava/lang/String;",
                            false
                        ));
                    }
                    break;
                default:
                    // last is primitive
                    list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "java/lang/String",
                        "valueOf",
                        "(" + type + ")Ljava/lang/String;",
                        false
                    ));
                    break;
            }
            args = args.substring(0, args.length() - 1);
        } else {
            // last is literal
            int last = args.lastIndexOf('\u0001');
            if (last == -1) {
                // no literals
                list.add(new LdcInsnNode(args));
                if (!types.isEmpty()) {
                    throw new IllegalStateException("Types not empty!");
                }
                return list;
            }
            String literal = args.substring(last + 1);
            list.add(new LdcInsnNode(literal));
            args = args.substring(0, last + 1);
        }
        // stack = [...types, String]
        while (!args.isEmpty()) {
            if (args.endsWith("\u0001")) {
                Type last = types.removeLast();
                // stack = [...types, last, String]
                String type = last.getDescriptor();
                switch (last.getSort()) {
                    case Type.OBJECT:
                    case Type.ARRAY:
                        // last is object
                        if (!type.equals("Ljava/lang/String;")) {
                            list.add(new InsnNode(Opcodes.SWAP));
                            // stack = [...types, String, last]
                            list.add(new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "java/lang/String",
                                "valueOf",
                                "(Ljava/lang/Object;)Ljava/lang/String;",
                                false
                            ));
                            // stack = [...types, String, String(last)]
                            // or if last was String
                            // stack = [...types, last, String]
                        }
                        break;
                    default:
                        // last is primitive
                        switch (last.getSize()) {
                            case 1:
                                list.add(new InsnNode(Opcodes.SWAP));
                                break;
                            case 2:
                                list.add(new InsnNode(Opcodes.DUP_X2));
                                list.add(new InsnNode(Opcodes.POP));
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + last.getSize());
                        }
                        // stack = [...types, String. last]
                        list.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "java/lang/String",
                            "valueOf",
                            "(" + type + ")Ljava/lang/String;",
                            false
                        ));
                        // stack = [...types, String, String(last)]
                        break;
                }
                // we want to do last.concat(String)
                if (!type.equals("Ljava/lang/String;")) {
                    // stack = [...types, String, String(last)]
                    list.add(new InsnNode(Opcodes.SWAP));
                }
                // stack = [...types, String(last), String]
                list.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/String",
                    "concat",
                    "(Ljava/lang/String;)Ljava/lang/String;",
                    false
                ));
                // stack = [...types, String]
                args = args.substring(0, args.length() - 1);
            } else {
                // last is literal
                int last = args.lastIndexOf('\u0001');
                String literal = args.substring(last + 1);
                list.add(new LdcInsnNode(literal));
                // stack = [...types, String, literal]
                list.add(new InsnNode(Opcodes.SWAP));
                // stack = [...types, literal, String]
                list.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/String",
                    "concat",
                    "(Ljava/lang/String;)Ljava/lang/String;",
                    false
                ));
                // stack = [...types, String]
                if (last == -1) {
                    args = "";
                } else {
                    args = args.substring(0, last + 1);
                }
            }
        }
        if (types.isEmpty()) {
            return list;
        }
        throw new IllegalStateException("Types not empty!");
    }

    /*
     * This method makes the following code:
     * LDC "A"
     * LDC 0L
     * LDC 1.0D
     * INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;JD)Ljava/lang/String; [
     *   java/lang/invoke/StringConcatFactory,
     *   "\u0001concat\u0001\u0001",
     * ]
     * ARETURN
     *
     * into:
     * LDC "A"
     * LDC 0L
     * LDC 1.0D
     * NEW java/lang/StringBuilder
     * DUP
     * INVOKESPECIAL java/lang/StringBuilder.<init> ()V
     * DUP_X2
     * POP
     * INVOKEVIRTUAL java/lang/StringBuilder.append (D)Ljava/lang/StringBuilder;
     * ICONST_0
     * DUP2_X2
     * POP2
     * INVOKEVIRTUAL java/lang/StringBuilder.insert (IJ)Ljava/lang/StringBuilder;
     * ICONST_0
     * LDC "concat"
     * INVOKEVIRTUAL java/lang/StringBuilder.insert (ILjava/lang/String;)Ljava/lang/StringBuilder;
     * ICONST_0
     * DUP2_X1
     * INVOKEVIRTUAL java/lang/StringBuilder.insert (ILjava/lang/String;)Ljava/lang/StringBuilder;
     * INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
     * ARETURN
     */
    private static InsnList makeConcatInternal2(String args, Deque<Type> types) {
        InsnList list = new InsnList();
        if (types.isEmpty()) {
            list.add(new LdcInsnNode(args));
            return list;
        }
        // stack = [...types]
        list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        list.add(new InsnNode(Opcodes.DUP));
        // stack = [...types, StringBuilder, StringBuilder]
        if (args.endsWith("\u0001")) {
            Type last = types.removeLast();
            String type = last.getDescriptor();
            switch (last.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY:
                    // last is object
                    if (!type.equals("Ljava/lang/String;")) {
                        list.add(new MethodInsnNode(
                            Opcodes.INVOKESPECIAL,
                            "java/lang/StringBuilder",
                            "<init>",
                            "()V",
                            false
                        ));
                        // stack = [...types, last, StringBuilder]
                        list.add(new InsnNode(Opcodes.SWAP));
                        // stack = [...types, StringBuilder, last]
                        list.add(new MethodInsnNode(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                            false
                        ));
                        // stack = [...types, StringBuilder]
                    } else {
                        // stack = [...types, last, StringBuilder, StringBuilder]
                        list.add(new InsnNode(Opcodes.DUP_X2));
                        list.add(new InsnNode(Opcodes.POP));
                        // stack = [...types, StringBuilder, last ,StringBuilder]
                        list.add(new InsnNode(Opcodes.SWAP));
                        // stack = [...types, StringBuilder, StringBuilder, last]
                        list.add(new MethodInsnNode(
                            Opcodes.INVOKESPECIAL,
                            "java/lang/StringBuilder",
                            "<init>",
                            "(Ljava/lang/String;)V",
                            false
                        ));
                        // stack = [...types, StringBuilder]
                    }
                    break;
                default:
                    // last is primitive
                    list.add(new MethodInsnNode(
                        Opcodes.INVOKESPECIAL,
                        "java/lang/StringBuilder",
                        "<init>",
                        "()V",
                        false
                    ));
                    // stack = [...types, last, StringBuilder]
                    switch (last.getSize()) {
                        case 1:
                            list.add(new InsnNode(Opcodes.SWAP));
                            break;
                        case 2:
                            list.add(new InsnNode(Opcodes.DUP_X2));
                            list.add(new InsnNode(Opcodes.POP));
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + last.getSize());
                    }
                    // stack = [...types, StringBuilder, last]
                    list.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/StringBuilder",
                        "append",
                        "(" + type + ")Ljava/lang/StringBuilder;",
                        false
                    ));
                    // stack = [...types, StringBuilder]
                    break;
            }
            args = args.substring(0, args.length() - 1);
        } else {
            // last is literal
            int last = args.lastIndexOf('\u0001');
            if (last == -1) {
                InsnList list2 = new InsnList();
                list2.add(new LdcInsnNode(args));
                if (!types.isEmpty()) {
                    throw new IllegalStateException("Types not empty!");
                }
                return list2;
            }
            String literal = args.substring(last + 1);
            list.add(new LdcInsnNode(literal));
            list.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false
            ));
            args = args.substring(0, last + 1);
        }
        // stack = [...types, StringBuilder]
        while (!args.isEmpty()) {
            if (args.endsWith("\u0001")) {
                Type last = types.removeLast();
                String type = last.getDescriptor();
                list.add(new InsnNode(Opcodes.ICONST_0));
                // stack = [...types, last, StringBuilder, 0]
                switch (last.getSort()) {
                    case Type.OBJECT:
                    case Type.ARRAY:
                        // last is object
                        list.add(new InsnNode(Opcodes.DUP2_X1));
                        // stack = [...types, StringBuilder, 0, last, StringBuilder, 0]
                        list.add(new InsnNode(Opcodes.POP2));
                        // stack = [...types, StringBuilder, 0, last]
                        if (!type.equals("Ljava/lang/String;")) {
                            list.add(new MethodInsnNode(
                                Opcodes.INVOKEVIRTUAL,
                                "java/lang/StringBuilder",
                                "insert",
                                "(ILjava/lang/Object;)Ljava/lang/StringBuilder;",
                                false
                            ));
                        } else {
                            list.add(new MethodInsnNode(
                                Opcodes.INVOKEVIRTUAL,
                                "java/lang/StringBuilder",
                                "insert",
                                "(ILjava/lang/String;)Ljava/lang/StringBuilder;",
                                false
                            ));
                        }
                        break;
                    default:
                        // last is primitive
                        switch (last.getSize()) {
                            case 1:
                                list.add(new InsnNode(Opcodes.DUP2_X1));
                                list.add(new InsnNode(Opcodes.POP2));
                                break;
                            case 2:
                                list.add(new InsnNode(Opcodes.DUP2_X2));
                                list.add(new InsnNode(Opcodes.POP2));
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + last.getSize());
                        }
                        // stack = [...types, StringBuilder, 0, last]
                        list.add(new MethodInsnNode(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "insert",
                            "(I" + type + ")Ljava/lang/StringBuilder;",
                            false
                        ));
                }
                // stack = [...types, StringBuilder]
                args = args.substring(0, args.length() - 1);
            } else {
                list.add(new InsnNode(Opcodes.ICONST_0));
                // stack = [...types, StringBuilder, 0]
                int last = args.lastIndexOf('\u0001');
                String literal = args.substring(last + 1);
                list.add(new LdcInsnNode(literal));
                // stack = [...types, StringBuilder, 0, literal]
                list.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "insert",
                    "(ILjava/lang/String;)Ljava/lang/StringBuilder;",
                    false
                ));
                // stack = [...types, StringBuilder]
                if (last == -1) {
                    args = "";
                } else {
                    args = args.substring(0, last + 1);
                }
            }
        }
        list.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "toString",
            "()Ljava/lang/String;",
            false
        ));
        return list;
    }

    public static InsnList makeConcatInternal3(ClassNode node, String args, Deque<Type> types) {
        if (!args.contains("\u0001")) {
            // no args
            InsnList list = new InsnList();
            list.add(new LdcInsnNode(args));
            if (!types.isEmpty()) {
                throw new IllegalStateException("Types not empty!");
            }
        }
        // find if already exits
        int count = 0;
        String desc = Type.getMethodDescriptor(Type.getType(String.class), types.toArray(new Type[0]));
        for (MethodNode method : node.methods) {
            if (method.desc.equals(desc) && method instanceof StringConcatMethodNode) {
                StringConcatMethodNode concatMethod = (StringConcatMethodNode) method;
                if (concatMethod.args.equals(args)) {
                    InsnList list = new InsnList();
                    list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        node.name,
                        concatMethod.name,
                        concatMethod.desc,
                        false
                    ));
                    return list;
                }
                if (concatMethod.name.startsWith("jvmdowngrader$concat")) {
                    count++;
                }
            }
        }
        // create new
        StringConcatMethodNode method = new StringConcatMethodNode(args, types, count);
        node.methods.add(method);
        InsnList list = new InsnList();
        list.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            node.name,
            method.name,
            method.desc,
            false
        ));
        return list;
    }

    public static class StringConcatMethodNode extends MethodNode {

        public final String args;

        public StringConcatMethodNode(String args, Deque<Type> types, int index) {
            super(Opcodes.ASM9);
            this.args = args;
            this.name = "jvmdowngrader$concat";
            if (index > 0) {
                this.name += index;
            }
            this.access = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC;
            this.desc = Type.getMethodDescriptor(Type.getType(String.class), types.toArray(new Type[0]));
            init(args, types);
        }

        private void init(String args, Deque<Type> types) {
            visitCode();
            int index = 0;
            int typesIndex = 0;
            if (args.isEmpty()) {
                throw new IllegalArgumentException("args is empty");
            }
            visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            visitInsn(Opcodes.DUP);
//            int firstTypeSize = 0;
            if (args.charAt(index) == '\u0001') {
                Type first = types.removeFirst();
                String type = first.getDescriptor();
                switch (first.getSort()) {
                    case Type.OBJECT:
                    case Type.ARRAY:
                        if (type.equals("Ljava/lang/String;")) {
                            // stack = [StringBuilder, StringBuilder]
                            visitVarInsn(Opcodes.ALOAD, typesIndex);
                            // stack = [StringBuilder, StringBuilder, String]
                            visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
                        } else {
                            // stack = [StringBuilder, StringBuilder]
                            visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                            // stack = [StringBuilder]
                            visitVarInsn(Opcodes.ALOAD, typesIndex);
                            // stack = [StringBuilder, Object]
                            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
                        }
                        break;
                    default:
                        // stack = [StringBuilder, StringBuilder]
                        visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                        // stack = [StringBuilder]
                        visitVarInsn(first.getOpcode(Opcodes.ILOAD), 0);
                        // stack = [StringBuilder, type]
                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + type + ")Ljava/lang/StringBuilder;", false);
                }
                typesIndex += first.getSize();
                index++;
            } else {
                // first is literal
                int first = args.indexOf('\u0001');
                if (first == -1) {
                    throw new IllegalArgumentException("args is invalid");
                }
                String literal = args.substring(0, first);
                // stack = [StringBuilder, StringBuilder]
                visitLdcInsn(literal);
                // stack = [StringBuilder, String]
                visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
                index = first;
            }
            while (index < args.length()) {
                if (args.charAt(index) == '\u0001') {
                    Type type = types.removeFirst();
                    String desc = type.getDescriptor();
                    switch (type.getSort()) {
                        case Type.OBJECT:
                        case Type.ARRAY:
                            if (desc.equals("Ljava/lang/String;")) {
                                // stack = [StringBuilder]
                                visitVarInsn(Opcodes.ALOAD, typesIndex);
                                // stack = [StringBuilder, String]
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                            } else {
                                // stack = [StringBuilder]
                                visitVarInsn(Opcodes.ALOAD, typesIndex);
                                // stack = [StringBuilder, Object]
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
                            }
                            break;
                        default:
                            // stack = [StringBuilder]
                            visitVarInsn(type.getOpcode(Opcodes.ILOAD), typesIndex);
                            // stack = [StringBuilder, int]
                            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + desc + ")Ljava/lang/StringBuilder;", false);
                            break;
//                        case Type.LONG:
//                            // stack = [StringBuilder]
//                            visitVarInsn(type.getOpcode(Opcodes.LLOAD), typesIndex);
//                            // stack = [StringBuilder, long]
//                            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + desc + ")Ljava/lang/StringBuilder;", false);
//                            break;
//                        case Type.FLOAT:
//                            // stack = [StringBuilder]
//                            visitVarInsn(type.getOpcode(Opcodes.FLOAD), typesIndex);
//                            // stack = [StringBuilder, float]
//                            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + desc + ")Ljava/lang/StringBuilder;", false);
//                            break;
//                        case Type.DOUBLE:
//                            // stack = [StringBuilder]
//                            visitVarInsn(type.getOpcode(Opcodes.DLOAD), typesIndex);
//                            // stack = [StringBuilder, double]
//                            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + desc + ")Ljava/lang/StringBuilder;", false);
//                            break;
//                        default:
//                            throw new IllegalStateException("Unknown type: " + type);
                    }
                    typesIndex += type.getSize();
                    index++;
                } else {
                    int next = args.indexOf('\u0001', index);
                    if (next == -1) {
                        next = args.length();
                    }
                    String literal = args.substring(index, next);
                    // stack = [StringBuilder]
                    visitLdcInsn(literal);
                    // stack = [StringBuilder, String]
                    visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                    index = next;
                }
            }
            if (!types.isEmpty()) {
                throw new IllegalStateException("Types not empty!");
            }
            // stack = [StringBuilder]
            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            // stack = [String]
            visitInsn(Opcodes.ARETURN);
            visitMaxs(0, 0);
            visitEnd();
        }

    }
}
