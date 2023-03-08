package xyz.wagyourtail.jvmdg.j9.stub;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.replace.Replace;

import java.util.*;

public class J_L_I_StringConcatFactory {

    @Replace(javaVersion = Opcodes.V9, ref = @Ref(value = "java/lang/invoke/StringConcatFactory", member = "makeConcat", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"), idBSM = true)
    public static void makeConcat(ClassNode cnode, MethodNode mnode, int i) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) mnode.instructions.get(i);
        Type[] args = Type.getArgumentTypes(indy.desc);
        char[] chars = new char[args.length];
        for (int j = 0; j < args.length; j++) {
            chars[j] = '\u0001';
        }
        InsnList list = makeConcatInternal2(new String(chars), new LinkedList<>(Arrays.asList(args)));
        mnode.instructions.insertBefore(indy, list);
        mnode.instructions.remove(indy);
    }

    @Replace(javaVersion = Opcodes.V9, ref = @Ref(value = "java/lang/invoke/StringConcatFactory", member = "makeConcatWithConstants", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"), idBSM = true)
    public static void makeConcatWithConstants(ClassNode cnode, MethodNode mnode, int i) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) mnode.instructions.get(i);
        Type[] args = Type.getArgumentTypes(indy.desc);
        String chars = (String) indy.bsmArgs[0];
        InsnList list = makeConcatInternal2(chars, new LinkedList<>(Arrays.asList(args)));
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
}
