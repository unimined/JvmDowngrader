package jvmdg.stubtests.manual;

import jvmdg.stubtests.StubTest;
import jvmdg.stubtests.Support;

/** Parity tests for module-descriptor members with no reachable receiver. */
public final class ModuleTests {

    private ModuleTests() {
    }

    /** Exports instances only exist inside a built ModuleDescriptor. */
    @StubTest(member = "java/lang/module/ModuleDescriptor$Exports.accessFlags()Ljava/util/Set;")
    public static void exportsAccessFlags(String id) {
        try {
            java.lang.module.ModuleDescriptor.Exports e =
                java.lang.module.ModuleDescriptor.newModule("m").exports("p").build()
                    .exports().iterator().next();
            Support.print(id, e.accessFlags());
        } catch (Throwable t) {
            Support.exc(id, t);
        }
    }

    /** The implicit java.base Requires is always present on a built module. */
    @StubTest(member = "java/lang/module/ModuleDescriptor$Requires.accessFlags()Ljava/util/Set;")
    public static void requiresAccessFlags(String id) {
        try {
            java.lang.module.ModuleDescriptor.Requires r =
                java.lang.module.ModuleDescriptor.newModule("m").build()
                    .requires().iterator().next();
            Support.print(id, r.accessFlags());
        } catch (Throwable t) {
            Support.exc(id, t);
        }
    }
}
