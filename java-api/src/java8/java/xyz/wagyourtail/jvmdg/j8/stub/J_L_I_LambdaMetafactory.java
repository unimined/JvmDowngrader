package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;

public class J_L_I_LambdaMetafactory {

    @Modify(javaVersion = Opcodes.V1_8, ref = @Ref(value = "Ljava/lang/invoke/LambdaMetafactory;", member = "metafactory", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"))
    public static void makeLambdaInnerClass(MethodNode mnode, int i, ClassNode cnode, Set<ClassNode> extra) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) mnode.instructions.get(i);
        String ifName = indy.name;
        Type constructor = Type.getMethodType(indy.desc);
        Type bridge = (Type) indy.bsmArgs[0];
        Handle invokedMethod = (Handle) indy.bsmArgs[1];
        Type invokedType = (Type) indy.bsmArgs[2];
        int nextAnonymous = 1;
        // find next anonymous class number
        for (InnerClassNode icn : cnode.innerClasses) {
            if (icn.name.matches("^" + cnode.name + "\\$\\d+$")) {
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
        cnode.innerClasses.add(new InnerClassNode(child.name, null, null, 0));
        // construct new class
        InsnList il = new InsnList();
        il.add(new TypeInsnNode(Opcodes.NEW, child.name));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, child.name, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, constructor.getArgumentTypes()), false));
        mnode.instructions.insertBefore(indy, il);
        mnode.instructions.remove(indy);
    }

    @Stub(ref = @Ref(value = "Ljava/lang/invoke/LambdaMetafactory;", member = "altMetafactory", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static CallSite altMetafactory(MethodHandles.Lookup caller, String invokedName, MethodType invokedType, Object... args) {
        throw new UnsupportedOperationException("altMetafactory is not supported currently... because I don't know enough about how it works.");
    }

    public static ClassNode lambdaMetafactoryCreatorInternal(String parentName, Type constructor, int nextAvailableAnonymous, String mname, String mdesc, String ifName, Type bridge, Handle invokedMethod, Type invokedType) {
        ClassNode cn = new ClassNode(Opcodes.ASM9);
        // class name and metadata
        cn.visit(Opcodes.V1_7, Opcodes.ACC_SUPER, parentName + "$" + nextAvailableAnonymous, null, "java/lang/Object", new String[] { constructor.getReturnType().getInternalName() });
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
        for (int i = 0, j = 1; i < constructor.getArgumentTypes().length; i++) {
            Type t = constructor.getArgumentTypes()[i];
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
            j += t.getSize();
            mv.visitFieldInsn(Opcodes.PUTFIELD, cn.name, "arg$$" + i, t.getDescriptor());
        }
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // create interf method
        mv = cn.visitMethod(Opcodes.ACC_PUBLIC, ifName, invokedType.getDescriptor(), null, null);
        mv.visitCode();
        // load fields back into stack
        for (int i = 0; i < constructor.getArgumentTypes().length; i++) {
            Type t = constructor.getArgumentTypes()[i];
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, cn.name, "arg$$" + i, t.getDescriptor());
        }
        // load rest of args
        for (int i = 0, j = 1; i < invokedType.getArgumentTypes().length; i++) {
            Type t = invokedType.getArgumentTypes()[i];
            mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
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
        mv.visitInsn(bridge.getReturnType().getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // visit bridge
        mv = cn.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE, ifName, bridge.getDescriptor(), null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        // load args
        for (int i = 0, j = 1; i < bridge.getArgumentTypes().length; i++) {
            Type t = bridge.getArgumentTypes()[i];
            mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
            j += t.getSize();
            mv.visitTypeInsn(Opcodes.CHECKCAST, invokedType.getArgumentTypes()[i].getInternalName());
        }
        // invoke interf method
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, cn.name, ifName, invokedType.getDescriptor(), false);
        mv.visitTypeInsn(Opcodes.CHECKCAST, bridge.getReturnType().getInternalName());
        mv.visitInsn(bridge.getReturnType().getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cn.visitEnd();
        return cn;
    }


}
