package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * The mechanical half of a stub pass: rewrites every structural {@code Type}
 * slot in an instruction through a {@link TypeRemap} — member insn
 * owner/desc, indy desc and bootstrap-handle fields, {@code ldc} Type
 * constants, stack-map frame slots, {@code multianewarray}, and
 * {@code TypeInsnNode} element types (including the retro-supertype
 * {@code jvmdg$instanceof}/{@code jvmdg$checkcast} rewrites).
 * <p>
 * Member-insn dispatch ({@link MemberResolver} → {@link MemberStubTransform}),
 * bootstrap-arg rewriting and {@code ConstantDynamic} stay with the caller —
 * those resolve through member registries, not the type remap.
 */
public class StructuralRemapTransform {

    private final TypeRemap remap;

    public StructuralRemapTransform(TypeRemap remap) {
        this.remap = remap;
    }

    /**
     * Remaps the structural Type slots of {@code insn} in place.
     * @return a replacement insn for the {@code TypeInsnNode} retro-supertype
     *         rewrite ({@code instanceof}/{@code checkcast} → invokestatic),
     *         or null when the insn continues as-is.
     */
    public AbstractInsnNode remapInsn(AbstractInsnNode insn, Set<String> warnings) {
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) insn;
            min.owner = remap.remap(Type.getObjectType(min.owner), warnings).getInternalName();
            min.desc = remap.remap(Type.getMethodType(min.desc), warnings).getDescriptor();
        } else if (insn instanceof FieldInsnNode) {
            FieldInsnNode fin = (FieldInsnNode) insn;
            fin.owner = remap.remap(Type.getObjectType(fin.owner), warnings).getInternalName();
            fin.desc = remap.remap(Type.getType(fin.desc), warnings).getDescriptor();
        } else if (insn instanceof TypeInsnNode) {
            return remapTypeInsn((TypeInsnNode) insn, warnings);
        } else if (insn instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
            indy.desc = remap.remap(Type.getMethodType(indy.desc), warnings).getDescriptor();
            indy.bsm = new Handle(
                indy.bsm.getTag(),
                remap.remap(Type.getObjectType(indy.bsm.getOwner()), warnings).getInternalName(),
                indy.bsm.getName(),
                remap.remap(Type.getMethodType(indy.bsm.getDesc()), warnings).getDescriptor(),
                indy.bsm.isInterface()
            );
        } else if (insn instanceof MultiANewArrayInsnNode) {
            MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) insn;
            manain.desc = remap.remap(Type.getType(manain.desc), warnings).getDescriptor();
        } else if (insn instanceof LdcInsnNode) {
            LdcInsnNode ldc = (LdcInsnNode) insn;
            if (ldc.cst instanceof Type) {
                ldc.cst = remap.remap((Type) ldc.cst, warnings);
            }
        } else if (insn instanceof FrameNode) {
            FrameNode fn = (FrameNode) insn;
            if (fn.local != null) {
                for (int j = 0; j < fn.local.size(); j++) {
                    Object o = fn.local.get(j);
                    if (o instanceof String) {
                        fn.local.set(j, remap.remap(Type.getObjectType((String) o), warnings).getInternalName());
                    }
                }
            }
            if (fn.stack != null) {
                for (int j = 0; j < fn.stack.size(); j++) {
                    Object o = fn.stack.get(j);
                    if (o instanceof String) {
                        fn.stack.set(j, remap.remap(Type.getObjectType((String) o), warnings).getInternalName());
                    }
                }
            }
        }
        return null;
    }

    /**
     * TypeInsnNode handling: array/object element remap, and for stubbed
     * object types the {@code jvmdg$instanceof}/{@code jvmdg$checkcast}
     * reflective hook that lets an adapter claim the insn — returning the
     * replacement invokestatic, or null.
     */
    private MethodInsnNode remapTypeInsn(TypeInsnNode insn, Set<String> warnings) {
        Type desc = Type.getObjectType(insn.desc);
        switch (desc.getSort()) {
            case Type.ARRAY:
                Type type = desc.getElementType();
                ClassStubEntry elementStub = remap.stubFor(type);
                if (elementStub != null) {
                    type = elementStub.target;
                }
                remap.warnUnstubbed(type, warnings);
                insn.desc = Type.getType(desc.getDescriptor().substring(0, desc.getDimensions()) + type.getDescriptor()).getInternalName();
                return null;
            case Type.OBJECT:
                ClassStubEntry stub = remap.stubFor(desc);
                if (stub != null) {
                    // baked jvmdg$ hooks from the stub class
                    switch (insn.getOpcode()) {
                        case Opcodes.INSTANCEOF:
                            if (stub.instanceofHook != null) {
                                return new MethodInsnNode(Opcodes.INVOKESTATIC, stub.instanceofHook.getOwner().getInternalName(), stub.instanceofHook.getName(), stub.instanceofHook.getDesc().getDescriptor(), false);
                            }
                            break;
                        case Opcodes.CHECKCAST:
                            if (stub.checkcastHook != null) {
                                return new MethodInsnNode(Opcodes.INVOKESTATIC, stub.checkcastHook.getOwner().getInternalName(), stub.checkcastHook.getName(), stub.checkcastHook.getDesc().getDescriptor(), false);
                            }
                            break;
                        default:
                    }
                    insn.desc = stub.target.getInternalName();
                    return null;
                }
                remap.warnUnstubbed(desc, warnings);
                break;
        }
        return null;
    }
}
