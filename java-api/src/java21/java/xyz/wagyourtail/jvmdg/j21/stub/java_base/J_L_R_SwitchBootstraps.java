package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class J_L_R_SwitchBootstraps {

    @Modify(javaVersion = Opcodes.V21, ref = @Ref(value = "java/lang/runtime/SwitchBootstraps", member = "typeSwitch", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void makeTypeSwitch(MethodNode mnode, int i, ClassNode cnode) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) mnode.instructions.get(i);
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
        MethodInsnNode min = makeSwitchInternal(indy.desc, types, cnode);
        mnode.instructions.set(indy, min);
    }

    @Modify(javaVersion = Opcodes.V21, ref = @Ref(value = "java/lang/runtime/SwitchBootstraps", member = "enumSwitch", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void makeEnumSwitch(MethodNode mnode, int i, ClassNode cnode) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) mnode.instructions.get(i);
        Type[] argTypes = Type.getArgumentTypes(indy.desc);
        List<Object> types = new ArrayList<>();
        for (Object bsmArg : indy.bsmArgs) {
            if (bsmArg instanceof String value) {
                types.add(new EnumType(argTypes[0], value));
            } else {
                types.add(bsmArg);
            }
        }
        MethodInsnNode min = makeSwitchInternal(indy.desc, types, cnode);
        mnode.instructions.set(indy, min);
    }

    public static MethodInsnNode makeSwitchInternal(String desc, List<Object> types, ClassNode cnode) {
        int i = 0;
        for (MethodNode mnode : cnode.methods) {
            if (mnode instanceof SwitchMethodNode smnode) {
                if (smnode.types.equals(types)) {
                    return new MethodInsnNode(Opcodes.INVOKESTATIC, cnode.name, mnode.name, mnode.desc, false);
                }
                i++;
            }
        }

        String name = "jvmdg$switch$" + i;
        SwitchMethodNode smnode = new SwitchMethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, name, desc, types);
        smnode.visitCode();

        // private static <name>(Object value, int start) {
        //     Objects.checkIndex(start, types.size());
        //     if (value == null) {
        //         return -1;
        //     }
        //     switch(start) {
        //         case 0:

        //     }
        // }
        smnode.visitVarInsn(Opcodes.ILOAD, 1);
        smnode.visitLdcInsn(types.size());
        // stack: start, types.size()
        smnode.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "checkIndex", "(II)I", false);
        smnode.visitInsn(Opcodes.POP);
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

        List<CaseLabels> labels = new ArrayList<>();
        List<Label> startLabels = new ArrayList<>();
        for (int j = 0; j < types.size(); j++) {
            CaseLabels caseLabels = CaseLabels.create();
            labels.add(caseLabels);
            startLabels.add(caseLabels.start);
        }
        Label def = new Label();

        smnode.visitVarInsn(Opcodes.ILOAD, 1);
        smnode.visitTableSwitchInsn(0, labels.size() - 1, def, startLabels.toArray(new Label[0]));

        for (int j = 0; j < types.size(); j++) {
            var caseLabels = labels.get(j);
            smnode.visitLabel(caseLabels.start);
            var type = types.get(j);
            if (type instanceof EnumType et) {
                // lookahead if multiple EnumTypes in a row
                int k = j + 1;
                while (k < types.size() && types.get(k) instanceof EnumType ket && ket.type.equals(et.type)) {
                    k++;
                }
                k--;
                if (k != j) {
                    var lastCaseLabels = labels.get(k);
                    // check if is of type
                    smnode.visitVarInsn(Opcodes.ALOAD, 0);
                    smnode.visitTypeInsn(Opcodes.INSTANCEOF, et.type.getInternalName());
                    smnode.visitJumpInsn(Opcodes.IFEQ, lastCaseLabels.end);
                    // cast
                    smnode.visitVarInsn(Opcodes.ALOAD, 0);
                    smnode.visitTypeInsn(Opcodes.CHECKCAST, et.type.getInternalName());
                    // get name
                    smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, et.type.getInternalName(), "name", "()Ljava/lang/String;", false);
                    // hashCode
                    smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);

                    int[] hashCodes = new int[k - j + 1];
                    Label[] tableLabels = new Label[k - j + 1];
                    for (int l = j; l <= k; l++) {
                        hashCodes[l - j] = ((EnumType) types.get(l)).value.hashCode();
                        tableLabels[l - j] = labels.get(l).afterTypeCheck;
                    }

                    smnode.visitLookupSwitchInsn(lastCaseLabels.end, hashCodes, tableLabels);
                }
                // load enum value
                smnode.visitLabel(caseLabels.afterTypeCheck);
                smnode.visitFieldInsn(Opcodes.GETSTATIC, et.type.getInternalName(), et.value, et.type.getDescriptor());
                smnode.visitVarInsn(Opcodes.ALOAD, 0);
                // equals
                smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false);
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
                    // check if is of type
                    smnode.visitVarInsn(Opcodes.ALOAD, 0);
                    smnode.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/String");
                    smnode.visitJumpInsn(Opcodes.IFEQ, lastCaseLabels.end);
                    // hashCode
                    smnode.visitVarInsn(Opcodes.ALOAD, 0);
                    smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false);

                    int[] hashCodes = new int[k - j + 1];
                    Label[] tableLabels = new Label[k - j + 1];
                    for (int l = j; l <= k; l++) {
                        hashCodes[l - j] = ((String) types.get(l)).hashCode();
                        tableLabels[l - j] = labels.get(l).afterTypeCheck;
                    }

                    smnode.visitLookupSwitchInsn(lastCaseLabels.end, hashCodes, tableLabels);
                }
                smnode.visitLabel(caseLabels.afterTypeCheck);
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

                    int[] hashCodes = new int[k - j + 1];
                    Label[] tableLabels = new Label[k - j + 1];
                    for (int l = j; l <= k; l++) {
                        hashCodes[l - j] = ((Integer) types.get(l)).hashCode();
                        tableLabels[l - j] = labels.get(l).afterTypeCheck;
                    }

                    smnode.visitLookupSwitchInsn(lastCaseLabels.end, hashCodes, tableLabels);
                }

                smnode.visitLabel(caseLabels.afterTypeCheck);
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
                // if (value instanceof type) {
                //     return j;
                // }
                smnode.visitVarInsn(Opcodes.ALOAD, 0);
                smnode.visitTypeInsn(Opcodes.INSTANCEOF, ((Type) type).getInternalName());
                smnode.visitJumpInsn(Opcodes.IFEQ, caseLabels.end);
                smnode.visitLdcInsn(j);
                smnode.visitJumpInsn(Opcodes.GOTO, retLabel);
            } else {
                throw new IllegalStateException("Unknown type for switch: " + type);
            }
            smnode.visitLabel(caseLabels.end);
        }
        smnode.visitLabel(def);
        smnode.visitLdcInsn(types.size());
        smnode.visitLabel(retLabel);
        smnode.visitInsn(Opcodes.IRETURN);


        smnode.visitMaxs(0, 0);
        smnode.visitEnd();
        cnode.methods.add(smnode);
        return new MethodInsnNode(Opcodes.INVOKESTATIC, cnode.name, name, desc, false);
    }


    private record EnumType(Type type, String value) {}

    private static class SwitchMethodNode extends MethodNode {
        private final List<Object> types;

        public SwitchMethodNode(int access, String name, String descriptor, List<Object> types) {
            super(Opcodes.ASM9, access, name, descriptor, null, null);
            this.types = types;
        }
    }

    private record CaseLabels(Label start, Label afterTypeCheck, Label end) {

        public static CaseLabels create() {
            return new CaseLabels(new Label(), new Label(), new Label());
        }

    }

}
