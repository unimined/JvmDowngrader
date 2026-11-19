package xyz.wagyourtail.jvmdg.stub.all;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.version.IndyDesugar;
import xyz.wagyourtail.jvmdg.version.ModifyContext;
import xyz.wagyourtail.jvmdg.version.Ref;

/**
 * Transform-time carrier for the {@code DispatchRuntime.bootstrap}
 * {@code @IndyDesugar}. Like the other desugar carriers
 * ({@code J_L_I_LambdaMetafactory}, {@code J_L_R_SwitchBootstraps}), this
 * class is never loaded at runtime — the handler body manipulates ASM tree
 * types that don't exist on target JVMs — which is why it is NOT hosted on a
 * runtime stub class: a stub impl owner must stay ASM-free or verification
 * (and {@code getDeclaredMethods}-signature resolution) poisons the whole
 * class.
 */
public class DispatchRuntimeDesugar {

    /**
     * {@code DispatchRuntime.bootstrap} under {@code --noIndy}: lowers the
     * dispatch indy to a direct {@code DispatchRuntime.dispatchStub} call —
     * {@code member "name desc"}, the impl-owner class + {@code "name desc"}
     * sig, the receiver and its args packed {@code self/Object[]} — so the
     * generic {@code jvmdg$indy$invoke$N} lowering never adds a synthetic
     * frame inside the transformed class where stack-sensitive callers
     * ({@code StackWalker}, exceptions) would see it. Registered
     * indy-site-wide like every desugar but gated on
     * {@code flags.noIndy}: invokedynamic IS the dispatch — desugaring it
     * under normal output would erase the receiver-class PIC the whole
     * design hangs on.
     */
    @IndyDesugar(ref = @Ref(value = "xyz/wagyourtail/jvmdg/stub/runtime/DispatchRuntime",
        member = "bootstrap",
        desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;" +
            "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;" +
            "Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/invoke/CallSite;"))
    public static void desugarDispatch(ModifyContext ctx) {
        if (!ctx.flags.noIndy) {
            return;
        }
        MethodNode mnode = ctx.method;
        ClassNode cnode = ctx.node;
        InvokeDynamicInsnNode indy = ctx.indy;
        Type[] args = Type.getArgumentTypes(indy.desc);
        InsnList out = new InsnList();
        // park the receiver + args in fresh locals so the call's operands
        // can push in signature order
        int local = mnode.maxLocals;
        int[] slots = new int[args.length];
        for (int i = 0; i < args.length; i++) {
            slots[i] = local;
            local += args[i].getSize();
        }
        mnode.maxLocals = local;
        for (int i = args.length - 1; i >= 0; i--) {
            out.add(new VarInsnNode(args[i].getOpcode(Opcodes.ISTORE), slots[i]));
        }
        // member key "name desc" — indy name + the member MethodType arg
        out.add(new LdcInsnNode(indy.name + " " + ((Type) indy.bsmArgs[0]).getDescriptor()));
        // impl owner class + impl "name desc" — the bsmArg pair, verbatim
        out.add(new LdcInsnNode(indy.bsmArgs[1]));
        out.add(new LdcInsnNode(indy.bsmArgs[2]));
        out.add(new VarInsnNode(Opcodes.ALOAD, slots[0]));
        // pack args (member args + trailing transform extras) into Object[]
        out.add(new LdcInsnNode(args.length - 1));
        out.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        for (int i = 1; i < args.length; i++) {
            out.add(new InsnNode(Opcodes.DUP));
            out.add(new LdcInsnNode(i - 1));
            out.add(new VarInsnNode(args[i].getOpcode(Opcodes.ILOAD), slots[i]));
            Handle box = ASMUtils.boxType(args[i]);
            if (box != null) {
                out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, box.getOwner(), box.getName(),
                    box.getDesc(), false));
            }
            out.add(new InsnNode(Opcodes.AASTORE));
        }
        out.add(new LdcInsnNode(Type.getObjectType(cnode.name)));
        out.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader",
            "()Ljava/lang/ClassLoader;", false));
        out.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "xyz/wagyourtail/jvmdg/stub/runtime/DispatchRuntime", "dispatchStub",
            "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Object;" +
                "[Ljava/lang/Object;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false));
        Type ret = Type.getReturnType(indy.desc);
        if (ret.getSort() == Type.VOID) {
            out.add(new InsnNode(Opcodes.POP));
        } else if (ret.getSort() == Type.OBJECT || ret.getSort() == Type.ARRAY) {
            out.add(new TypeInsnNode(Opcodes.CHECKCAST, ret.getInternalName()));
        } else {
            // primitive result arrives boxed
            Type boxed = ASMUtils.getBoxFor(ret);
            out.add(new TypeInsnNode(Opcodes.CHECKCAST, boxed.getInternalName()));
            Handle unbox = ASMUtils.unboxType(boxed);
            out.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, unbox.getOwner(), unbox.getName(),
                unbox.getDesc(), false));
        }
        mnode.instructions.insert(indy, out);
        mnode.instructions.remove(indy);
    }
}
