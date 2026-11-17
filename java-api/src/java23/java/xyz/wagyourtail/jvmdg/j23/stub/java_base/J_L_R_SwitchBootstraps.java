package xyz.wagyourtail.jvmdg.j23.stub.java_base;

import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import xyz.wagyourtail.jvmdg.version.IndyDesugar;
import xyz.wagyourtail.jvmdg.version.ModifyContext;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_L_R_SwitchBootstraps {

    // java 23+ uses common supertype instead of Object, and <23 doesn't cast the method handle in the callsite
    @IndyDesugar(ref = @Ref(value = "java/lang/runtime/SwitchBootstraps", member = "typeSwitch", desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void makeTypeSwitch(ModifyContext ctx) {
        InvokeDynamicInsnNode indy = ctx.indy;
        indy.desc = "(Ljava/lang/Object;I)I";
    }

}
