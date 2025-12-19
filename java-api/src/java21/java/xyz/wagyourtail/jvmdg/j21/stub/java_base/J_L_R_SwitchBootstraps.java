package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.util.*;

public class J_L_R_SwitchBootstraps {

    private static final int HASH_THRESHOLD = 4;

    /**
     * Runtime helper to build the ordinal -> case index map.
     */
    public static int[] buildEnumMap(Class<?> enumClass, String[] labels) {
        Enum<?>[] constants = (Enum<?>[]) enumClass.getEnumConstants();
        int constantsLength = constants.length;
        int labelsLength = labels.length;
        int[] map = new int[constantsLength];
        Arrays.fill(map, labelsLength);

        for (int i = 0; i < constantsLength; i++) {
            Enum<?> c = constants[i];
            for (int j = 0; j < labelsLength; j++) {
                if (c.name().equals(labels[j])) {
                    map[i] = j;
                    break;
                }
            }
        }
        return map;
    }

    @Modify(ref = @Ref(value = "java/lang/runtime/SwitchBootstraps", member = "typeSwitch", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void makeTypeSwitch(MethodNode mnode, int i, ClassNode cnode, boolean noSynthetic) {
        processSwitch(mnode, i, cnode, noSynthetic, false);
    }

    @Modify(ref = @Ref(value = "java/lang/runtime/SwitchBootstraps", member = "enumSwitch", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void makeEnumSwitch(MethodNode mnode, int i, ClassNode cnode, boolean noSynthetic) {
        processSwitch(mnode, i, cnode, noSynthetic, true);
    }

    private static void processSwitch(MethodNode mnode, int i, ClassNode cnode, boolean noSynthetic, boolean isEnum) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) mnode.instructions.get(i);
        Type[] argTypes = Type.getArgumentTypes(indy.desc);
        Type enumType = isEnum ? argTypes[0] : null;

        List<Object> types = new ArrayList<>();
        for (Object bsmArg : indy.bsmArgs) {
            if (bsmArg instanceof ConstantDynamic condy) {
                String d = condy.getDescriptor();

                if (d.equals("Ljava/lang/Enum$EnumDesc;")) {
                    ConstantDynamic cls = (ConstantDynamic) condy.getBootstrapMethodArgument(1);
                    String clsName = (String) cls.getBootstrapMethodArgument(1);
                    String value = (String) condy.getBootstrapMethodArgument(2);
                    types.add(new EnumType(Type.getObjectType(clsName.replace('.', '/')), value));
                } else if (d.equals("Ljava/lang/Boolean;")) {
                    if (condy.getName().equals("TRUE")) {
                        types.add(Boolean.TRUE);
                    } else if (condy.getName().equals("FALSE")) {
                        types.add(Boolean.FALSE);
                    } else throw new IllegalStateException("Unknown condy type for switch: " + condy);
                } else throw new IllegalStateException("Unknown condy type for switch: " + condy);
            } else if (isEnum && bsmArg instanceof String value) {
                types.add(new EnumType(enumType, value));
            } else {
                types.add(bsmArg);
            }
        }

        MethodInsnNode min = makeSwitchInternal(mnode.name, indy.desc, types, cnode, noSynthetic, isEnum);
        mnode.instructions.set(indy, min);
    }

    public static MethodInsnNode makeSwitchInternal(String holdingMethod, String desc, List<Object> types, ClassNode cnode, boolean noSynthetic, boolean isEnumSwitch) {
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

        final int target = 0;
        final int restart = 1;
        final int tag = 2; // 0:none, 1:int/char, 2:string, 3:enum, 4:boolean
        final int intVal = 3;
        final int objVal = 4;

        String mapFieldName = null;
        String initFieldName = null;
        boolean useMappedCache = false;
        Type mappedEnumType = null;

        if (isEnumSwitch && !types.isEmpty()) {
            boolean allEnums = true;
            Type firstType = null;
            for (Object t : types) {
                if (t instanceof EnumType et) {
                    if (firstType == null) firstType = et.type;
                } else {
                    allEnums = false;
                    break;
                }
            }
            if (allEnums && firstType != null) {
                useMappedCache = true;
                mappedEnumType = firstType;
                mapFieldName = name + "$map";
                initFieldName = name + "$init";

                cnode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE | Opcodes.ACC_SYNTHETIC, mapFieldName, "[I", null, null));
                cnode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE | Opcodes.ACC_SYNTHETIC, initFieldName, "Z", null, null));
            }
        }

        // inline checkIndex(restart, types.size())
        Label lError = new Label();
        Label lNullCheck = new Label();
        smnode.visitVarInsn(Opcodes.ILOAD, restart);
        smnode.visitJumpInsn(Opcodes.IFLT, lError);
        smnode.visitVarInsn(Opcodes.ILOAD, restart);
        smnode.visitLdcInsn(types.size());
        smnode.visitJumpInsn(Opcodes.IF_ICMPGT, lError);
        smnode.visitJumpInsn(Opcodes.GOTO, lNullCheck);

        smnode.visitLabel(lError);
        smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        smnode.visitTypeInsn(Opcodes.NEW, "java/lang/IndexOutOfBoundsException");
        smnode.visitInsn(Opcodes.DUP);
        smnode.visitVarInsn(Opcodes.ILOAD, restart);
        smnode.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);
        smnode.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IndexOutOfBoundsException", "<init>", "(Ljava/lang/String;)V", false);
        smnode.visitInsn(Opcodes.ATHROW);

        smnode.visitLabel(lNullCheck);
        smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        // null -> -1
        Label lValid = new Label();
        smnode.visitVarInsn(Opcodes.ALOAD, target);
        smnode.visitJumpInsn(Opcodes.IFNONNULL, lValid);
        smnode.visitInsn(Opcodes.ICONST_M1);
        smnode.visitInsn(Opcodes.IRETURN);

        smnode.visitLabel(lValid);
        smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        // fast path for pure enum switches: ordinal -> mapped case index
        if (useMappedCache) {
            Label lSkipCache = new Label();
            smnode.visitVarInsn(Opcodes.ILOAD, restart);
            smnode.visitJumpInsn(Opcodes.IFNE, lSkipCache);

            smnode.visitFieldInsn(Opcodes.GETSTATIC, cnode.name, initFieldName, "Z");
            Label lLoadCache = new Label();
            smnode.visitJumpInsn(Opcodes.IFNE, lLoadCache);

            // init cache
            smnode.visitLdcInsn(mappedEnumType);
            smnode.visitLdcInsn(types.size());
            smnode.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
            for (int j = 0; j < types.size(); j++) {
                smnode.visitInsn(Opcodes.DUP);
                smnode.visitLdcInsn(j);
                smnode.visitLdcInsn(((EnumType) types.get(j)).value);
                smnode.visitInsn(Opcodes.AASTORE);
            }
            smnode.visitMethodInsn(Opcodes.INVOKESTATIC, "xyz/wagyourtail/jvmdg/j21/stub/java_base/J_L_R_SwitchBootstraps", "buildEnumMap", "(Ljava/lang/Class;[Ljava/lang/String;)[I", false);
            smnode.visitFieldInsn(Opcodes.PUTSTATIC, cnode.name, mapFieldName, "[I");
            smnode.visitInsn(Opcodes.ICONST_1);
            smnode.visitFieldInsn(Opcodes.PUTSTATIC, cnode.name, initFieldName, "Z");

            smnode.visitLabel(lLoadCache);
            smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            smnode.visitFieldInsn(Opcodes.GETSTATIC, cnode.name, mapFieldName, "[I");
            smnode.visitVarInsn(Opcodes.ALOAD, target);
            smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Enum", "ordinal", "()I", false);
            smnode.visitInsn(Opcodes.IALOAD);
            smnode.visitInsn(Opcodes.IRETURN);

            smnode.visitLabel(lSkipCache);
            smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        // precompute tag/intVal/objVal once
        smnode.visitInsn(Opcodes.ICONST_0);
        smnode.visitVarInsn(Opcodes.ISTORE, tag);
        smnode.visitInsn(Opcodes.ICONST_0);
        smnode.visitVarInsn(Opcodes.ISTORE, intVal);
        smnode.visitInsn(Opcodes.ACONST_NULL);
        smnode.visitVarInsn(Opcodes.ASTORE, objVal);

        boolean hasInt = false;
        boolean hasString = false;
        boolean hasEnum = false;
        boolean hasBool = false;

        for (Object t : types) {
            if (t instanceof Integer) hasInt = true;
            else if (t instanceof String) hasString = true;
            else if (t instanceof EnumType) hasEnum = true;
            else if (t instanceof Boolean) hasBool = true;
        }

        Label lSwitchStart = new Label();
        boolean localsInit = false;

        if (hasInt) {
            smnode.visitVarInsn(Opcodes.ALOAD, target);
            smnode.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/Number");
            Label next1 = new Label();
            smnode.visitJumpInsn(Opcodes.IFEQ, next1);
            smnode.visitInsn(Opcodes.ICONST_1);
            smnode.visitVarInsn(Opcodes.ISTORE, tag);
            smnode.visitVarInsn(Opcodes.ALOAD, target);
            smnode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
            smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
            smnode.visitVarInsn(Opcodes.ISTORE, intVal);
            smnode.visitJumpInsn(Opcodes.GOTO, lSwitchStart);

            smnode.visitLabel(next1);
            smnode.visitFrame(Opcodes.F_APPEND, 3, new Object[]{Opcodes.INTEGER, Opcodes.INTEGER, "java/lang/Object"}, 0, null);
            localsInit = true;

            smnode.visitVarInsn(Opcodes.ALOAD, target);
            smnode.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/Character");
            Label next2 = new Label();
            smnode.visitJumpInsn(Opcodes.IFEQ, next2);
            smnode.visitInsn(Opcodes.ICONST_1);
            smnode.visitVarInsn(Opcodes.ISTORE, tag);
            smnode.visitVarInsn(Opcodes.ALOAD, target);
            smnode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
            smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
            smnode.visitVarInsn(Opcodes.ISTORE, intVal);
            smnode.visitJumpInsn(Opcodes.GOTO, lSwitchStart);

            smnode.visitLabel(next2);
            smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        if (hasString) {
            smnode.visitVarInsn(Opcodes.ALOAD, target);
            smnode.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/String");
            Label next = new Label();
            smnode.visitJumpInsn(Opcodes.IFEQ, next);
            smnode.visitInsn(Opcodes.ICONST_2);
            smnode.visitVarInsn(Opcodes.ISTORE, tag);
            smnode.visitVarInsn(Opcodes.ALOAD, target);
            smnode.visitVarInsn(Opcodes.ASTORE, objVal);
            smnode.visitJumpInsn(Opcodes.GOTO, lSwitchStart);

            smnode.visitLabel(next);
            if (!localsInit) {
                smnode.visitFrame(Opcodes.F_APPEND, 3, new Object[]{Opcodes.INTEGER, Opcodes.INTEGER, "java/lang/Object"}, 0, null);
                localsInit = true;
            } else {
                smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
        }

        if (hasEnum) {
            smnode.visitVarInsn(Opcodes.ALOAD, target);
            smnode.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/Enum");
            Label next = new Label();
            smnode.visitJumpInsn(Opcodes.IFEQ, next);
            smnode.visitInsn(Opcodes.ICONST_3);
            smnode.visitVarInsn(Opcodes.ISTORE, tag);
            smnode.visitVarInsn(Opcodes.ALOAD, target);
            smnode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Enum");
            smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Enum", "name", "()Ljava/lang/String;", false);
            smnode.visitVarInsn(Opcodes.ASTORE, objVal);
            smnode.visitJumpInsn(Opcodes.GOTO, lSwitchStart);

            smnode.visitLabel(next);
            if (!localsInit) {
                smnode.visitFrame(Opcodes.F_APPEND, 3, new Object[]{Opcodes.INTEGER, Opcodes.INTEGER, "java/lang/Object"}, 0, null);
                localsInit = true;
            } else {
                smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
        }

        if (hasBool) {
            smnode.visitVarInsn(Opcodes.ALOAD, target);
            smnode.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/Boolean");
            Label next = new Label();
            smnode.visitJumpInsn(Opcodes.IFEQ, next);
            smnode.visitInsn(Opcodes.ICONST_4);
            smnode.visitVarInsn(Opcodes.ISTORE, tag);
            smnode.visitVarInsn(Opcodes.ALOAD, target);
            smnode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
            smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);

            Label isTrue = new Label();
            Label endBool = new Label();
            smnode.visitJumpInsn(Opcodes.IFNE, isTrue);

            smnode.visitInsn(Opcodes.ICONST_0);
            smnode.visitVarInsn(Opcodes.ISTORE, intVal);
            smnode.visitJumpInsn(Opcodes.GOTO, endBool);

            smnode.visitLabel(isTrue);
            if (!localsInit) {
                smnode.visitFrame(Opcodes.F_APPEND, 3, new Object[]{Opcodes.INTEGER, Opcodes.INTEGER, "java/lang/Object"}, 0, null);
                localsInit = true;
            } else {
                smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            smnode.visitInsn(Opcodes.ICONST_1);
            smnode.visitVarInsn(Opcodes.ISTORE, intVal);

            smnode.visitLabel(endBool);
            smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            smnode.visitJumpInsn(Opcodes.GOTO, lSwitchStart);

            smnode.visitLabel(next);
            smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        smnode.visitLabel(lSwitchStart);
        if (!localsInit) {
            smnode.visitFrame(Opcodes.F_APPEND, 3, new Object[]{Opcodes.INTEGER, Opcodes.INTEGER, "java/lang/Object"}, 0, null);
        } else {
            smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        List<Run> runs = new ArrayList<>();
        for (int j = 0; j < types.size(); j++) {
            Object t = types.get(j);
            Run current = runs.isEmpty() ? null : runs.get(runs.size() - 1);
            if (current != null && current.isCompatible(t)) {
                current.add(j, t);
            } else {
                runs.add(new Run(j, t));
            }
        }

        Label[] entryLabels = new Label[types.size()];
        for (Run r : runs) {
            r.label = new Label();
            for (int idx : r.indices) {
                entryLabels[idx] = r.label;
            }
        }
        Label defaultLabel = new Label();

        if (!types.isEmpty()) {
            smnode.visitVarInsn(Opcodes.ILOAD, restart);
            smnode.visitTableSwitchInsn(0, types.size() - 1, defaultLabel, entryLabels);
        } else {
            smnode.visitJumpInsn(Opcodes.GOTO, defaultLabel);
        }

        Type selectorType = Type.getArgumentTypes(desc)[0];
        for (int rIdx = 0; rIdx < runs.size(); rIdx++) {
            Run run = runs.get(rIdx);
            Label nextRunLabel = (rIdx < runs.size() - 1) ? runs.get(rIdx + 1).label : defaultLabel;

            smnode.visitLabel(run.label);
            smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

            if (run.typeCategory == 1) {
                smnode.visitVarInsn(Opcodes.ILOAD, tag);
                smnode.visitInsn(Opcodes.ICONST_1);
                smnode.visitJumpInsn(Opcodes.IF_ICMPNE, nextRunLabel);
                emitIntegerRun(smnode, run, intVal, restart, nextRunLabel);

            } else if (run.typeCategory == 2) {
                smnode.visitVarInsn(Opcodes.ILOAD, tag);
                smnode.visitInsn(Opcodes.ICONST_2);
                smnode.visitJumpInsn(Opcodes.IF_ICMPNE, nextRunLabel);

                if (run.indices.size() >= HASH_THRESHOLD) {
                    emitStringHashRun(smnode, run, objVal, restart, nextRunLabel);
                } else {
                    emitStringLinearRun(smnode, run, objVal, restart, nextRunLabel);
                }

            } else if (run.typeCategory == 3) {
                smnode.visitVarInsn(Opcodes.ILOAD, tag);
                smnode.visitInsn(Opcodes.ICONST_3);
                smnode.visitJumpInsn(Opcodes.IF_ICMPNE, nextRunLabel);

                smnode.visitVarInsn(Opcodes.ALOAD, target);
                smnode.visitTypeInsn(Opcodes.INSTANCEOF, run.enumType.getInternalName());
                smnode.visitJumpInsn(Opcodes.IFEQ, nextRunLabel);

                if (run.indices.size() >= HASH_THRESHOLD) {
                    emitStringHashRun(smnode, run, objVal, restart, nextRunLabel);
                } else {
                    emitStringLinearRun(smnode, run, objVal, restart, nextRunLabel);
                }

            } else if (run.typeCategory == 4) {
                smnode.visitVarInsn(Opcodes.ILOAD, tag);
                smnode.visitInsn(Opcodes.ICONST_4);
                smnode.visitJumpInsn(Opcodes.IF_ICMPNE, nextRunLabel);

                for (int k = 0; k < run.indices.size(); k++) {
                    int idx = run.indices.get(k);
                    Boolean b = (Boolean) run.values.get(k);
                    smnode.visitVarInsn(Opcodes.ILOAD, intVal);
                    smnode.visitInsn(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
                    Label nextItem = new Label();
                    smnode.visitJumpInsn(Opcodes.IF_ICMPNE, nextItem);
                    checkRestartAndReturn(smnode, restart, idx, nextItem);
                    smnode.visitLabel(nextItem);
                    smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                }
                smnode.visitJumpInsn(Opcodes.GOTO, nextRunLabel);

            } else {
                for (int k = 0; k < run.indices.size(); k++) {
                    int idx = run.indices.get(k);
                    Object type = run.values.get(k);
                    Label nextItem = new Label();

                    if (type instanceof Type t) {
                        if (!t.equals(selectorType)) {
                            smnode.visitVarInsn(Opcodes.ALOAD, target);
                            smnode.visitTypeInsn(Opcodes.INSTANCEOF, t.getInternalName());
                            smnode.visitJumpInsn(Opcodes.IFEQ, nextItem);
                        }
                    } else {
                        emitBoxedConst(smnode, type);
                        smnode.visitVarInsn(Opcodes.ALOAD, target);
                        smnode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false);
                        smnode.visitJumpInsn(Opcodes.IFEQ, nextItem);
                    }
                    checkRestartAndReturn(smnode, restart, idx, nextItem);
                    smnode.visitLabel(nextItem);
                    smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                }
                smnode.visitJumpInsn(Opcodes.GOTO, nextRunLabel);
            }
        }

        smnode.visitLabel(defaultLabel);
        smnode.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        smnode.visitLdcInsn(types.size());
        smnode.visitInsn(Opcodes.IRETURN);

        smnode.visitMaxs(0, 0);
        smnode.visitEnd();
        cnode.methods.add(smnode);
        return new MethodInsnNode(Opcodes.INVOKESTATIC, cnode.name, name, desc, (cnode.access & Opcodes.ACC_INTERFACE) != 0);
    }

    private static void checkRestartAndReturn(MethodNode mn, int varRestart, int idx, Label skip) {
        mn.visitVarInsn(Opcodes.ILOAD, varRestart);
        mn.visitLdcInsn(idx);
        mn.visitJumpInsn(Opcodes.IF_ICMPGT, skip);
        mn.visitLdcInsn(idx);
        mn.visitInsn(Opcodes.IRETURN);
    }

    private static void emitBoxedConst(MethodNode mn, Object c) {
        if (c instanceof Float f) {
            mn.visitLdcInsn(f.floatValue());
            mn.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
        } else if (c instanceof Double d) {
            mn.visitLdcInsn(d.doubleValue());
            mn.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        } else if (c instanceof Long l) {
            mn.visitLdcInsn(l.longValue());
            mn.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
        } else if (c instanceof Boolean b) {
            mn.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", b ? "TRUE" : "FALSE", "Ljava/lang/Boolean;");
        } else {
            mn.visitLdcInsn(c);
        }
    }

    private static void emitIntegerRun(MethodNode mn, Run run, int varIntVal, int varRestart, Label nextRunLabel) {
        Map<Integer, List<Integer>> valueToIndices = new TreeMap<>();
        for (int k = 0; k < run.indices.size(); k++) {
            valueToIndices.computeIfAbsent((Integer) run.values.get(k), _k -> new ArrayList<>()).add(run.indices.get(k));
        }

        int[] keys = valueToIndices.keySet().stream().mapToInt(v -> v).toArray();
        Label[] targets = new Label[keys.length];
        for (int j = 0; j < keys.length; j++) targets[j] = new Label();

        mn.visitVarInsn(Opcodes.ILOAD, varIntVal);
        int min = keys[0];
        int max = keys[keys.length - 1];

        if (max - min < keys.length * 2 + 10) {
            Label[] tableTargets = new Label[max - min + 1];
            Arrays.fill(tableTargets, nextRunLabel);
            for (int j = 0; j < keys.length; j++) {
                tableTargets[keys[j] - min] = targets[j];
            }
            mn.visitTableSwitchInsn(min, max, nextRunLabel, tableTargets);
        } else {
            mn.visitLookupSwitchInsn(nextRunLabel, keys, targets);
        }

        for (int j = 0; j < keys.length; j++) {
            mn.visitLabel(targets[j]);
            mn.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            List<Integer> indices = valueToIndices.get(keys[j]);
            for (int idx : indices) {
                Label skip = new Label();
                checkRestartAndReturn(mn, varRestart, idx, skip);
                mn.visitLabel(skip);
                mn.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            mn.visitJumpInsn(Opcodes.GOTO, nextRunLabel);
        }
    }

    private static void emitStringLinearRun(MethodNode mn, Run run, int varObjVal, int varRestart, Label nextRunLabel) {
        for (int k = 0; k < run.indices.size(); k++) {
            int idx = run.indices.get(k);
            Object val = run.values.get(k);
            String sVal = (val instanceof EnumType et) ? et.value : (String) val;

            mn.visitLdcInsn(sVal);
            mn.visitVarInsn(Opcodes.ALOAD, varObjVal);
            mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);

            Label nextItem = new Label();
            mn.visitJumpInsn(Opcodes.IFEQ, nextItem);
            checkRestartAndReturn(mn, varRestart, idx, nextItem);
            mn.visitLabel(nextItem);
            mn.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }
        mn.visitJumpInsn(Opcodes.GOTO, nextRunLabel);
    }

    private static void emitStringHashRun(MethodNode mn, Run run, int varObjVal, int varRestart, Label nextRunLabel) {
        // Group indices by String value, then by hashCode
        Map<Integer, Map<String, List<Integer>>> hashBuckets = new TreeMap<>();

        for (int k = 0; k < run.indices.size(); k++) {
            Object val = run.values.get(k);
            String sVal = (val instanceof EnumType et) ? et.value : (String) val;
            int hash = sVal.hashCode();

            hashBuckets.computeIfAbsent(hash, h -> new LinkedHashMap<>()).computeIfAbsent(sVal, s -> new ArrayList<>()).add(run.indices.get(k));
        }

        int[] keys = hashBuckets.keySet().stream().mapToInt(Integer::intValue).toArray();
        Label[] targets = new Label[keys.length];
        Arrays.setAll(targets, i -> new Label());

        mn.visitVarInsn(Opcodes.ALOAD, varObjVal);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false);
        mn.visitLookupSwitchInsn(nextRunLabel, keys, targets);

        for (int i = 0; i < keys.length; i++) {
            mn.visitLabel(targets[i]);
            mn.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

            Map<String, List<Integer>> bucket = hashBuckets.get(keys[i]);
            for (Map.Entry<String, List<Integer>> entry : bucket.entrySet()) {
                String sVal = entry.getKey();
                List<Integer> indices = entry.getValue();

                mn.visitLdcInsn(sVal);
                mn.visitVarInsn(Opcodes.ALOAD, varObjVal);
                mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);

                Label nextStr = new Label();
                mn.visitJumpInsn(Opcodes.IFEQ, nextStr);
                for (int idx : indices) {
                    Label skip = new Label();
                    checkRestartAndReturn(mn, varRestart, idx, skip);
                    mn.visitLabel(skip);
                    mn.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                }
                mn.visitJumpInsn(Opcodes.GOTO, nextRunLabel);

                mn.visitLabel(nextStr);
                mn.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            mn.visitJumpInsn(Opcodes.GOTO, nextRunLabel);
        }
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

    private static class Run {
        int typeCategory;
        Type enumType;
        Label label;
        List<Integer> indices = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        public Run(int idx, Object firstVal) {
            indices.add(idx);
            values.add(firstVal);
            if (firstVal instanceof Integer) {
                typeCategory = 1;
            } else if (firstVal instanceof String) {
                typeCategory = 2;
            } else if (firstVal instanceof EnumType et) {
                typeCategory = 3;
                enumType = et.type;
            } else if (firstVal instanceof Boolean) {
                typeCategory = 4;
            } else {
                typeCategory = 0;
            }
        }

        public void add(int idx, Object val) {
            indices.add(idx);
            values.add(val);
        }

        public boolean isCompatible(Object val) {
            if (typeCategory == 0) return false;
            if (typeCategory == 1 && val instanceof Integer) return true;
            if (typeCategory == 2 && val instanceof String) return true;
            if (typeCategory == 3 && val instanceof EnumType et && et.type.equals(enumType)) return true;
            return typeCategory == 4 && val instanceof Boolean;
        }

    }

}
