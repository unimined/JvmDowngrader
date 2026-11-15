package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.version.IndyDesugar;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.ModifyContext;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

@JEP(280)
public class J_L_I_StringConcatFactory {

    @IndyDesugar(ref = @Ref(value = "java/lang/invoke/StringConcatFactory", member = "makeConcat", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"))
    public static void makeConcat(ModifyContext ctx) {
        MethodNode mnode = ctx.method;
        ClassNode cnode = ctx.node;
        boolean noSynthetic = ctx.noSynthetic;
        InvokeDynamicInsnNode indy = ctx.indy;
        Type[] args = Type.getArgumentTypes(indy.desc);
        char[] chars = new char[args.length];
        for (int j = 0; j < args.length; j++) {
            chars[j] = '\u0001';
        }
        InsnList list = makeConcatInternal3(mnode.name, cnode, new String(chars), new ArrayDeque<>(Arrays.asList(args)), new ArrayDeque<>(), noSynthetic);
        mnode.instructions.insertBefore(indy, list);
        mnode.instructions.remove(indy);
    }

    @IndyDesugar(ref = @Ref(value = "java/lang/invoke/StringConcatFactory", member = "makeConcatWithConstants", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void makeConcatWithConstants(ModifyContext ctx) {
        MethodNode mnode = ctx.method;
        ClassNode cnode = ctx.node;
        boolean noSynthetic = ctx.noSynthetic;
        InvokeDynamicInsnNode indy = ctx.indy;
        Type[] args = Type.getArgumentTypes(indy.desc);
        Deque<Object> bsmArgs = new ArrayDeque<>(Arrays.asList(indy.bsmArgs));
        String chars = (String) bsmArgs.removeFirst();
        InsnList list = makeConcatInternal3(mnode.name, cnode, chars, new ArrayDeque<>(Arrays.asList(args)), bsmArgs, noSynthetic);
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
    private static final MethodHandle CONCAT;
    private static final MethodHandle CONCAT_CONST;

    static {
        try {
            CONCAT = MethodHandles.lookup().findStatic(J_L_I_StringConcatFactory.class, "concat",
                MethodType.methodType(String.class, Object[].class));
            CONCAT_CONST = MethodHandles.lookup().findStatic(J_L_I_StringConcatFactory.class,
                "concatWithConstants", MethodType.methodType(String.class, String.class,
                    Object[].class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Runtime impl for a direct {@code makeConcat} invoke — the indy
     * callsite path is desugared by {@link #makeConcat(ModifyContext)}.
     * The callsite's target is a {@code concat(Object[])} spreader
     * {@code asType}'d onto the callsite desc, so primitives auto-box.
     */
    @Stub(ref = @Ref(value = "java/lang/invoke/StringConcatFactory", member = "makeConcat", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"))
    public static CallSite makeConcatStub(MethodHandles.Lookup lookup, String name, MethodType concatType) {
        return new ConstantCallSite(CONCAT
            .asCollector(Object[].class, concatType.parameterCount())
            .asType(concatType));
    }

    @Stub(ref = @Ref(value = "java/lang/invoke/StringConcatFactory", member = "makeConcatWithConstants", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static CallSite makeConcatWithConstantsStub(MethodHandles.Lookup lookup, String name, MethodType concatType, String recipe, Object... constants) {
        MethodHandle bound = MethodHandles.insertArguments(CONCAT_CONST, 0, recipe,
            (Object) constants);
        return new ConstantCallSite(bound
            .asCollector(Object[].class, concatType.parameterCount())
            .asType(concatType));
    }

    public static String concat(Object[] args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            sb.append(arg);
        }
        return sb.toString();
    }

    /**
     * StringConcatFactory recipe expansion: {@code \\u0001} consumes the
     * next dynamic arg, {@code \\u0002} the next constant, everything else
     * is literal text.
     */
    public static String concatWithConstants(String recipe, Object[] constants, Object[] args) {
        StringBuilder sb = new StringBuilder();
        int ai = 0, ci = 0;
        for (int i = 0; i < recipe.length(); i++) {
            char c = recipe.charAt(i);
            if (c == '\u0001') {
                sb.append(args[ai++]);
            } else if (c == '\u0002') {
                sb.append(constants[ci++]);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

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
                    // stack = [...types, last, StringBuilder, StringBuilder]
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
                            "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
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

    public static InsnList makeConcatInternal3(String mname, ClassNode node, String args, Deque<Type> types, Deque<Object> bsmArgs, boolean noSynthetic) {
        mname = mname.replace("<", "$").replace(">", "$");
        if (!args.contains("\u0001")) {
            // no args
            InsnList list = new InsnList();
            list.add(new LdcInsnNode(args));
            if (!types.isEmpty()) {
                throw new IllegalStateException("Types not empty!");
            }
        }
        // find if already exits
        int count = 1;
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
                        (node.access & Opcodes.ACC_INTERFACE) != 0
                    ));
                    return list;
                }
                if (concatMethod.name.startsWith("jvmdowngrader$concat$" + mname)) {
                    count++;
                }
            }
        }
        // create new
        StringConcatMethodNode method = new StringConcatMethodNode(mname, args, types, bsmArgs, count, noSynthetic);
        node.methods.add(method);
        InsnList list = new InsnList();
        list.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            node.name,
            method.name,
            method.desc,
            (node.access & Opcodes.ACC_INTERFACE) != 0
        ));
        return list;
    }

    public static class StringConcatMethodNode extends MethodNode {

        public final String args;

        public StringConcatMethodNode(String mname, String args, Deque<Type> types, Deque<Object> bsmArgs, int index, boolean noSynthetic) {
            super(Opcodes.ASM9);
            this.args = args;
            this.name = "jvmdowngrader$concat$" + mname + "$" + index;
            this.access = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | (noSynthetic ? 0 : Opcodes.ACC_SYNTHETIC);
            this.desc = Type.getMethodDescriptor(Type.getType(String.class), types.toArray(new Type[0]));
            init(args, types, bsmArgs);
        }

        private void init(String args, Deque<Type> types, Deque<Object> bsmArgs) {
            visitCode();
            int index = 0;
            int typesIndex = 0;
            if (args.isEmpty()) {
                throw new IllegalArgumentException("args is empty");
            }
            // stack = []
            visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            // stack = [StringBuilder]
            visitInsn(Opcodes.DUP);
            // stack = [StringBuilder, StringBuilder]
            visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            // stack = [StringBuilder]
            while (index < args.length()) {
                switch (args.charAt(index)) {
                    case '\u0001':
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
                            case Type.BYTE:
                            case Type.SHORT:
                            case Type.INT:
                                // stack = [StringBuilder]
                                visitVarInsn(Opcodes.ILOAD, typesIndex);
                                // stack = [StringBuilder, "int"]
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
                                break;
                            default:
                                // stack = [StringBuilder]
                                visitVarInsn(type.getOpcode(Opcodes.ILOAD), typesIndex);
                                // stack = [StringBuilder, int]
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + desc + ")Ljava/lang/StringBuilder;", false);
                                break;
                        }
                        typesIndex += type.getSize();
                        index++;
                        break;
                    case '\u0002':
                        // stack = [StringBuilder]
                        Object constant = bsmArgs.removeFirst();

                        // stack = [StringBuilder, ?]
                        if (constant instanceof String) {
                            visitLdcInsn(constant);
                        } else if (constant instanceof Integer) {
                            visitLdcInsn(constant);
                            visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);
                        } else if (constant instanceof Long) {
                            visitLdcInsn(constant);
                            visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(J)Ljava/lang/String;", false);
                        } else if (constant instanceof Float) {
                            visitLdcInsn(constant);
                            visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(F)Ljava/lang/String;", false);
                        } else if (constant instanceof Double) {
                            visitLdcInsn(constant);
                            visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(D)Ljava/lang/String;", false);
                        } else {
                            visitLdcInsn(String.valueOf(constant));
                        }

                        // stack = [StringBuilder, String]
                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                        index++;
                        break;
                    default:
                        int nextStack = args.indexOf('\u0001', index);
                        if (nextStack == -1) {
                            nextStack = args.length();
                        }
                        int nextArg = args.indexOf('\u0002', index);
                        if (nextArg == -1) {
                            nextArg = args.length();
                        }
                        int next = Math.min(nextStack, nextArg);
                        String literal = args.substring(index, next);
                        // stack = [StringBuilder]
                        visitLdcInsn(literal);
                        // stack = [StringBuilder, String]
                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                        index = next;
                        break;
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
