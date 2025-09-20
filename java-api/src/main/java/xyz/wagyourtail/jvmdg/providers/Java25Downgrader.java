package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.j25.stub.J_L_IO;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Java25Downgrader extends VersionProvider {

    public Java25Downgrader() {
        super(Opcodes.V25, Opcodes.V24, 0);
    }

    @Override
    public void init() {
        // -- java.base --
        stub(J_L_IO.class);

    }

    @Override
    public ClassNode otherTransforms(ClassNode clazz, Set<ClassNode> extra, Function<String, ClassNode> getReadOnly, Set<String> warnings, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        addMissingMains(clazz, getReadOnly, superTypeResolver);
        renameInstanceMains(clazz);
        return super.otherTransforms(clazz);
    }

    private void renameInstanceMains(ClassNode clazz) {
        for (MethodNode method : clazz.methods) {
            if (method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V") && (method.access & Opcodes.ACC_STATIC) == 0) {
                method.name = "jvmdg$instanceMain";
            }
            for (AbstractInsnNode insn : method.instructions) {
                if (insn.getType() == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    if (min.getOpcode() != Opcodes.INVOKESTATIC && min.name.equals("main") && min.desc.equals("([Ljava/lang/String;)V")) {
                        min.name = "jvmdg$instanceMain";
                        break;
                    }
                }
            }
        }
    }

    private void addMissingMains(ClassNode clazz, Function<String, ClassNode> getReadOnly, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
//        0. A public static void main(String[] args) method
//        1. A static void main(String[] args) method of non-private access (i.e., public, protected or package) declared in the launched class,
//        2. A static void main() method of non-private access declared in the launched class,
//        3. A void main(String[] args) instance method of non-private access declared in the launched class or inherited from a superclass, or, finally,
//        4. A void main() instance method of non-private access declared in the launched class or inherited from a superclass.

        EnumSet<MainType> mainMethods = EnumSet.noneOf(MainType.class);
        int publicStatic = (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        List<Pair<Type, Boolean>> superTypes = superTypeResolver.apply(Type.getObjectType(clazz.name));
        superTypes.add(0, new Pair<>(Type.getObjectType(clazz.name), false));
        for (Pair<Type, Boolean> superType : superTypes) {
            ClassNode cn = getReadOnly.apply(superType.getFirst().getInternalName());
            if (cn == null) continue;
            for (MethodNode mn : cn.methods) {
                if (!mn.name.equals("main") || (mn.access & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE) {
                    continue;
                }
                if (mn.desc.equals("([Ljava/lang/String;)V")) {
                    if ((mn.access & publicStatic) == publicStatic) {
                        mainMethods.add(MainType.TRADITIONAL);
                    } else if ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
                        mainMethods.add(MainType.STATIC_ARGS);
                    } else {
                        mainMethods.add(MainType.INSTANCE_ARGS);
                    }
                } else if (mn.desc.equals("()V")) {
                    if ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
                        mainMethods.add(MainType.STATIC);
                    } else {
                        mainMethods.add(MainType.INSTANCE);
                    }
                }

            }
        }
        // has traditional main method
        if (mainMethods.isEmpty() || mainMethods.contains(MainType.TRADITIONAL)) {
            return;
        }
        // The iterator returned by the iterator method traverses the elements in their natural order (the order in which the enum constants are declared)
        MainType firstType = mainMethods.iterator().next();
        MethodVisitor mv;
        switch (firstType) {
            case STATIC_ARGS:
                // if it's in the current class make it public
                for (MethodNode mn : clazz.methods) {
                    if (mn.name.equals("main") && mn.desc.equals("([Ljava/lang/String;)V")) {
                        mn.access |= Opcodes.ACC_PUBLIC;
                    }
                }
                break;
            case STATIC:
                // add a function with the args that calls the no-arg version
                mv = clazz.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | (downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "main", "([Ljava/lang/String;)V", null, null);
                mv.visitCode();
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, clazz.name, "main", "()V", false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
                break;
            case INSTANCE_ARGS:
                // can't have instance main on interface
                if ((clazz.access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE) break;
                mv = clazz.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | (downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "main", "([Ljava/lang/String;)V", null, null);
                mv.visitCode();
                mv.visitTypeInsn(Opcodes.NEW, clazz.name);
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz.name, "<init>", "()V", false);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz.name, "jvmdg$instanceMain", "([Ljava/lang/String;)V", false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
                break;
            case INSTANCE:
                // can't have instance main on interface
                if ((clazz.access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE) break;
                mv = clazz.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | (downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "main", "([Ljava/lang/String;)V", null, null);
                mv.visitCode();
                mv.visitTypeInsn(Opcodes.NEW, clazz.name);
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz.name, "<init>", "()V", false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz.name, "main", "()V", false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
                break;
            default:
                throw new IllegalArgumentException("No main method found");
        }

    }

    public static enum MainType {
        TRADITIONAL("([Ljava/lang/String;)V"),
        STATIC_ARGS("([Ljava/lang/String;)V"),
        STATIC("()V"),
        INSTANCE_ARGS("([Ljava/lang/String;)V"),
        INSTANCE("()V");

        public final String desc;

        MainType(String desc) {
            this.desc = desc;
        }
    }

}
