package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@JEP(126)
public class J_L_I_LambdaMetafactory {

    public static final int FLAG_SERIALIZABLE = 1 << 0;

    public static final int FLAG_MARKERS = 1 << 1;

    public static final int FLAG_BRIDGES = 1 << 2;

    @Modify(ref = @Ref(value = "Ljava/lang/invoke/LambdaMetafactory;", member = "metafactory", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"))
    public static void makeLambdaInnerClass(MethodNode mnode, int i, ClassNode cnode, Set<ClassNode> extra, boolean noSynthetic) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) mnode.instructions.get(i);
        String ifName = indy.name;
        Type constructor = Type.getMethodType(indy.desc);
        Type bridge = (Type) indy.bsmArgs[0];
        Handle invokedMethod = (Handle) indy.bsmArgs[1];
        Type invokedType = (Type) indy.bsmArgs[2];
        int nextAnonymous = 1;
        // find next anonymous class number
        for (InnerClassNode icn : cnode.innerClasses) {
            if (icn.name.matches("^" + Pattern.quote(cnode.name) + "\\$\\d+$")) {
                int num = Integer.parseInt(icn.name.substring(icn.name.lastIndexOf('$') + 1));
                if (num >= nextAnonymous) {
                    nextAnonymous = num + 1;
                }
            }
        }
        String parentName = cnode.name;
        boolean needsBridge = !bridge.equals(invokedType);
        ClassNode child = lambdaMetafactoryCreatorInternal(parentName, constructor, nextAnonymous, mnode.name, mnode.desc, ifName, invokedMethod, invokedType, !needsBridge ? 0 : FLAG_BRIDGES, null, !needsBridge ? null : new Type[] { bridge });
        extra.add(child);

        makeLambdaInnerClassTail(mnode, indy, cnode, noSynthetic, nextAnonymous, invokedMethod, constructor, child);
    }

    @Modify(ref = @Ref(value = "Ljava/lang/invoke/LambdaMetafactory;", member = "altMetafactory", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void makeAltLambdaInnerClass(MethodNode mnode, int instructionIndex, ClassNode cnode, Set<ClassNode> extra, boolean noSynthetic) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) mnode.instructions.get(instructionIndex);
        String ifName = indy.name;
        Type constructor = Type.getMethodType(indy.desc);

        int argIndex = 0;
        Object[] args = indy.bsmArgs;

        // parse args
        Type interfaceMethodType = (Type) args[argIndex++];
        Handle invokedMethod = (Handle) args[argIndex++];
        Type invokedType = (Type) args[argIndex++];
        int flags = (int) args[argIndex++];

        Type[] altInterfaces = null;
        Type[] altMethods = null;

        if ((flags & FLAG_MARKERS) != 0) {
            int count = (int) args[argIndex++];
            if (count > 0) {
                altInterfaces = new Type[count];
                for (int i = 0; i < count; i++) {
                    altInterfaces[i] = (Type) args[argIndex++];
                }
            }
        }

        if ((flags & FLAG_BRIDGES) != 0) {
            int count = (int) args[argIndex++];
            if (count > 0) {
                altMethods = new Type[count];
                for (int i = 0; i < count; i++) {
                    altMethods[i] = (Type) args[argIndex++];
                }
            }
        }

        if (!interfaceMethodType.equals(invokedType)) {
            flags |= FLAG_BRIDGES;
            if (altMethods == null) {
                altMethods = new Type[] { interfaceMethodType };
            } else {
                Type[] oldAltMethods = altMethods;
                altMethods = new Type[oldAltMethods.length + 1];
                altMethods[0] = interfaceMethodType;
                System.arraycopy(oldAltMethods, 0, altMethods, 1, oldAltMethods.length);
            }
        }

        int nextAnonymous = 1;
        // find next anonymous class number
        for (InnerClassNode icn : cnode.innerClasses) {
            if (icn.name.matches("^" + Pattern.quote(cnode.name) + "\\$\\d+$")) {
                int num = Integer.parseInt(icn.name.substring(icn.name.lastIndexOf('$') + 1));
                if (num >= nextAnonymous) {
                    nextAnonymous = num + 1;
                }
            }
        }

        String parentName = cnode.name;
        ClassNode child = lambdaMetafactoryCreatorInternal(parentName, constructor, nextAnonymous, mnode.name, mnode.desc, ifName, invokedMethod, invokedType, flags, altInterfaces, altMethods);
        extra.add(child);

        makeLambdaInnerClassTail(mnode, indy, cnode, noSynthetic, nextAnonymous, invokedMethod, constructor, child);
    }

