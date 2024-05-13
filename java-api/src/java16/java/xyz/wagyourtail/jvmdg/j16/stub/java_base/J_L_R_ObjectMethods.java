package xyz.wagyourtail.jvmdg.j16.stub.java_base;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class J_L_R_ObjectMethods {

    @Modify(ref = @Ref(value = "java/lang/runtime/ObjectMethods", member = "bootstrap", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;"))
    public static void bootstrap(MethodNode mnode, int i, ClassNode cnode) {
        var indy = (InvokeDynamicInsnNode) mnode.instructions.get(i);
        var recordClass = (Type) indy.bsmArgs[0];
        var fieldNames = (String) indy.bsmArgs[1];
        var getters = new ArrayList<Handle>();
        for (int j = 2; j < indy.bsmArgs.length; j++) {
            getters.add((Handle) indy.bsmArgs[j]);
        }
        var mname = "jvmdowngrader$" + indy.name;
        switch (indy.name) {
            case "equals" -> makeEquals(cnode, mname, indy.desc, recordClass, getters);
            case "hashCode" -> makeHashCode(cnode, mname, indy.desc, recordClass, getters);
            case "toString" -> makeToString(cnode, mname, indy.desc, recordClass, fieldNames, getters);
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

    private static void makeEquals(ClassNode cnode, String mname, String desc, Type recordClass, ArrayList<Handle> getters) {
        var visitor = cnode.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, mname, desc, null, null);
        visitor.visitCode();
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        // if (this == obj) return true;
        var l0 = new Label();
        visitor.visitJumpInsn(Opcodes.IF_ACMPNE, l0);
        visitor.visitInsn(Opcodes.ICONST_1);
        visitor.visitInsn(Opcodes.IRETURN);
        visitor.visitLabel(l0);
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
                    visitor.visitInsn(Opcodes.FCMPL);
                    visitor.visitJumpInsn(Opcodes.IFNE, notEqual);
                }
                case Type.DOUBLE -> {
                    visitor.visitInsn(Opcodes.DCMPL);
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
        // return false;
        visitor.visitInsn(Opcodes.ICONST_0);
        visitor.visitInsn(Opcodes.IRETURN);
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
    }

    private static void makeHashCode(ClassNode cnode, String mname, String desc, Type recordClass, ArrayList<Handle> getters) {
        var visitor = cnode.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, mname, desc, null, null);
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
            switch (type.getSort()) {
                case Type.BOOLEAN -> {
                    visitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Boolean",
                        "valueOf",
                        "(Z)Ljava/lang/Boolean;",
                        false
                    );
                }
                case Type.BYTE -> {
                    visitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Byte",
                        "valueOf",
                        "(B)Ljava/lang/Byte;",
                        false
                    );
                }
                case Type.CHAR -> {
                    visitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Character",
                        "valueOf",
                        "(C)Ljava/lang/Character;",
                        false
                    );
                }
                case Type.SHORT -> {
                    visitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Short",
                        "valueOf",
                        "(S)Ljava/lang/Short;",
                        false
                    );
                }
                case Type.INT -> {
                    visitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Integer",
                        "valueOf",
                        "(I)Ljava/lang/Integer;",
                        false
                    );
                }
                case Type.LONG -> {
                    visitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Long",
                        "valueOf",
                        "(J)Ljava/lang/Long;",
                        false
                    );
                }
                case Type.FLOAT -> {
                    visitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Float",
                        "valueOf",
                        "(F)Ljava/lang/Float;",
                        false
                    );
                }
                case Type.DOUBLE -> {
                    visitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Double",
                        "valueOf",
                        "(D)Ljava/lang/Double;",
                        false
                    );
                }
                case Type.ARRAY, Type.OBJECT -> {
                    // do nothing
                }
                default -> throw new IllegalStateException("Unexpected value: " + type.getSort());
            }
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

    private static void makeToString(ClassNode cnode, String mname, String desc, Type recordClass, String fieldNames, List<Handle> getters) {
        var visitor = cnode.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, mname, desc, null, null);
        visitor.visitCode();
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        String last = recordClass.getInternalName();
        last = last.substring(last.lastIndexOf('/') + 1);
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
                    visitor.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/StringBuilder",
                        "append",
                        "(" + tdesc + ")Ljava/lang/StringBuilder;",
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
