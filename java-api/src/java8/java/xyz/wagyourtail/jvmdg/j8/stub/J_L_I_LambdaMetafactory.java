package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.*;
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
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@JEP(126)
public class J_L_I_LambdaMetafactory {

    /** {@code LambdaMetafactory} flag — generated class implements {@code Serializable}. */
    public static final int FLAG_SERIALIZABLE = 1 << 0;
    /** {@code LambdaMetafactory} flag — extra marker interfaces follow the flags arg. */
    public static final int FLAG_MARKERS = 1 << 1;
    /** {@code LambdaMetafactory} flag — extra bridge signatures follow the markers. */
    public static final int FLAG_BRIDGES = 1 << 2;

    @IndyDesugar(ref = @Ref(value = "Ljava/lang/invoke/LambdaMetafactory;", member = "metafactory", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"))
    public static void makeLambdaInnerClass(ModifyContext ctx) {
        InvokeDynamicInsnNode indy = ctx.indy;
        String ifName = indy.name;
        Type constructor = Type.getMethodType(indy.desc);
        Type bridge = (Type) indy.bsmArgs[0];
        Handle invokedMethod = (Handle) indy.bsmArgs[1];
        Type invokedType = (Type) indy.bsmArgs[2];
        boolean needsBridge = !bridge.equals(invokedType);
        makeLambdaInnerClassTail(ctx, ifName, constructor, bridge, invokedMethod, invokedType,
            needsBridge ? FLAG_BRIDGES : 0, null, needsBridge ? new Type[]{bridge} : null);
    }

    @IndyDesugar(ref = @Ref(value = "Ljava/lang/invoke/LambdaMetafactory;", member = "altMetafactory", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void makeAltLambdaInnerClass(ModifyContext ctx) {
        InvokeDynamicInsnNode indy = ctx.indy;
        String ifName = indy.name;
        Type constructor = Type.getMethodType(indy.desc);

        // bsmArgs: samMethodType, implMethod, instantiatedMethodType, flags,
        //          [markerCount, markerInterfaces...], [bridgeCount, bridges...]
        int argIndex = 0;
        Object[] args = indy.bsmArgs;
        Type interfaceMethodType = (Type) args[argIndex++];
        Handle invokedMethod = (Handle) args[argIndex++];
        Type invokedType = (Type) args[argIndex++];
        int flags = (int) args[argIndex++];
        // same argument-grammar validation the JDK applies — a malformed
        // site throws here, warns, and stays indy rather than silently
        // producing a wrong impl class
        if ((flags & ~(FLAG_SERIALIZABLE | FLAG_MARKERS | FLAG_BRIDGES)) != 0) {
            throw new IllegalArgumentException("altMetafactory: unknown flag bits 0x" +
                Integer.toHexString(flags));
        }

        Type[] altInterfaces = null;
        if ((flags & FLAG_MARKERS) != 0) {
            int count = (int) args[argIndex++];
            if (count > 0) {
                if (count > args.length - argIndex - ((flags & FLAG_BRIDGES) != 0 ? 1 : 0)) {
                    throw new IllegalArgumentException("altMetafactory: marker count " + count + " overruns bootstrap args");
                }
                altInterfaces = new Type[count];
                for (int i = 0; i < count; i++) {
                    altInterfaces[i] = (Type) args[argIndex++];
                }
            }
        }
        Type[] altMethods = null;
        if ((flags & FLAG_BRIDGES) != 0) {
            int count = (int) args[argIndex++];
            if (count > 0) {
                if (count > args.length - argIndex) {
                    throw new IllegalArgumentException("altMetafactory: bridge count " + count + " overruns bootstrap args");
                }
                altMethods = new Type[count];
                for (int i = 0; i < count; i++) {
                    altMethods[i] = (Type) args[argIndex++];
                }
            }
        }
        if (argIndex != args.length) {
            throw new IllegalArgumentException("altMetafactory: " + (args.length - argIndex) +
                " trailing bootstrap args");
        }

        // the erased sam signature is an implicit bridge whenever it differs
        // from the instantiated signature (same rule the JDK applies inside
        // InnerClassLambdaMetafactory)
        if (!interfaceMethodType.equals(invokedType)) {
            flags |= FLAG_BRIDGES;
            if (altMethods == null) {
                altMethods = new Type[]{interfaceMethodType};
            } else {
                Type[] old = altMethods;
                altMethods = new Type[old.length + 1];
                System.arraycopy(old, 0, altMethods, 1, old.length);
                altMethods[0] = interfaceMethodType;
            }
        }

        makeLambdaInnerClassTail(ctx, ifName, constructor, interfaceMethodType, invokedMethod, invokedType,
            flags, altInterfaces, altMethods);
    }

    private static void makeLambdaInnerClassTail(ModifyContext ctx, String ifName, Type constructor,
                                                 Type samMethodType, Handle invokedMethod, Type invokedType,
                                                 int flags, Type[] altInterfaces, Type[] altMethods) {
        MethodNode mnode = ctx.method;
        ClassNode cnode = ctx.node;
        Set<ClassNode> extra = ctx.extras;
        boolean noSynthetic = ctx.noSynthetic;
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
        ClassNode child = lambdaMetafactoryCreatorInternal(ctx, parentName, constructor, nextAnonymous, mnode.name, mnode.desc,
            ifName, samMethodType, invokedMethod, invokedType, cnode.version, flags, altInterfaces, altMethods);
        extra.add(child);
        // make invoked method at least package-private and not synthetic —
        // interfaces can't carry package-private members and the generated
        // impl class invokes through INVOKEINTERFACE, so private members on
        // an interface owner promote to public; cnode.access is unreliable
        // here (stub-source interfaces lose ACC_INTERFACE mid-pipeline), so
        // the handle's own itf bit decides
        if (invokedMethod.getOwner().equals(cnode.name)) {
            boolean itf = invokedMethod.isInterface() || (cnode.access & Opcodes.ACC_INTERFACE) != 0;
            for (MethodNode mn : cnode.methods) {
                if (mn.name.equals(invokedMethod.getName()) && mn.desc.equals(invokedMethod.getDesc())) {
                    mn.access &= ~Opcodes.ACC_SYNTHETIC;
                    if (itf) {
                        mn.access = (mn.access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
                    } else {
                        mn.access &= ~Opcodes.ACC_PRIVATE;
                    }
                }
            }
        }
        // add child as inner class
        cnode.innerClasses.add(new InnerClassNode(child.name, null, null, Opcodes.ACC_STATIC));
        // create method for constructing child
        String factoryName = "jvmdowngrader$lambda$" + mnode.name.replace("<", "$").replace(">", "$") + "$" + nextAnonymous;
        MethodVisitor mv = cnode.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | (noSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), factoryName, constructor.getDescriptor(), null, null);
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

        // serializable lambdas need a $deserializeLambda$ hook on the capturing
        // class — SerializedLambda.readResolve reflects it up by name
        if ((flags & FLAG_SERIALIZABLE) != 0) {
            emitDeserializeLambda(ctx, cnode, noSynthetic, factoryName, constructor, ifName, samMethodType, invokedMethod, invokedType);
        }

        // replace invokedynamic with method call
        mnode.instructions.set(ctx.indy, new MethodInsnNode(Opcodes.INVOKESTATIC, cnode.name, factoryName, constructor.getDescriptor(), (cnode.access & Opcodes.ACC_INTERFACE) != 0));
    }

    private static final MethodHandle LAMBDA_PROXY;

    static {
        try {
            LAMBDA_PROXY = MethodHandles.lookup().findStatic(J_L_I_LambdaMetafactory.class,
                "lambdaProxy", MethodType.methodType(Object.class, Class.class, MethodType.class,
                    MethodHandle.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Runtime impl for a direct {@code metafactory} invoke — the indy
     * callsite path is desugared by {@link #metafactory(ModifyContext)}.
     * Rather than spinning an impl class (which would need bytecode gen
     * in the ASM-free runtime), the callsite's target is a factory
     * producing {@link MethodHandleProxies#asInterfaceInstance} proxies:
     * captured args bind into {@code implMethod}'s leading params, the
     * tail is adapted to the erased SAM type, and {@code asInterfaceInstance}
     * throws for non-SAM interfaces (e.g. marker+bridge cases
     * {@code altMetafactory} covers) — an honest failure.
     */
    @Stub(ref = @Ref(value = "Ljava/lang/invoke/LambdaMetafactory;", member = "metafactory", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"))
    public static CallSite metafactory(MethodHandles.Lookup caller, String invokedName, MethodType invokedType, MethodType samMethodType, MethodHandle implMethod, MethodType instantiatedMethodType) {
        MethodHandle factory = MethodHandles.insertArguments(LAMBDA_PROXY, 0,
            invokedType.returnType(), samMethodType, implMethod);
        return new ConstantCallSite(factory
            .asCollector(Object[].class, invokedType.parameterCount())
            .asType(invokedType));
    }

    public static Object lambdaProxy(Class<?> iface, MethodType samMethodType, MethodHandle implMethod,
                                     Object[] captured) {
        MethodHandle impl = MethodHandles.insertArguments(implMethod, 0, captured);
        return MethodHandleProxies.asInterfaceInstance(iface, impl.asType(samMethodType));
    }

    @Stub(ref = @Ref(value = "Ljava/lang/invoke/LambdaMetafactory;", member = "altMetafactory", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static CallSite altMetafactory(MethodHandles.Lookup caller, String invokedName, MethodType invokedType, Object... args) {
        throw new MissingStubError("altMetafactory is not supported currently... because I don't know enough about how it works.");
    }

    public static ClassNode lambdaMetafactoryCreatorInternal(ModifyContext ctx, String parentName, Type constructor, int nextAvailableAnonymous, String mname, String mdesc, String ifName, Type samMethodType, Handle invokedMethod, Type invokedType, int version, int flags, Type[] altInterfaces, Type[] altMethods) {
        ClassNode cn = new ClassNode(Opcodes.ASM9);
        // class name and metadata — emit at the caller's class version (floor
        // V1_7): the impl invokes the impl-handle directly and
        // invokestatic→InterfaceMethodref constants only verify in classfiles >= 52.
        // final, matching JDK InnerClassLambdaMetafactory's generated shape
        List<String> interfaces = new ArrayList<>();
        interfaces.add(constructor.getReturnType().getInternalName());
        if ((flags & FLAG_MARKERS) != 0 && altInterfaces != null) {
            for (Type altInterface : altInterfaces) {
                interfaces.add(altInterface.getInternalName());
            }
        }
        if ((flags & FLAG_SERIALIZABLE) != 0 && !interfaces.contains("java/io/Serializable")) {
            interfaces.add("java/io/Serializable");
        }
        cn.visit(Math.max(Opcodes.V1_7, version), Opcodes.ACC_SUPER | Opcodes.ACC_FINAL, parentName + "$" + nextAvailableAnonymous, null, "java/lang/Object", interfaces.toArray(new String[0]));
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
        if (invokedMethod.getTag() == Opcodes.H_INVOKESTATIC
                || invokedMethod.getTag() == Opcodes.H_NEWINVOKESPECIAL) {
            // statics and constructors take no receiver — the emitted arg
            // stack is exactly the handle's declared params (the NEW/DUP
            // uninitialized ref isn't an arg)
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
        Type rtype = invokedType.getReturnType();
        doCast(mv, rtype, ret);
        mv.visitInsn(rtype.getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // visit bridges
        if ((flags & FLAG_BRIDGES) != 0 && altMethods != null) {
            for (Type bridge : altMethods) {
                mv = cn.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE, ifName, bridge.getDescriptor(), null, null);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                // load args
                Type[] bridgeArgTypes = bridge.getArgumentTypes();
                Type[] invokedArgTypes = invokedType.getArgumentTypes();
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

        // serialization machinery, mirroring InnerClassLambdaMetafactory:
        // serializable lambdas get an instantiatedMethodType + writeReplace
        // producing a SerializedLambda; non-serializable ones get hostile
        // read/writeObject methods so a marker-carried Serializable interface
        // can't be abused to deserialize one
        if ((flags & FLAG_SERIALIZABLE) != 0) {
            generateSerializationFriendlyMethods(ctx, cn, parentName, constructor, ifName, samMethodType, invokedMethod, invokedType);
        } else {
            generateSerializationHostileMethods(cn);
        }

        cn.visitEnd();
        return cn;
    }

    /** {@code t}'s method descriptor rebuilt from source-vocabulary types. */
    private static String unstubDesc(ModifyContext ctx, Type methodType) {
        Type[] args = methodType.getArgumentTypes();
        Type[] u = new Type[args.length];
        for (int i = 0; i < args.length; i++) {
            u[i] = ctx.unstub(args[i]);
        }
        return Type.getMethodDescriptor(ctx.unstub(methodType.getReturnType()), u);
    }

    private static void generateSerializationFriendlyMethods(ModifyContext ctx, ClassNode cn, String parentName, Type constructor,
                                                             String ifName, Type samMethodType, Handle invokedMethod, Type invokedType) {
        // private static final MethodType instantiatedMethodType — the JDK
        // stores the instantiated signature so $deserializeLambda$ can
        // validate it; init via a CONSTANT_MethodType ldc (legal >= 51)
        cn.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
            "instantiatedMethodType", "Ljava/lang/invoke/MethodType;", null, null).visitEnd();
        MethodVisitor mv = cn.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitLdcInsn(Type.getMethodType(unstubDesc(ctx, invokedType)));
        mv.visitFieldInsn(Opcodes.PUTSTATIC, cn.name, "instantiatedMethodType", "Ljava/lang/invoke/MethodType;");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // private Object writeReplace() { return new SerializedLambda(...) }
        Type[] ctorArgs = constructor.getArgumentTypes();
        mv = cn.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "writeReplace", "()Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/invoke/SerializedLambda");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(Type.getObjectType(parentName));
        // SerializedLambda class-name fields are internal (slash) names in
        // source vocabulary — matching the JDK's wire format exactly
        mv.visitLdcInsn(ctx.unstub(constructor.getReturnType()).getInternalName());
        mv.visitLdcInsn(ifName);
        mv.visitLdcInsn(unstubDesc(ctx, samMethodType));
        mv.visitLdcInsn(invokedMethod.getTag());
        mv.visitLdcInsn(ctx.unstub(Type.getObjectType(invokedMethod.getOwner())).getInternalName());
        mv.visitLdcInsn(invokedMethod.getName());
        mv.visitLdcInsn(unstubDesc(ctx, Type.getMethodType(invokedMethod.getDesc())));
        mv.visitLdcInsn(unstubDesc(ctx, invokedType));
        // captured args, boxed into an Object[]
        mv.visitLdcInsn(ctorArgs.length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < ctorArgs.length; i++) {
            Type t = ctorArgs[i];
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(i);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, cn.name, "arg$$" + i, t.getDescriptor());
            if (t.getSort() < Type.ARRAY) {
                doCast(mv, boxType(t), t);
            }
            mv.visitInsn(Opcodes.AASTORE);
        }
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/invoke/SerializedLambda", "<init>",
            "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void generateSerializationHostileMethods(ClassNode cn) {
        String[] exceptions = {"java/io/NotSerializableException"};
        cn.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "writeObject", "(Ljava/io/ObjectOutputStream;)V", null, exceptions)
            .visitEnd();
        cn.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "readObject", "(Ljava/io/ObjectInputStream;)V", null, exceptions)
            .visitEnd();
        cn.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "readObjectNoData", "()V", null, exceptions)
            .visitEnd();
        for (int i = cn.methods.size() - 3; i < cn.methods.size(); i++) {
            MethodVisitor mv = cn.methods.get(i);
            mv.visitCode();
            emitNotSerializableThrow(mv);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
    }

    private static void emitNotSerializableThrow(MethodVisitor mv) {
        mv.visitTypeInsn(Opcodes.NEW, "java/io/NotSerializableException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("Non-serializable lambda");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/NotSerializableException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);
    }

    /**
     * Emits {@code $deserializeLambda$} on the capturing class —
     * {@code SerializedLambda}'s deserialization path resolves
     * {@code captorClass.$deserializeLambda$} reflectively. javac already
     * emits this method for classes containing serializable lambdas (its
     * cases re-materialize through their own indy sites, which desugar
     * independently), so this only fires for class files lacking one —
     * merging into javac's frame-complete method would break verification.
     * Repeated serializable sites in such classes accumulate cases ahead of
     * the trailing {@code IllegalArgumentException}, each branch target
     * carrying an explicit F_SAME frame (emitted code owns its stackmap).
     */
    private static void emitDeserializeLambda(ModifyContext ctx, ClassNode cnode, boolean noSynthetic, String factoryName, Type constructor,
                                              String ifName, Type samMethodType, Handle invokedMethod, Type invokedType) {
        String dserName = "$deserializeLambda$";
        String dserDesc = "(Ljava/lang/invoke/SerializedLambda;)Ljava/lang/Object;";
        MethodNode dser = null;
        for (MethodNode mn : cnode.methods) {
            if (mn.name.equals(dserName) && mn.desc.equals(dserDesc)) {
                dser = mn;
                break;
            }
        }
        if (dser != null) {
            if (dser.instructions == null || !isEmittedDser(dser)) {
                // an existing $deserializeLambda$ is a foreign method
                // (javac's or hand-written) — do not splice into it; its
                // cases dispatch through their own indy sites, which desugar
                // independently
                return;
            }
        } else {
            dser = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | (noSynthetic ? 0 : Opcodes.ACC_SYNTHETIC),
                dserName, dserDesc, null, null);
            // tail only — cases get prepended ahead of it
            dser.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
            dser.visitInsn(Opcodes.DUP);
            dser.visitLdcInsn("Invalid lambda deserialization");
            dser.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
            dser.visitInsn(Opcodes.ATHROW);
            dser.visitMaxs(0, 0);
            cnode.methods.add(dser);
        }

        // case: all identifying SerializedLambda fields must match before we
        // trust it enough to call the lambda factory — the same fields the
        // JDK's injected $deserializeLambda$ compares, in source vocabulary
        InsnList body = new InsnList();
        LabelNode skip = new LabelNode();
        // getImplMethodKind — an int compare, not a string getter
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/SerializedLambda", "getImplMethodKind", "()I", false));
        body.add(new LdcInsnNode(invokedMethod.getTag()));
        body.add(new JumpInsnNode(Opcodes.IF_ICMPNE, skip));
        String[] getters = {
            "getFunctionalInterfaceClass", ctx.unstub(constructor.getReturnType()).getInternalName(),
            "getFunctionalInterfaceMethodName", ifName,
            "getFunctionalInterfaceMethodSignature", unstubDesc(ctx, samMethodType),
            "getImplClass", ctx.unstub(Type.getObjectType(invokedMethod.getOwner())).getInternalName(),
            "getImplMethodName", invokedMethod.getName(),
            "getImplMethodSignature", unstubDesc(ctx, Type.getMethodType(invokedMethod.getDesc())),
        };
        for (int i = 0; i < getters.length; i += 2) {
            body.add(new VarInsnNode(Opcodes.ALOAD, 0));
            body.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/SerializedLambda", getters[i], "()Ljava/lang/String;", false));
            body.add(new LdcInsnNode(getters[i + 1]));
            body.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
            body.add(new JumpInsnNode(Opcodes.IFEQ, skip));
        }
        // captured args: count check, then each arg typed back through the
        // generated factory — matching javac's emitted shape
        Type[] ctorArgs = constructor.getArgumentTypes();
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/SerializedLambda", "getCapturedArgCount", "()I", false));
        body.add(new LdcInsnNode(ctorArgs.length));
        body.add(new JumpInsnNode(Opcodes.IF_ICMPNE, skip));
        for (int i = 0; i < ctorArgs.length; i++) {
            body.add(new VarInsnNode(Opcodes.ALOAD, 0));
            body.add(new LdcInsnNode(i));
            body.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/SerializedLambda", "getCapturedArg", "(I)Ljava/lang/Object;", false));
            emitUnboxOrCast(body, ctorArgs[i]);
        }
        body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cnode.name, factoryName, constructor.getDescriptor(), (cnode.access & Opcodes.ACC_INTERFACE) != 0));
        body.add(new InsnNode(Opcodes.ARETURN));
        body.add(skip);
        body.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        // prepend ahead of the trailing throw so cases accumulate
        dser.instructions.insertBefore(dser.instructions.getFirst(), body);
    }

    /** Whether {@code m} is a {@code $deserializeLambda$} we emitted (vs javac's). */
    private static boolean isEmittedDser(MethodNode m) {
        // our case sequence always opens with ALOAD 0 + the
        // getImplMethodKind/getFunctionalInterfaceClass getters; javac's
        // injected method opens with getImplMethodName+astore feeding a
        // lookupswitch — either way the second insn discriminates reliably
        AbstractInsnNode second = m.instructions.getFirst() == null ? null : m.instructions.getFirst().getNext();
        return second instanceof MethodInsnNode
            && "java/lang/invoke/SerializedLambda".equals(((MethodInsnNode) second).owner)
            && "getFunctionalInterfaceClass".equals(((MethodInsnNode) second).name);
    }

    /** {@code Object} on stack → the field's declared type (unbox or checkcast). */
    private static void emitUnboxOrCast(InsnList insns, Type t) {
        String unbox;
        switch (t.getSort()) {
            case Type.BOOLEAN: unbox = "booleanValue"; break;
            case Type.BYTE: unbox = "byteValue"; break;
            case Type.CHAR: unbox = "charValue"; break;
            case Type.SHORT: unbox = "shortValue"; break;
            case Type.INT: unbox = "intValue"; break;
            case Type.LONG: unbox = "longValue"; break;
            case Type.FLOAT: unbox = "floatValue"; break;
            case Type.DOUBLE: unbox = "doubleValue"; break;
            case Type.OBJECT:
            case Type.ARRAY:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, t.getInternalName()));
                return;
            default:
                return;
        }
        Type boxed = boxType(t);
        insns.add(new TypeInsnNode(Opcodes.CHECKCAST, boxed.getInternalName()));
        insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, boxed.getInternalName(), unbox, "()" + t.getDescriptor(), false));
    }

    private static Type boxType(Type primitive) {
        switch (primitive.getSort()) {
            case Type.BOOLEAN: return Type.getObjectType("java/lang/Boolean");
            case Type.BYTE: return Type.getObjectType("java/lang/Byte");
            case Type.CHAR: return Type.getObjectType("java/lang/Character");
            case Type.SHORT: return Type.getObjectType("java/lang/Short");
            case Type.INT: return Type.getObjectType("java/lang/Integer");
            case Type.LONG: return Type.getObjectType("java/lang/Long");
            case Type.FLOAT: return Type.getObjectType("java/lang/Float");
            case Type.DOUBLE: return Type.getObjectType("java/lang/Double");
            default: return Type.getObjectType("java/lang/Object");
        }
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
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
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
