package xyz.wagyourtail.jvmdg.j16.stub.java_base;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.version.IndyDesugar;
import xyz.wagyourtail.jvmdg.version.ModifyContext;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.TypeDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class J_L_R_ObjectMethods {

    @IndyDesugar(ref = @Ref(value = "java/lang/runtime/ObjectMethods", member = "bootstrap", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;"))
    public static void bootstrap(ModifyContext ctx) {
        MethodNode mnode = ctx.method;
        ClassNode cnode = ctx.node;
        boolean noSynthetic = ctx.noSynthetic;
        var indy = ctx.indy;
        var recordClass = (Type) indy.bsmArgs[0];
        var fieldNames = (String) indy.bsmArgs[1];
        var getters = new ArrayList<Handle>();
        for (int j = 2; j < indy.bsmArgs.length; j++) {
            getters.add((Handle) indy.bsmArgs[j]);
        }
        var mname = "jvmdowngrader$" + mnode.name + "$" + indy.name;
        mname = mname.replace("<", "$").replace(">", "$");
        switch (indy.name) {
            case "equals" -> makeEquals(cnode, mname, indy.desc, recordClass, getters, noSynthetic);
            case "hashCode" -> makeHashCode(cnode, mname, indy.desc, recordClass, getters, noSynthetic);
            case "toString" -> makeToString(cnode, mname, indy.desc, recordClass, fieldNames, getters, noSynthetic);
        }
        mnode.instructions.insert(indy,
            new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                cnode.name,
                mname,
                indy.desc,
                false
            )
        );
        mnode.instructions.remove(indy);
    }

    private static final MethodHandle RECORD_EQUALS;
    private static final MethodHandle RECORD_HASHCODE;
    private static final MethodHandle RECORD_TOSTRING;

    static {
        try {
            RECORD_EQUALS = MethodHandles.lookup().findStatic(J_L_R_ObjectMethods.class,
                "recordEquals", MethodType.methodType(boolean.class, Class.class, MethodHandle[].class,
                    Object.class, Object.class));
            RECORD_HASHCODE = MethodHandles.lookup().findStatic(J_L_R_ObjectMethods.class,
                "recordHashCode", MethodType.methodType(int.class, MethodHandle[].class, Object.class));
            RECORD_TOSTRING = MethodHandles.lookup().findStatic(J_L_R_ObjectMethods.class,
                "recordToString", MethodType.methodType(String.class, String.class, String.class,
                    MethodHandle[].class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Runtime impl for a direct {@code ObjectMethods.bootstrap} invoke —
     * the indy callsite path is desugared by {@link #bootstrap(ModifyContext)}.
     * Builds a {@link ConstantCallSite} whose target is a bound static
     * helper {@code asType}'d onto the requested callsite type; getters
     * are adapted to {@code (Object)Object} (asType boxes primitives).
     */
    @Stub(ref = @Ref(value = "java/lang/runtime/ObjectMethods", member = "bootstrap", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;"))
    public static Object bootstrapStub(MethodHandles.Lookup lookup, String name, TypeDescriptor type, Class<?> recordClass, String names, MethodHandle... getters) throws Throwable {
        MethodHandle target;
        switch (name) {
            case "equals":
                target = MethodHandles.insertArguments(RECORD_EQUALS, 0, recordClass,
                    (Object) getters);
                break;
            case "hashCode":
                target = MethodHandles.insertArguments(RECORD_HASHCODE, 0, (Object) getters);
                break;
            case "toString":
                target = MethodHandles.insertArguments(RECORD_TOSTRING, 0,
                    recordClass.getSimpleName(), names, (Object) getters);
                break;
            default:
                throw new IllegalArgumentException("Unknown ObjectMethods bootstrap: " + name);
        }
        return new ConstantCallSite(target.asType((MethodType) type));
    }

    private static Object get(MethodHandle getter, Object self) throws Throwable {
        return getter.asType(MethodType.methodType(Object.class, Object.class)).invoke(self);
    }

    public static boolean recordEquals(Class<?> recordClass, MethodHandle[] getters, Object self,
                                       Object other) throws Throwable {
        if (self == other) return true;
        if (other == null || !recordClass.isInstance(other)) return false;
        for (MethodHandle getter : getters) {
            Class<?> rt = getter.type().returnType();
            Object a = get(getter, self);
            Object b = get(getter, other);
            // JDK record semantics: float/double compare via wrapper
            // compare (NaN equal, -0.0 != 0.0); everything else
            // value-equals after boxing
            if (rt == float.class) {
                if (Float.compare((Float) a, (Float) b) != 0) return false;
            } else if (rt == double.class) {
                if (Double.compare((Double) a, (Double) b) != 0) return false;
            } else if (!Objects.equals(a, b)) {
                return false;
            }
        }
        return true;
    }

    public static int recordHashCode(MethodHandle[] getters, Object self) throws Throwable {
        int hash = 0;
        for (MethodHandle getter : getters) {
            Class<?> rt = getter.type().returnType();
            Object v = get(getter, self);
            int c;
            if (rt == boolean.class) {
                c = (Boolean) v ? 1231 : 1237;
            } else if (rt == byte.class || rt == short.class || rt == int.class) {
                c = ((Number) v).intValue();
            } else if (rt == char.class) {
                c = (Character) v;
            } else if (rt == long.class) {
                c = Long.hashCode((Long) v);
            } else if (rt == float.class) {
                c = Float.hashCode((Float) v);
            } else if (rt == double.class) {
                c = Double.hashCode((Double) v);
            } else {
                c = v == null ? 0 : v.hashCode();
            }
            hash = 31 * hash + c;
        }
        return hash;
    }

    public static String recordToString(String simpleName, String names, MethodHandle[] getters,
                                        Object self) throws Throwable {
        StringBuilder sb = new StringBuilder(simpleName).append('[');
        String[] parts = names.isEmpty() ? new String[0] : names.split(";");
        for (int i = 0; i < getters.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(parts[i]).append('=').append(get(getters[i], self));
        }
        return sb.append(']').toString();
    }

    private static void makeEquals(ClassNode cnode, String mname, String desc, Type recordClass, ArrayList<Handle> getters, boolean noSynthetic) {
        var visitor = cnode.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | (noSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), mname, desc, null, null);
        visitor.visitCode();
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        // if (this == obj) return true;
        var l0 = new Label();
        visitor.visitJumpInsn(Opcodes.IF_ACMPNE, l0);
        visitor.visitInsn(Opcodes.ICONST_1);
        visitor.visitInsn(Opcodes.IRETURN);
        visitor.visitLabel(l0);
        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        var notEqual = new Label();
        // if (obj != null) {
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitJumpInsn(Opcodes.IFNULL, notEqual);
        // if (obj instanceof clazz) {
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitTypeInsn(Opcodes.INSTANCEOF, recordClass.getInternalName());
        visitor.visitJumpInsn(Opcodes.IFEQ, notEqual);
        // obj = (clazz) obj;
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitTypeInsn(Opcodes.CHECKCAST, recordClass.getInternalName());
        visitor.visitVarInsn(Opcodes.ASTORE, 2);
        for (Handle getter : getters) {
            // if (this.field != obj.field) return false;
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, cnode.name, getter.getName(), getter.getDesc());
            visitor.visitVarInsn(Opcodes.ALOAD, 2);
            visitor.visitFieldInsn(Opcodes.GETFIELD, recordClass.getInternalName(), getter.getName(), getter.getDesc());
            // check type
            var type = Type.getType(getter.getDesc());
            switch (type.getSort()) {
                case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> {
                    visitor.visitJumpInsn(Opcodes.IF_ICMPNE, notEqual);
                }
                case Type.LONG -> {
                    visitor.visitInsn(Opcodes.LCMP);
                    visitor.visitJumpInsn(Opcodes.IFNE, notEqual);
                }
                case Type.FLOAT -> {
                    // Use compare over FCMPL to handle NaN and -0.0/0.0 correctly (which is what the real ObjectMethods does)
                    visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "compare", "(FF)I", false);
                    visitor.visitJumpInsn(Opcodes.IFNE, notEqual);
                }
                case Type.DOUBLE -> {
                    // Same idea but for DCMPL
                    visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "compare", "(DD)I", false);
                    visitor.visitJumpInsn(Opcodes.IFNE, notEqual);
                }
                case Type.ARRAY, Type.OBJECT -> {
                    visitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/util/Objects",
                        "equals",
                        "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                        false
                    );
                    visitor.visitJumpInsn(Opcodes.IFEQ, notEqual);
                }
                default -> throw new IllegalStateException("Unexpected value: " + type.getSort());
            }
        }
        // return true;
        visitor.visitInsn(Opcodes.ICONST_1);
        visitor.visitInsn(Opcodes.IRETURN);
        visitor.visitLabel(notEqual);
        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        // return false;
        visitor.visitInsn(Opcodes.ICONST_0);
        visitor.visitInsn(Opcodes.IRETURN);
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
    }

    private static void makeHashCode(ClassNode cnode, String mname, String desc, Type recordClass, ArrayList<Handle> getters, boolean noSynthetic) {
        var visitor = cnode.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | (noSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), mname, desc, null, null);
        visitor.visitCode();
        visitor.visitLdcInsn(getters.size());
        visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        visitor.visitVarInsn(Opcodes.ASTORE, 1);
        for (int i = 0; i < getters.size(); i++) {
            var getter = getters.get(i);
            visitor.visitVarInsn(Opcodes.ALOAD, 1);
            visitor.visitLdcInsn(i);
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, recordClass.getInternalName(), getter.getName(), getter.getDesc());
            var type = Type.getType(getter.getDesc());
            ASMUtils.boxType(visitor, type);
            visitor.visitInsn(Opcodes.AASTORE);
        }
        // return Objects.hashCode(fields);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/util/Arrays",
            "hashCode",
            "([Ljava/lang/Object;)I",
            false
        );
        visitor.visitInsn(Opcodes.IRETURN);
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
    }

    private static void makeToString(ClassNode cnode, String mname, String desc, Type recordClass, String fieldNames, List<Handle> getters, boolean noSynthetic) {
        var visitor = cnode.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | (noSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), mname, desc, null, null);
        visitor.visitCode();
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        String last = recordClass.getInternalName();
        last = last.substring(Math.max(last.lastIndexOf('/'), last.lastIndexOf('$')) + 1);
        visitor.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        visitor.visitInsn(Opcodes.DUP);
        visitor.visitLdcInsn(last + "[");
        visitor.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/StringBuilder",
            "<init>",
            "(Ljava/lang/String;)V",
            false
        );
        String[] fns;
        if (fieldNames.isEmpty()) {
            fns = new String[0];
        } else {
            fns = fieldNames.split(";");
        }
        if (fns.length != getters.size()) {
            throw new IllegalStateException("field names and getters size mismatch, \nfn's: (" + fns.length + ") " + Arrays.toString(fns) + "\ngetters: (" + getters.size() + ") " + getters);
        }
        for (int i = 0; i < fns.length; i++) {
            visitor.visitLdcInsn(fns[i] + "=");
            visitor.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false
            );
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, recordClass.getInternalName(), getters.get(i).getName(), getters.get(i).getDesc());
            var type = Type.getType(getters.get(i).getDesc());
            var tdesc = type.getDescriptor();
            switch (type.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY:
                    if (tdesc.equals("Ljava/lang/String;")) {
                        visitor.visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                            false
                        );
                    } else {
                        visitor.visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                            false
                        );
                    }
                    break;
                default:
                    // Widen primitives to fit 'append(...)' signature
                    var appendDesc = switch (type.getSort()) {
                        case Type.BOOLEAN -> "Z";
                        case Type.CHAR -> "C";
                        case Type.BYTE, Type.SHORT, Type.INT -> "I"; // no append for byte/short, widen to int
                        case Type.LONG -> "J";
                        case Type.FLOAT -> "F";
                        case Type.DOUBLE -> "D";
                        default -> tdesc;
                    };
                    visitor.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/StringBuilder",
                        "append",
                        "(" + appendDesc + ")Ljava/lang/StringBuilder;",
                        false
                    );
                    break;
            }
            if (i < fns.length - 1) {
                visitor.visitLdcInsn(", ");
                visitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                    false
                );
            }
        }
        visitor.visitLdcInsn("]");
        visitor.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "append",
            "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
            false
        );
        visitor.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "toString",
            "()Ljava/lang/String;",
            false
        );
        visitor.visitInsn(Opcodes.ARETURN);
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
    }

}
