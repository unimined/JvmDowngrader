package xyz.wagyourtail.jvmdg;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.replace.Replace;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class VersionProvider {

    private static final ScanResult defaultClasspath = resolveAllClasses(null);
    private final Map<Type, Pair<Type, Stub>> classStubs = new HashMap<>();
    private final Map<String, Pair<Method, Stub>> methodStubs = new HashMap<>();
    private final Map<String, Pair<Method, Replace>> methodReplaces = new HashMap<>();
    private final int targetVersion;
    private ScanResult allClasses;



    public VersionProvider(int targetVersion) {
        this.targetVersion = targetVersion;
        allClasses = defaultClasspath;
        init();
    }

    public VersionProvider(int targetVersion, Set<URI> classpath) {
        this.targetVersion = targetVersion;
        allClasses = resolveAllClasses(classpath);
        init();
    }

    public static int getCurrentClassVersion() {
        String version = System.getProperty("java.class.version");
        if (version != null) {
            try {
                return Integer.parseInt(version.split("\\.")[0]);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        throw new UnsupportedOperationException("Unable to determine current class version");
    }


    public static ScanResult resolveAllClasses(Set<URI> classpath) {
        ClassGraph classGraph = new ClassGraph()
            .enableSystemJarsAndModules();
//            .disableModuleScanning()
        if (classpath != null) {
            Set<URI> uris = new HashSet<>();
            // add java classes
            File javaHome = new File(System.getProperty("java.home"));
            if (getCurrentClassVersion() < Opcodes.V9) {
                uris.add(new File(javaHome, "lib/rt.jar").toURI());
            } else {
                File modules = new File(javaHome, "jmods");
                for (File f : modules.listFiles()) {
                    if (f.getName().endsWith(".jmod")) {
                        uris.add(f.toURI());
                    }
                }
            }
            // add user classes
            uris.addAll(classpath);
            classGraph.overrideClasspath(uris.toArray(new Object[0]));
        }
        return classGraph.scan();
    }

    public abstract void init();

    public static String getDescriptor(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            switch (clazz.getName()) {
                case "boolean":
                    return "Z";
                case "byte":
                    return "B";
                case "char":
                    return "C";
                case "short":
                    return "S";
                case "int":
                    return "I";
                case "long":
                    return "J";
                case "float":
                    return "F";
                case "double":
                    return "D";
                case "void":
                    return "V";
                default:
                    throw new InternalError("Unknown primitive type: " + clazz.getName());
            }
        }
        if (clazz.isArray()) {
            return clazz.getName().replace('.', '/');
        }
        return "L" + clazz.getName().replace('.', '/') + ";";
    }

    public void stub(Class<?> clazz) {
        Stub classStub = clazz.getAnnotation(Stub.class);
        if (classStub != null) {
            String name = classStub.ref().value();
            Type type;
            if (name.startsWith("L")) {
                type = Type.getType(name);
            } else {
                type = Type.getObjectType(name);
            }
            classStubs.put(type, new Pair<>(Type.getType(clazz), classStub));
        } else {
            for (Method m : clazz.getDeclaredMethods()) {
                methodStub(m);
            }
        }
    }

    private static Class<?>[] replaceSig = new Class<?>[] { ClassNode.class, MethodNode.class, int.class };

    public void methodStub(Method m) {
        Stub methodStub = m.getAnnotation(Stub.class);
        int modifiers = m.getModifiers();
        if (methodStub != null) {
            if ((modifiers & Opcodes.ACC_STATIC) == 0) {
                throw new IllegalArgumentException("Method " + Type.getType(m.getDeclaringClass()).getInternalName() + "." + m.getName() + " must be static");
            }
            if ((modifiers & Opcodes.ACC_PUBLIC) == 0) {
                throw new IllegalArgumentException("Method must be public");
            }
            Type method = Type.getType(m);
            Type[] args = method.getArgumentTypes();
            final Type owner;
            Type ret = method.getReturnType();
            String name;
            if (methodStub.ref().value().equals("")) {
                Type[] newArgs = new Type[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, newArgs.length);
                owner = args[0];
                args = newArgs;
            } else {
                String n = methodStub.ref().value();
                if (n.startsWith("L")) {
                    n = n.substring(1, n.length() - 1);
                }
                owner = Type.getObjectType(n);
            }
            if (!methodStub.ref().member().equals("")) {
                name = methodStub.ref().member();
            } else {
                name = m.getName();
            }
            if (name.equals("<init>")) {
                ret = Type.VOID_TYPE;
            }
            if (!methodStub.ref().desc().equals("")) {
                throw new UnsupportedOperationException("Not implemented yet");
            }
            if (!methodStub.subtypesOnly()) {
                methodStubs.put(owner.getInternalName() + ";" + name + Type.getMethodDescriptor(ret, args), new Pair<>(m, methodStub));
            }
            if (methodStub.subtypes() || methodStub.subtypesOnly()) {
                final String ownerName = owner.getClassName();
                for (ClassInfo ci : allClasses.getAllClasses().filter(new ClassInfoList.ClassInfoFilter() {
                    @Override
                    public boolean accept(ClassInfo classInfo) {
                        return classInfo.extendsSuperclass(ownerName) || classInfo.implementsInterface(ownerName);
                    }
                })) {
                    if (methodStub.returnDecendant()) {
                        if (!owner.getInternalName().equals(ret.getInternalName())) {
                            throw new IllegalArgumentException("Return type must be the same as the owner to return a decendant");
                        }
                        Type ret2 = Type.getObjectType(ci.getName().replace('.', '/'));
                        methodStubs.put(ci.getName().replace('.', '/') + ";" + name + Type.getMethodDescriptor(ret2, args), new Pair<>(m, methodStub));
                    }
                }
            }
        }
        Replace methodReplace = m.getAnnotation(Replace.class);
        if (methodReplace != null) {
            // assert that the method desc matches expected
            Class<?>[] paramTypes = m.getParameterTypes();
            if (paramTypes.length != replaceSig.length) {
                throw new IllegalArgumentException("Method must have the following signature: " + Arrays.toString(replaceSig));
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (paramTypes[i] != replaceSig[i]) {
                    throw new IllegalArgumentException("Method must have the following signature: " + Arrays.toString(replaceSig));
                }
            }
            String name = methodReplace.ref().value();
            if (name.startsWith("L")) {
                name = name.substring(1, name.length() - 1);
            }
            methodReplaces.put(name + ";" + methodReplace.ref().member() + methodReplace.ref().desc(), new Pair<>(m, methodReplace));
        }
    }

    public void downgrade(ClassNode clazz) throws InvocationTargetException, IllegalAccessException {
        transformClass(clazz);
        transformMethod(clazz);
        clazz.version = targetVersion - 1;
    }

    public void transformClass(ClassNode clazz) {
        // super
        Type type = Type.getObjectType(clazz.superName);
        if (classStubs.containsKey(type)) {
            clazz.superName = classStubs.get(type).getFirst().getInternalName();
        }

        // interface
        if (clazz.interfaces != null) {
            for (int i = 0; i < clazz.interfaces.size(); i++) {
                type = Type.getObjectType(clazz.interfaces.get(i));
                if (classStubs.containsKey(type)) {
                    clazz.interfaces.set(i, classStubs.get(type).getFirst().getInternalName());
                }
            }
        }

        // signature
        if (clazz.signature != null) {
            Pair<Boolean, String> pair = transformSignature(clazz.signature);
            if (pair.getFirst()) {
                clazz.signature = pair.getSecond();
            }
        }

        // field descriptor
        if (clazz.fields != null) {
            for (FieldNode field : clazz.fields) {
                type = Type.getType(field.desc);
                Pair<Boolean, Type> pair = replaceType(type);
                if (pair.getFirst()) {
                    field.desc = pair.getSecond().getDescriptor();
                }
            }
        }

        // method
        if (clazz.methods != null) {
            for (MethodNode method : new ArrayList<>(clazz.methods)) {
                // descriptor
                Pair<Boolean, Type> pair = replaceType(Type.getMethodType(method.desc));
                if (pair.getFirst()) {
                    method.desc = pair.getSecond().getDescriptor();
                }
                // signature
                if (method.signature != null) {
                    Pair<Boolean, String> pair2 = transformSignature(method.signature);
                    if (pair2.getFirst()) {
                        method.signature = pair2.getSecond();
                    }
                }

                // instructions
                if (method.instructions != null) {
                    for (AbstractInsnNode insn : method.instructions) {
                        if (insn instanceof InvokeDynamicInsnNode) {
                            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                            // check desc
                            pair = replaceType(Type.getMethodType(indy.desc));
                            if (pair.getFirst()) {
                                indy.desc = pair.getSecond().getDescriptor();
                            }
                            // check bsm
                            Type bsmOwner = Type.getObjectType(indy.bsm.getOwner());
                            if (classStubs.containsKey(bsmOwner)) {
                                indy.bsm = new Handle(
                                        indy.bsm.getTag(),
                                        classStubs.get(bsmOwner).getFirst().getInternalName(),
                                        indy.bsm.getName(),
                                        indy.bsm.getDesc(),
                                        indy.bsm.isInterface()
                                );
                            }
                            // check bsm desc
                            pair = replaceType(Type.getMethodType(indy.bsm.getDesc()));
                            if (pair.getFirst()) {
                                indy.bsm = new Handle(
                                        indy.bsm.getTag(),
                                        indy.bsm.getOwner(),
                                        indy.bsm.getName(),
                                        pair.getSecond().getDescriptor(),
                                        indy.bsm.isInterface()
                                );
                            }
                            // check args
                            for (int i = 0; i < indy.bsmArgs.length; i++) {
                                Object arg = indy.bsmArgs[i];
                                if (arg instanceof Handle) {
                                    Handle handle = (Handle) arg;
                                    Type handleOwner = Type.getObjectType(handle.getOwner());
                                    if (classStubs.containsKey(handleOwner)) {
                                        indy.bsmArgs[i] = new Handle(
                                                handle.getTag(),
                                                classStubs.get(handleOwner).getFirst().getInternalName(),
                                                handle.getName(),
                                                handle.getDesc(),
                                                handle.isInterface()
                                        );
                                    }
                                    pair = replaceType(Type.getType(handle.getDesc()));
                                    if (pair.getFirst()) {
                                        indy.bsmArgs[i] = new Handle(
                                                handle.getTag(),
                                                handle.getOwner(),
                                                handle.getName(),
                                                pair.getSecond().getDescriptor(),
                                                handle.isInterface()
                                        );
                                    }
                                } else if (arg instanceof Type) {
                                    type = (Type) arg;
                                    pair = replaceType(type);
                                    if (pair.getFirst()) {
                                        switch (type.getSort()) {
                                            case Type.ARRAY:
                                            case Type.OBJECT:
                                                indy.bsmArgs[i] = pair.getSecond();
                                                break;
                                            case Type.METHOD:
                                                indy.bsmArgs[i] = Type.getMethodType(pair.getSecond());
                                                break;
                                        }
                                    }
                                }
                            }
                        } else if (insn instanceof MethodInsnNode) {
                            MethodInsnNode min = (MethodInsnNode) insn;
                            Type owner = Type.getObjectType(min.owner);
                            if (classStubs.containsKey(owner)) {
                                min.owner = classStubs.get(owner).getFirst().getInternalName();
                            }
                            pair = replaceType(Type.getMethodType(min.desc));
                            if (pair.getFirst()) {
                                min.desc = pair.getSecond().getDescriptor();
                            }
                        } else if (insn instanceof FieldInsnNode) {
                            FieldInsnNode fin = (FieldInsnNode) insn;
                            Type owner = Type.getObjectType(fin.owner);
                            if (classStubs.containsKey(owner)) {
                                fin.owner = classStubs.get(owner).getFirst().getInternalName();
                            }
                            pair = replaceType(Type.getType(fin.desc));
                            if (pair.getFirst()) {
                                fin.desc = pair.getSecond().getDescriptor();
                            }
                        } else if (insn instanceof TypeInsnNode) {
                            TypeInsnNode tin = (TypeInsnNode) insn;
                            pair = replaceType(Type.getObjectType(tin.desc));
                            if (pair.getFirst()) {
                                tin.desc = pair.getSecond().getDescriptor();
                            }
                        } else if (insn instanceof MultiANewArrayInsnNode) {
                            MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) insn;
                            pair = replaceType(Type.getType(manain.desc));
                            if (pair.getFirst()) {
                                manain.desc = pair.getSecond().getDescriptor();
                            }
                        }
                    }
                }

                // local variables
                if (method.localVariables != null) {
                    for (LocalVariableNode local : method.localVariables) {
                        type = Type.getType(local.desc);
                        pair = replaceType(type);
                        if (pair.getFirst()) {
                            local.desc = pair.getSecond().getDescriptor();
                        }
                        if (local.signature != null) {
                            Pair<Boolean, String> pair2 = transformSignature(local.signature);
                            if (pair2.getFirst()) {
                                local.signature = pair2.getSecond();
                            }
                        }
                    }
                }
            }
        }

        // record components
        if (clazz.recordComponents != null) {
            for (RecordComponentNode record : clazz.recordComponents) {
                Pair<Boolean, Type> pair = replaceType(Type.getType(record.descriptor));
                if (pair.getFirst()) {
                    record.descriptor = pair.getSecond().getDescriptor();
                }
                if (record.signature != null) {
                    Pair<Boolean, String> p2 = transformSignature(record.signature);
                    if (p2.getFirst()) {
                        record.signature = p2.getSecond();
                    }
                }
            }
        }

    }

    public Pair<Boolean, Type> replaceType(Type type) {
        switch (type.getSort()) {
            case Type.METHOD:
                Type[] args = type.getArgumentTypes();
                Type ret = type.getReturnType();
                boolean changed = false;
                for (int i = 0; i < args.length; i++) {
                    Pair<Boolean, Type> pair = replaceType(args[i]);
                    if (pair.getFirst()) {
                        args[i] = pair.getSecond();
                        changed = true;
                    }
                }
                Pair<Boolean, Type> pair = replaceType(ret);
                if (pair.getFirst()) {
                    ret = pair.getSecond();
                    changed = true;
                }
                if (changed) {
                    return new Pair<>(true, Type.getMethodType(ret, args));
                }
                break;
            case Type.ARRAY:
                int dims = type.getDimensions();
                if (classStubs.containsKey(type.getElementType())) {
                    StringBuilder desc = new StringBuilder();
                    for (int i = 0; i < dims; i++) {
                        desc.append("[");
                    }
                    desc.append(classStubs.get(type.getElementType()).getFirst().getDescriptor());
                    return new Pair<>(true, Type.getType(desc.toString()));
                }
                break;
            case Type.OBJECT:
                if (classStubs.containsKey(type)) {
                    return new Pair<>(true, classStubs.get(type).getFirst());
                }
                break;
            default:
                break;
        }
        return new Pair<>(false, type);
    }

    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("L([^;<]+)([<;])");

    public Pair<Boolean, String> transformSignature(String signature) {
        Matcher matcher = SIGNATURE_PATTERN.matcher(signature);
        boolean changed = false;
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            Type type = Type.getObjectType(matcher.group(1));
            if (classStubs.containsKey(type)) {
                changed = true;
                matcher.appendReplacement(
                    sb,
                    "L" + classStubs.get(type).getFirst().getInternalName().replace("$", "\\$") + matcher.group(2)
                );
            }
        }
        matcher.appendTail(sb);
        return new Pair<>(changed, sb.toString());
    }

    public void transformMethod(ClassNode clazz) throws InvocationTargetException, IllegalAccessException {
        if (clazz.methods == null) {
            return;
        }

        for (MethodNode method : new ArrayList<>(clazz.methods)) {
            if (method.instructions == null) {
                continue;
            }

            for (int i = 0; i < method.instructions.size(); i++) {
                AbstractInsnNode insn = method.instructions.get(i);
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    String stubDesc = min.owner + ";" + min.name + min.desc;
                    if (methodStubs.containsKey(stubDesc)) {
                        Pair<Method, Stub> stub = methodStubs.get(stubDesc);
                        if (min.name.equals("<init>")) {
                            // remove new and dup, searching backwards..
                            int j;
                            int skip = 0;
                            for (j = i - 1; j >= 0; j--) {
                                AbstractInsnNode prev = method.instructions.get(j);
                                if (prev.getOpcode() == Opcodes.NEW && prev instanceof TypeInsnNode &&
                                    ((TypeInsnNode) prev).desc.equals(min.owner) && skip-- == 0) {
                                    method.instructions.remove(prev);
                                    // check and remove dup
                                    AbstractInsnNode dup = method.instructions.get(j);
                                    if (dup.getOpcode() == Opcodes.DUP) {
                                        method.instructions.remove(dup);
                                    } else {
                                        throw new IllegalStateException("Expected DUP after NEW, but got " + dup);
                                    }
                                    break;
                                } else if (prev.getOpcode() == Opcodes.INVOKESPECIAL &&
                                    prev instanceof MethodInsnNode &&
                                    ((MethodInsnNode) prev).owner.equals(((MethodInsnNode) insn).owner) &&
                                    ((MethodInsnNode) prev).name.equals("<init>")) {
                                    skip++;
                                }
                            }
                            if (j < 0) {
                                throw new IllegalStateException(
                                    "Could not find NEW for INVOKESPECIAL constructor " + insn);
                            }
                            i -= 2;
                        }
                        // check if return type is different
                        if (!min.name.equals("<init>") && !Type.getReturnType(stub.first).equals(Type.getReturnType(min.desc))) {
                            // check if next is a pop, because then we don't need to cast
                            AbstractInsnNode next = method.instructions.get(i + 1);
                            if (next.getOpcode() != Opcodes.POP) {
                                // cast return value
                                method.instructions.insert(insn, new TypeInsnNode(Opcodes.CHECKCAST, Type.getReturnType(min.desc).getInternalName()));
                                i++;
                            }
                        }
                        // replace method call
                        min.owner = Type.getType(stub.first.getDeclaringClass()).getInternalName();
                        min.name = stub.first.getName();
                        min.desc = Type.getMethodDescriptor(stub.first);
                        min.setOpcode(Opcodes.INVOKESTATIC);
                    }
                    if (methodReplaces.containsKey(stubDesc)) {
                        Pair<Method, Replace> replace = methodReplaces.get(stubDesc);
                        replace.getFirst().invoke(null, clazz, method, i);
                    }
                } else if (insn instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                    String stubDesc = indy.bsm.getOwner() + ";" + indy.bsm.getName() + indy.bsm.getDesc();
                    if (methodStubs.containsKey(stubDesc)) {
                        Pair<Method, Stub> stub = methodStubs.get(stubDesc);
                        // replace method call
                        indy.bsm = new Handle(Opcodes.H_INVOKESTATIC, Type.getType(stub.first.getDeclaringClass()).getInternalName(), stub.first.getName(), Type.getMethodDescriptor(stub.first), false);
                    }
                    if (methodReplaces.containsKey(stubDesc)) {
                        Pair<Method, Replace> replace = methodReplaces.get(stubDesc);
                        replace.getFirst().invoke(null, clazz, method, i);
                        continue;
                    }
                    /// bsm args
                    for (int j = 0; j < indy.bsmArgs.length; j++) {
                        Object arg = indy.bsmArgs[j];
                        if (arg instanceof Handle) {
                            Handle handle = (Handle) arg;
                            stubDesc = handle.getOwner() + ";" + handle.getName() + handle.getDesc();
                            if (methodStubs.containsKey(stubDesc)) {
                                Pair<Method, Stub> stub = methodStubs.get(stubDesc);
                                // replace method call
                                indy.bsmArgs[j] = new Handle(Opcodes.H_INVOKESTATIC, Type.getType(stub.first.getDeclaringClass()).getInternalName(), stub.first.getName(), Type.getMethodDescriptor(stub.first), false);
                            }
                            if (methodReplaces.containsKey(stubDesc)) {
                                throw new IllegalStateException("Cannot replace non-bootstrap method in indy with @Replace currently");
                            }
                        }
                    }
                }
            }
        }
    }

}