    private static void makeLambdaInnerClassTail(MethodNode mnode, InvokeDynamicInsnNode indy, ClassNode cnode, boolean noSynthetic, int nextAnonymous, Handle invokedMethod, Type constructor, ClassNode child) {
        // make invoked method at least package-private and not synthetic
        if (invokedMethod.getOwner().equals(cnode.name)) {
            for (MethodNode mn : cnode.methods) {
                if (mn.name.equals(invokedMethod.getName()) && mn.desc.equals(invokedMethod.getDesc())) {
                    mn.access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC);
                }
            }
        }
        // add child as inner class
        cnode.innerClasses.add(new InnerClassNode(child.name, null, null, Opcodes.ACC_STATIC));
        // create method for constructing child
        MethodVisitor mv = cnode.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | (noSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "jvmdowngrader$lambda$" + mnode.name.replace("<", "$").replace(">", "$") + "$" + nextAnonymous, constructor.getDescriptor(), null, null);
        mv.visitCode();
        mv.visitTypeInsn(Opcodes.NEW, child.name);
        mv.visitInsn(Opcodes.DUP);
        int j = 0;
        for (Type t : constructor.getArgumentTypes()) {
            mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
            j += t.getSize();
        }
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, child.name, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, constructor.getArgumentTypes()), false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // replace invokedynamic with method call
        mnode.instructions.set(indy, new MethodInsnNode(Opcodes.INVOKESTATIC, cnode.name, "jvmdowngrader$lambda$" + mnode.name.replace("<", "$").replace(">", "$") + "$" + nextAnonymous, constructor.getDescriptor(), (cnode.access & Opcodes.ACC_INTERFACE) != 0));
    }

    public static ClassNode lambdaMetafactoryCreatorInternal(String parentName, Type constructor, int nextAvailableAnonymous, String mname, String mdesc, String ifName, Handle invokedMethod, Type invokedType, int flags, Type[] altInterfaces, Type[] altMethods) {
        ClassNode cn = new ClassNode(Opcodes.ASM9);
        List<String> interfaces = new ArrayList<>();
        interfaces.add(constructor.getReturnType().getInternalName());

        if ((flags & FLAG_MARKERS) != 0) {
            for (Type altInterface : altInterfaces) {
                interfaces.add(altInterface.getInternalName());
            }
        }

        if ((flags & FLAG_SERIALIZABLE) != 0 && !interfaces.contains("java/io/Serializable")) {
            interfaces.add("java/io/Serializable");
        }

        // class name and metadata
        cn.visit(Opcodes.V1_7, Opcodes.ACC_SUPER | Opcodes.ACC_FINAL, parentName + "$" + nextAvailableAnonymous, null, "java/lang/Object", interfaces.toArray(new String[0]));
        cn.visitOuterClass(parentName, mname, mdesc);
        cn.visitInnerClass(cn.name, parentName, "$" + nextAvailableAnonymous, 0);

        // fields for constructor types
        Type[] constructorArgTypes = constructor.getArgumentTypes();
        for (int i = 0; i < constructorArgTypes.length; i++) {
            Type t = constructorArgTypes[i];
            FieldVisitor fv = cn.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "arg$$" + i, t.getDescriptor(), null, null);
            fv.visitEnd();
        }

        // create constructor writing fields
        MethodVisitor mv = cn.visitMethod(Opcodes.ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, constructorArgTypes), "()V", null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        for (int i = 0, j = 1; i < constructorArgTypes.length; i++) {
            Type t = constructorArgTypes[i];
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
            j += t.getSize();
            mv.visitFieldInsn(Opcodes.PUTFIELD, cn.name, "arg$$" + i, t.getDescriptor());
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // create interf method
        mv = cn.visitMethod(Opcodes.ACC_PUBLIC, ifName, invokedType.getDescriptor(), null, null);
        mv.visitCode();

        if (invokedMethod.getTag() == Opcodes.H_NEWINVOKESPECIAL) {
            // newinvokespecial is a constructor, so we need to create a new instance
            mv.visitTypeInsn(Opcodes.NEW, invokedMethod.getOwner());
            mv.visitInsn(Opcodes.DUP);
        }

        Type[] args;
        if (invokedMethod.getTag() == Opcodes.H_INVOKESTATIC) {
            args = Type.getArgumentTypes(invokedMethod.getDesc());
        } else {
            Type[] virtArgs = Type.getArgumentTypes(invokedMethod.getDesc());
            args = new Type[virtArgs.length + 1];
            args[0] = Type.getObjectType(invokedMethod.getOwner());
            System.arraycopy(virtArgs, 0, args, 1, virtArgs.length);
        }
        Type ret = Type.getReturnType(invokedMethod.getDesc());

        int n = 0;
        // load fields back into stack
        for (int i = 0; i < constructorArgTypes.length; i++) {
            Type t = constructorArgTypes[i];
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, cn.name, "arg$$" + i, t.getDescriptor());
            doCast(mv, args[n], t);
            n++;
        }
        // load rest of args
        Type[] invokedArgTypes = invokedType.getArgumentTypes();
        for (int i = 0, j = 1; i < invokedArgTypes.length; i++) {
            Type t = invokedArgTypes[i];
            mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
            doCast(mv, args[n], t);
            n++;
            j += t.getSize();
        }
        int opcode;
        switch (invokedMethod.getTag()) {
            case Opcodes.H_INVOKEINTERFACE:
                opcode = Opcodes.INVOKEINTERFACE;
                break;
            case Opcodes.H_INVOKESTATIC:
                opcode = Opcodes.INVOKESTATIC;
                break;
            case Opcodes.H_INVOKESPECIAL:
            case Opcodes.H_NEWINVOKESPECIAL:
                opcode = Opcodes.INVOKESPECIAL;
                break;
            case Opcodes.H_INVOKEVIRTUAL:
                opcode = Opcodes.INVOKEVIRTUAL;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported invokedMethod tag: " + invokedMethod.getTag());
        }
        // invoke original synthetic created for lambda
        mv.visitMethodInsn(opcode, invokedMethod.getOwner(), invokedMethod.getName(), invokedMethod.getDesc(), invokedMethod.isInterface());
        Type rtype = invokedType.getReturnType();
        doCast(mv, rtype, ret);
        mv.visitInsn(rtype.getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // visit bridges
        if ((flags & FLAG_BRIDGES) != 0) {
            for (Type bridge : altMethods) {
                mv = cn.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE, ifName, bridge.getDescriptor(), null, null);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                // load args
                Type[] bridgeArgTypes = bridge.getArgumentTypes();
                for (int i = 0, j = 1; i < bridgeArgTypes.length; i++) {
                    Type t = bridgeArgTypes[i];
                    mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
                    j += t.getSize();
                    doCast(mv, invokedArgTypes[i], t);
                }
                // invoke interf method
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, cn.name, ifName, invokedType.getDescriptor(), false);
                Type bridgeRtype = bridge.getReturnType();
                doCast(mv, bridgeRtype, rtype);
                mv.visitInsn(bridgeRtype.getOpcode(Opcodes.IRETURN));
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }

        // if not serializable, then add serialization hostile methods
        if ((flags & FLAG_SERIALIZABLE) == 0) {
            String[] exceptions = { "java/io/NotSerializableException" };

            mv = cn.visitMethod(Opcodes.ACC_PRIVATE, "readObject", "(Ljava/io/ObjectInputStream;)V", null, exceptions);
            mv.visitCode();
            generateSerializationHostileMethodCode(mv);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();

            mv = cn.visitMethod(Opcodes.ACC_PRIVATE, "writeObject", "(Ljava/io/ObjectInputStream;)V", null, exceptions);
            mv.visitCode();
            generateSerializationHostileMethodCode(mv);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        cn.visitEnd();
        return cn;
    }

    private static void doCast(MethodVisitor mv, Type expected, Type actual) {
        if (expected.equals(actual)) {
            return;
        }
        // is primitive
        if (expected.getSort() < 9) {
            if (actual.getSort() == Type.OBJECT) {
                switch (expected.getSort()) {
                    case Type.BOOLEAN:
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "booleanValue", "()Z", false);
                        break;
                    case Type.BYTE:
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "byteValue", "()B", false);
                        break;
                    case Type.CHAR:
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                        break;
                    case Type.SHORT:
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "shortValue", "()S", false);
                        break;
                    case Type.INT:
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
                        break;
                    case Type.LONG:
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
                        break;
                    case Type.FLOAT:
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
                        break;
                    case Type.DOUBLE:
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                        break;
                }
            }
        } else {
            // is object
            if (actual.getSort() < 9) {
                switch (expected.getInternalName()) {
                    case "java/lang/Boolean":
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                        break;
                    case "java/lang/Byte":
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                        break;
                    case "java/lang/Character":
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                        break;
                    case "java/lang/Short":
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                        break;
                    case "java/lang/Integer":
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                        break;
                    case "java/lang/Long":
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                        break;
                    case "java/lang/Float":
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                        break;
                    case "java/lang/Double":
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                        break;
                }
            } else {
                mv.visitTypeInsn(Opcodes.CHECKCAST, expected.getInternalName());
            }
        }
    }

    private static void generateSerializationHostileMethodCode(MethodVisitor mv) {
        mv.visitTypeInsn(Opcodes.NEW, "java/io/NotSerializableException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("Non-serializable lambda");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/NotSerializableException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);
    }


}
