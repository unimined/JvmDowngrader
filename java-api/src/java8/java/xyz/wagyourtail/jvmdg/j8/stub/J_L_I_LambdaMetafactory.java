package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;
import java.util.regex.Pattern;

public class J_L_I_LambdaMetafactory {

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
        ClassNode child = lambdaMetafactoryCreatorInternal(parentName, constructor, nextAnonymous, mnode.name, mnode.desc, ifName, bridge, invokedMethod, invokedType);
        extra.add(child);
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

    @Stub(ref = @Ref(value = "Ljava/lang/invoke/LambdaMetafactory;", member = "altMetafactory", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static CallSite altMetafactory(MethodHandles.Lookup caller, String invokedName, MethodType invokedType, Object... args) {
        throw new MissingStubError("altMetafactory is not supported currently... because I don't know enough about how it works.");
    }

    public static ClassNode lambdaMetafactoryCreatorInternal(String parentName, Type constructor, int nextAvailableAnonymous, String mname, String mdesc, String ifName, Type bridge, Handle invokedMethod, Type invokedType) {
        ClassNode cn = new ClassNode(Opcodes.ASM9);
        // class name and metadata
        cn.visit(Opcodes.V1_7, Opcodes.ACC_SUPER, parentName + "$" + nextAvailableAnonymous, null, "java/lang/Object", new String[]{constructor.getReturnType().getInternalName()});
        cn.visitOuterClass(parentName, mname, mdesc);
        cn.visitInnerClass(cn.name, parentName, "$" + nextAvailableAnonymous, 0);

        // fields for constructor types
        for (int i = 0; i < constructor.getArgumentTypes().length; i++) {
            Type t = constructor.getArgumentTypes()[i];
            FieldVisitor fv = cn.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "arg$$" + i, t.getDescriptor(), null, null);
            fv.visitEnd();
        }

        // create constructor writing fields
        MethodVisitor mv = cn.visitMethod(Opcodes.ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, constructor.getArgumentTypes()), "()V", null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        for (int i = 0, j = 1; i < constructor.getArgumentTypes().length; i++) {
            Type t = constructor.getArgumentTypes()[i];
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
        for (int i = 0; i < constructor.getArgumentTypes().length; i++) {
            Type t = constructor.getArgumentTypes()[i];
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, cn.name, "arg$$" + i, t.getDescriptor());
            doCast(mv, args[n], t);
            n++;
        }
        // load rest of args
        for (int i = 0, j = 1; i < invokedType.getArgumentTypes().length; i++) {
            Type t = invokedType.getArgumentTypes()[i];
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
        doCast(mv, invokedType.getReturnType(), ret);
        mv.visitInsn(invokedType.getReturnType().getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // visit bridge
        if (!bridge.equals(invokedType)) {
            mv = cn.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE, ifName, bridge.getDescriptor(), null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            // load args
            for (int i = 0, j = 1; i < bridge.getArgumentTypes().length; i++) {
                Type t = bridge.getArgumentTypes()[i];
                mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
                j += t.getSize();
                doCast(mv, invokedType.getArgumentTypes()[i], t);
            }
            // invoke interf method
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, cn.name, ifName, invokedType.getDescriptor(), false);
            doCast(mv, bridge.getReturnType(), invokedType.getReturnType());
            mv.visitInsn(bridge.getReturnType().getOpcode(Opcodes.IRETURN));
            mv.visitMaxs(0, 0);
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


}
