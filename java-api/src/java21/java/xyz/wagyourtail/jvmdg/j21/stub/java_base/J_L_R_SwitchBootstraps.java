package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@JEP(441)
public class J_L_R_SwitchBootstraps {

    @IndyDesugar(ref = @Ref(value = "java/lang/runtime/SwitchBootstraps", member = "typeSwitch", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void makeTypeSwitch(ModifyContext ctx) {
        MethodNode mnode = ctx.method;
        ClassNode cnode = ctx.node;
        boolean noSynthetic = ctx.noSynthetic;
        InvokeDynamicInsnNode indy = ctx.indy;
        List<Object> types = new ArrayList<>();
        for (Object bsmArg : indy.bsmArgs) {
            if (bsmArg instanceof ConstantDynamic condy) {
                String type = condy.getDescriptor();
                if (!type.equals("Ljava/lang/Enum$EnumDesc;")) {
                    throw new IllegalStateException("Unknown condy type for switch: " + type);
                }
                ConstantDynamic cls = (ConstantDynamic) condy.getBootstrapMethodArgument(1);
                String clsName = (String) cls.getBootstrapMethodArgument(1);
                String value = (String) condy.getBootstrapMethodArgument(2);
                types.add(new EnumType(Type.getObjectType(clsName.replace('.', '/')), value));
            } else {
                types.add(bsmArg);
            }
        }
        MethodInsnNode min = makeSwitchInternal(mnode.name, indy.desc, types, cnode, noSynthetic);
        mnode.instructions.set(indy, min);
    }

    @IndyDesugar(ref = @Ref(value = "java/lang/runtime/SwitchBootstraps", member = "enumSwitch", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void makeEnumSwitch(ModifyContext ctx) {
        MethodNode mnode = ctx.method;
        ClassNode cnode = ctx.node;
        boolean noSynthetic = ctx.noSynthetic;
        InvokeDynamicInsnNode indy = ctx.indy;
        Type[] argTypes = Type.getArgumentTypes(indy.desc);
        List<Object> types = new ArrayList<>();
        for (Object bsmArg : indy.bsmArgs) {
            if (bsmArg instanceof String value) {
                types.add(new EnumType(argTypes[0], value));
            } else {
                types.add(bsmArg);
            }
        }
        MethodInsnNode min = makeSwitchInternal(mnode.name, indy.desc, types, cnode, noSynthetic);
        mnode.instructions.set(indy, min);
    }

    private static final MethodHandle TYPE_SWITCH;
    private static final MethodHandle ENUM_SWITCH;

    static {
        try {
            TYPE_SWITCH = MethodHandles.lookup().findStatic(J_L_R_SwitchBootstraps.class,
                "typeSwitchImpl", MethodType.methodType(int.class, MethodHandles.Lookup.class,
                    Object[].class, Object.class, int.class));
            ENUM_SWITCH = MethodHandles.lookup().findStatic(J_L_R_SwitchBootstraps.class,
                "enumSwitchImpl", MethodType.methodType(int.class, MethodHandles.Lookup.class,
                    Class.class, Object[].class, Object.class, int.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Runtime impl for a direct {@code typeSwitch} invoke — the indy
     * callsite path is desugared by {@link #makeTypeSwitch(ModifyContext)}.
     * Label match mirrors {@link #makeSwitchInternal}: {@code null} value
     * returns -1, {@code restart} resumes the label scan, labels are enum
     * name / String / int-widening / retro-instanceof checks.
     */
    @Stub(ref = @Ref(value = "java/lang/runtime/SwitchBootstraps", member = "typeSwitch", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static CallSite typeSwitchStub(MethodHandles.Lookup lookup, String invocationName, MethodType invocationType, Object... labels) {
        MethodHandle mh = MethodHandles.insertArguments(TYPE_SWITCH, 0, lookup, (Object) labels);
        return new ConstantCallSite(mh.asType(invocationType));
    }

    @Stub(ref = @Ref(value = "java/lang/runtime/SwitchBootstraps", member = "enumSwitch", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static CallSite enumSwitchStub(MethodHandles.Lookup lookup, String invocationName, MethodType invocationType, Object... labels) {
        MethodHandle mh = MethodHandles.insertArguments(ENUM_SWITCH, 0, lookup,
            invocationType.parameterType(0), (Object) labels);
        return new ConstantCallSite(mh.asType(invocationType));
    }

    public static int typeSwitchImpl(MethodHandles.Lookup lookup, Object[] labels, Object value,
                                     int restart) throws Throwable {
        if (value == null) {
            return -1;
        }
        for (int i = restart; i < labels.length; i++) {
            if (matchesLabel(lookup, labels[i], value)) {
                return i;
            }
        }
        return labels.length;
    }

    public static int enumSwitchImpl(MethodHandles.Lookup lookup, Class<?> enumClass, Object[] labels,
                                     Object value, int restart) throws Throwable {
        if (value == null) {
            return -1;
        }
        String name = value instanceof Enum ? ((Enum<?>) value).name() : null;
        for (int i = restart; i < labels.length; i++) {
            Object label = labels[i];
            if (label instanceof String) {
                // enumSwitch String labels are enum constant names —
                // EnumType in the desugar
                if (enumClass.isInstance(value) && label.equals(name)) {
                    return i;
                }
            } else if (matchesLabel(lookup, label, value)) {
                return i;
            }
        }
        return labels.length;
    }

    private static boolean matchesLabel(MethodHandles.Lookup lookup, Object label, Object value)
        throws Throwable {
        if (label instanceof Class) {
            // mirror the desugar's Type branch: route through the
            // retro-supertype membership table so labels naming types
            // that gained supertypes still match
            Class<?> cls = (Class<?>) label;
            boolean array = cls.isArray();
            String name = array ? cls.getComponentType().getName() : cls.getName();
            return xyz.wagyourtail.jvmdg.stub.RetroTypes.isInstanceOrFallback(value,
                name.replace('.', '/'), cls.getName(), lookup.lookupClass(), array);
        } else if (label instanceof String) {
            return label.equals(value);
        } else if (label instanceof Integer) {
            int l = (Integer) label;
            return (value instanceof Number && ((Number) value).intValue() == l) ||
                (value instanceof Character && ((Character) value) == l);
        } else if (label != null && label.getClass().getName().equals("java.lang.Enum$EnumDesc")) {
            // EnumDesc condy label — reflective read keeps a constant-pool
            // edge to the j12+ type out of the stub
            String constantName = (String) label.getClass().getMethod("constantName").invoke(label);
            Object constantType = label.getClass().getMethod("constantType").invoke(label);
            Class<?> enumClass = (Class<?>) constantType.getClass()
                .getMethod("resolveConstantDesc", MethodHandles.Lookup.class)
                .invoke(constantType, lookup);
            return enumClass.isInstance(value) && ((Enum<?>) value).name().equals(constantName);
        } else {
            throw new IllegalStateException("Unknown type for switch: " + label);
        }
    }

    public static MethodInsnNode makeSwitchInternal(String holdingMethod, String desc, List<Object> types, ClassNode cnode, boolean noSynthetic) {
        holdingMethod = holdingMethod.replace("<", "$").replace(">", "$");
        int i = 0;
        for (MethodNode mnode : cnode.methods) {
            if (mnode instanceof SwitchMethodNode smnode) {
                if (smnode.types.equals(types)) {
                    return new MethodInsnNode(Opcodes.INVOKESTATIC, cnode.name, mnode.name, mnode.desc, (cnode.access & Opcodes.ACC_INTERFACE) != 0);
                }
                if (mnode.name.equals("jvmdowngrader$switch$" + holdingMethod + "$" + i)) {
                    i++;
                }
            }
        }

        String name = "jvmdowngrader$switch$" + holdingMethod + "$" + i;
        SwitchMethodNode smnode = new SwitchMethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | (noSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), name, desc, types);
        smnode.visitCode();

        // private static <name>(Object value, int start) {
        //     Objects.checkIndex(start, types.size() + 1);
        //     if (value == null) {
        //         return -1;
        //     }
        //     switch(start) {
        //         case 0:

        //     }
        // }
//        smnode.visitVarInsn(Opcodes.ILOAD, 1);
        // stack: start
//        smnode.visitLdcInsn(types.size());
        // stack: start, types.size()
//        smnode.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "checkIndex", "(II)I", false);

        // inline checkIndex
        smnode.visitVarInsn(Opcodes.ILOAD, 1);
        // stack: start
        var l1 = new Label();
        smnode.visitJumpInsn(Opcodes.IFLT, l1);
        smnode.visitVarInsn(Opcodes.ILOAD, 1);
        // stack: start
        smnode.visitLdcInsn(types.size() + 1);
        // stack: start, types.size()
        smnode.visitJumpInsn(Opcodes.IF_ICMPGE, l1);
        var l2 = new Label();
        smnode.visitJumpInsn(Opcodes.GOTO, l2);
        smnode.visitLabel(l1);
        smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        smnode.visitTypeInsn(Opcodes.NEW, "java/lang/IndexOutOfBoundsException");
        // stack: (U) IOOBE
        smnode.visitInsn(Opcodes.DUP);
        // stack: IOOBE, IOOBE
        // string concat factory with constants
        smnode.visitVarInsn(Opcodes.ILOAD, 1);
        // stack: IOOBE, IOOBE, start
        smnode.visitLdcInsn(types.size() + 1);
        // stack: IOOBE, IOOBE, start, types.size()
        smnode.visitInvokeDynamicInsn(
            "makeConcatWithConstants",
            "(II)Ljava/lang/String;",
            new Handle(
                Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/StringConcatFactory",
                "makeConcatWithConstants",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
                false
            ),
            "Index " + "\u0001" + " out of bounds for length " + "\u0001"
        );
        // stack: IOOBE, IOOBE, message
        smnode.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IndexOutOfBoundsException", "<init>", "(Ljava/lang/String;)V", false);
        // stack: IOOBE
        smnode.visitInsn(Opcodes.ATHROW);

        smnode.visitLabel(l2);
        smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        // stack:
        smnode.visitVarInsn(Opcodes.ALOAD, 0);
        var l0 = new Label();
        // stack: value
        smnode.visitJumpInsn(Opcodes.IFNONNULL, l0);
        // stack:
        smnode.visitInsn(Opcodes.ICONST_M1);
        // stack: -1
        var retLabel = new Label();
        smnode.visitJumpInsn(Opcodes.GOTO, retLabel);
        smnode.visitLabel(l0);
        smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        List<CaseLabels> labels = new ArrayList<>();
        List<Label> startLabels = new ArrayList<>();
        for (int j = 0; j < types.size(); j++) {
            CaseLabels caseLabels = CaseLabels.create();
            labels.add(caseLabels);
            startLabels.add(caseLabels.start);
        }
        Label def = new Label();

        if (!labels.isEmpty()) {
            smnode.visitVarInsn(Opcodes.ILOAD, 1);
            smnode.visitTableSwitchInsn(0, labels.size() - 1, def, startLabels.toArray(new Label[0]));

            for (int j = 0; j < types.size(); j++) {
                var caseLabels = labels.get(j);
                smnode.visitLabel(caseLabels.start);
                smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                var type = types.get(j);
                if (type instanceof EnumType et) {
                    // lookahead if multiple EnumTypes in a row
                    int k = j + 1;
                    while (k < types.size() && types.get(k) instanceof EnumType ket && ket.type.equals(et.type)) {
                        k++;
                    }
                    k--;
                    var lastCaseLabels = labels.get(k);

                    // check if is of type
                    smnode.visitVarInsn(Opcodes.ALOAD, 0);
                    smnode.visitTypeInsn(Opcodes.INSTANCEOF, et.type.getInternalName());
                    smnode.visitJumpInsn(Opcodes.IFEQ, lastCaseLabels.end);

                    if (k != j) {
                        Set<HashWithLabel> hashWithLabels = new TreeSet<>();
                        for (int l = j; l <= k; l++) {
                            EnumType ket = (EnumType) types.get(l);
                            hashWithLabels.add(new HashWithLabel(ket.value.hashCode(), labels.get(l).afterTypeCheck));
                        }

                        if (hashWithLabels.size() > 1) {
                            // cast
                            smnode.visitVarInsn(Opcodes.ALOAD, 0);
                            smnode.visitTypeInsn(Opcodes.CHECKCAST, et.type.getInternalName());
                            // get name
                            smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, et.type.getInternalName(), "name", "()Ljava/lang/String;", false);
                            // hashCode
                            smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);

                            HashWithLabel[] hashWithLabelsArr = hashWithLabels.toArray(new HashWithLabel[0]);
                            int[] hashCodes = new int[hashWithLabels.size()];
                            Label[] tableLabels = new Label[hashWithLabels.size()];
                            for (int l = 0; l < hashWithLabelsArr.length; l++) {
                                hashCodes[l] = hashWithLabelsArr[l].hash;
                                tableLabels[l] = hashWithLabelsArr[l].label;
                            }
                            smnode.visitLookupSwitchInsn(lastCaseLabels.end, hashCodes, tableLabels);
                        }
                    }
                    // load enum value
                    smnode.visitLabel(caseLabels.afterTypeCheck);
                    smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    smnode.visitLdcInsn(et.value);
                    smnode.visitVarInsn(Opcodes.ALOAD, 0);
                    smnode.visitTypeInsn(Opcodes.CHECKCAST, et.type.getInternalName());
                    smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, et.type.getInternalName(), "name", "()Ljava/lang/String;", false);
                    // equals
                    smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                    // if true return index
                    smnode.visitJumpInsn(Opcodes.IFEQ, caseLabels.end);
                    smnode.visitLdcInsn(j);
                    smnode.visitJumpInsn(Opcodes.GOTO, retLabel);
                } else if (type instanceof String) {
                    // lookahead if multiple Strings in a row
                    int k = j + 1;
                    while (k < types.size() && types.get(k) instanceof String) {
                        k++;
                    }
                    k--;
                    if (k != j) {
                        var lastCaseLabels = labels.get(k);

                        Set<HashWithLabel> hashWithLabels = new TreeSet<>();
                        for (int l = j; l <= k; l++) {
                            hashWithLabels.add(new HashWithLabel(types.get(l).hashCode(), labels.get(l).afterTypeCheck));
                        }
                        if (hashWithLabels.size() > 1) {

                            // check if is of type
                            smnode.visitVarInsn(Opcodes.ALOAD, 0);
                            smnode.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/String");
                            smnode.visitJumpInsn(Opcodes.IFEQ, lastCaseLabels.end);
                            // hashCode
                            smnode.visitVarInsn(Opcodes.ALOAD, 0);
                            smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false);

                            HashWithLabel[] hashWithLabelsArr = hashWithLabels.toArray(new HashWithLabel[0]);
                            int[] hashCodes = new int[hashWithLabels.size()];
                            Label[] tableLabels = new Label[hashWithLabels.size()];
                            for (int l = 0; l < hashWithLabelsArr.length; l++) {
                                hashCodes[l] = hashWithLabelsArr[l].hash;
                                tableLabels[l] = hashWithLabelsArr[l].label;
                            }

                            smnode.visitLookupSwitchInsn(lastCaseLabels.end, hashCodes, tableLabels);
                        }
                    }
                    smnode.visitLabel(caseLabels.afterTypeCheck);
                    smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    // load string
                    smnode.visitLdcInsn(type);
                    smnode.visitVarInsn(Opcodes.ALOAD, 0);
                    // equals
                    smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false);
                    // if true return index
                    smnode.visitJumpInsn(Opcodes.IFEQ, caseLabels.end);
                    smnode.visitLdcInsn(j);
                    smnode.visitJumpInsn(Opcodes.GOTO, retLabel);
                } else if (type instanceof Integer) {
                    // lookahead if multiple ints in a row
                    int k = j + 1;
                    while (k < types.size() && types.get(k) instanceof Integer) {
                        k++;
                    }
                    k--;
                    if (k != j) {
                        var lastCaseLabels = labels.get(k);

                        Set<HashWithLabel> hashWithLabels = new TreeSet<>();
                        for (int l = j; l <= k; l++) {
                            hashWithLabels.add(new HashWithLabel(types.get(l).hashCode(), labels.get(l).afterTypeCheck));
                        }

                        if (hashWithLabels.size() > 1) {

                            // check if is of type
                            smnode.visitVarInsn(Opcodes.ALOAD, 0);
                            smnode.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/Number");
                            smnode.visitVarInsn(Opcodes.ALOAD, 0);
                            smnode.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/Character");
                            smnode.visitInsn(Opcodes.IOR);
                            smnode.visitJumpInsn(Opcodes.IFEQ, lastCaseLabels.end);
                            // hashCode
                            smnode.visitVarInsn(Opcodes.ALOAD, 0);
                            smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false);

                            HashWithLabel[] hashWithLabelsArr = hashWithLabels.toArray(new HashWithLabel[0]);
                            int[] hashCodes = new int[hashWithLabels.size()];
                            Label[] tableLabels = new Label[hashWithLabels.size()];
                            for (int l = 0; l < hashWithLabelsArr.length; l++) {
                                hashCodes[l] = hashWithLabelsArr[l].hash;
                                tableLabels[l] = hashWithLabelsArr[l].label;
                            }

                            smnode.visitLookupSwitchInsn(lastCaseLabels.end, hashCodes, tableLabels);
                        }
                    }

                    smnode.visitLabel(caseLabels.afterTypeCheck);
                    smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    // load int
                    // if (value instanceof Number v && v.intValue() == type.intValue()) {
                    //     return j;
                    smnode.visitVarInsn(Opcodes.ALOAD, 0);
                    smnode.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/Number");
                    var notnum = new Label();
                    smnode.visitJumpInsn(Opcodes.IFEQ, notnum);
                    smnode.visitVarInsn(Opcodes.ALOAD, 0);
                    smnode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
                    smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
                    smnode.visitLdcInsn(type);
                    smnode.visitJumpInsn(Opcodes.IF_ICMPEQ, notnum);
                    smnode.visitLdcInsn(j);
                    smnode.visitJumpInsn(Opcodes.GOTO, retLabel);
                    smnode.visitLabel(notnum);
                    // } else if (value instanceof Character v && v.charValue() == type.intValue()) {
                    //     return j;
                    // }
                    smnode.visitVarInsn(Opcodes.ALOAD, 0);
                    smnode.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/Character");
                    smnode.visitJumpInsn(Opcodes.IFEQ, caseLabels.end);
                    smnode.visitVarInsn(Opcodes.ALOAD, 0);
                    smnode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
                    smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                    smnode.visitLdcInsn(type);
                    smnode.visitJumpInsn(Opcodes.IF_ICMPEQ, caseLabels.end);
                    smnode.visitLdcInsn(j);
                    smnode.visitJumpInsn(Opcodes.GOTO, retLabel);
                } else if (type instanceof Type) {
                    // labels keep their source type (the transform leaves
                    // typeSwitch args unremapped) so a stubbed label routes
                    // through the retro-supertype membership table by name;
                    // plain labels resolve lazily — emitting instanceof on
                    // a possibly-missing type would fail verification
                    Type label = (Type) type;
                    boolean array = label.getSort() == Type.ARRAY;
                    String memberName = array
                        ? label.getElementType().getInternalName()
                        : label.getInternalName();
                    String fallbackName = array
                        ? label.getDescriptor().replace('/', '.')
                        : memberName.replace('/', '.');
                    smnode.visitVarInsn(Opcodes.ALOAD, 0);
                    smnode.visitLdcInsn(memberName);
                    smnode.visitLdcInsn(fallbackName);
                    smnode.visitLdcInsn(Type.getObjectType(cnode.name));
                    smnode.visitInsn(array ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
                    smnode.visitMethodInsn(Opcodes.INVOKESTATIC, "xyz/wagyourtail/jvmdg/stub/RetroTypes",
                        "isInstanceOrFallback",
                        "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;Z)Z", false);
                    smnode.visitJumpInsn(Opcodes.IFEQ, caseLabels.end);
                    smnode.visitLdcInsn(j);
                    smnode.visitJumpInsn(Opcodes.GOTO, retLabel);
                } else {
                    throw new IllegalStateException("Unknown type for switch: " + type);
                }
                smnode.visitLabel(caseLabels.end);
                smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
        }

        smnode.visitLabel(def);
        smnode.visitLdcInsn(types.size());
        smnode.visitLabel(retLabel);
        smnode.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
        smnode.visitInsn(Opcodes.IRETURN);

        smnode.visitMaxs(0, 0);
        smnode.visitEnd();
        cnode.methods.add(smnode);
        return new MethodInsnNode(Opcodes.INVOKESTATIC, cnode.name, name, desc, (cnode.access & Opcodes.ACC_INTERFACE) != 0);
    }


    private record EnumType(Type type, String value) {
    }

    private static class SwitchMethodNode extends MethodNode {
        private final List<Object> types;

        public SwitchMethodNode(int access, String name, String descriptor, List<Object> types) {
            super(Opcodes.ASM9, access, name, descriptor, null, null);
            this.types = types;
        }

    }

    private record HashWithLabel(int hash, Label label) implements Comparable<HashWithLabel> {

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof HashWithLabel hwl && hwl.hash == hash;
        }


        @Override
        public int compareTo(@NotNull HashWithLabel o) {
            return Integer.compare(hash, o.hash);
        }

    }

    private record CaseLabels(Label start, Label afterTypeCheck, Label end) {

        public static CaseLabels create() {
            return new CaseLabels(new Label(), new Label(), new Label());
        }

    }

}
