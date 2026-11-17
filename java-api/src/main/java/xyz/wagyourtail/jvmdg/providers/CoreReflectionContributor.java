package xyz.wagyourtail.jvmdg.providers;

import xyz.wagyourtail.jvmdg.stub.all.DispatchRuntimeDesugar;
import xyz.wagyourtail.jvmdg.stub.all.GainedRuntimeDesugar;
import xyz.wagyourtail.jvmdg.stub.all.stub.J_L_Class;
import xyz.wagyourtail.jvmdg.stub.all.stub.J_L_I_MethodHandles$Lookup;
import xyz.wagyourtail.jvmdg.stub.all.stub.J_L_R_Constructor;
import xyz.wagyourtail.jvmdg.stub.all.stub.J_L_R_Method;
import xyz.wagyourtail.jvmdg.version.ReflectionContributor;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

/**
 * The api jar's reflection-stub surface: {@code Class} caller-sensitive
 * lookups + interface filtering, {@code Lookup.find*} /
 * {@code unreflect*} handle queries, and the {@code Method}/
 * {@code Constructor} view stubs — registered through a
 * {@link ReflectionContributor} so the declaration lives in one place
 * ({@code META-INF/services}) instead of a provider-inherited
 * {@code preInit} body. {@link DispatchRuntimeDesugar} and
 * {@link GainedRuntimeDesugar} are transform-only
 * (ASM-bearing) — scanned for their {@code @IndyDesugar}, never loaded at
 * runtime.
 */
public class CoreReflectionContributor implements ReflectionContributor {

    @Override
    public void contribute(VersionProvider provider) {
        provider.stub(J_L_Class.class);
        provider.stub(J_L_I_MethodHandles$Lookup.class);
        provider.stub(J_L_R_Method.class);
        provider.stub(J_L_R_Constructor.class);
        provider.stub(DispatchRuntimeDesugar.class);
        provider.stub(GainedRuntimeDesugar.class);
    }
}
