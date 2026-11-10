package jvmdg.stubtests.manual;

import jvmdg.stubtests.StubTest;
import jvmdg.stubtests.Support;
import xyz.wagyourtail.jvmdg.version.Ref;

/**
 * Parity tests for members denied as environment-dependent
 * ({@code System.getProperties}, {@code Class.getMethods}), ambient-state
 * ({@code LogRecord.getLongThreadID}), non-{@code java.*} owners
 * ({@code com.sun.net.httpserver.Filter}), and [modify] ctors on abstract
 * owners — those need a user-defined subclass whose {@code super(...)} call
 * is the rewritten invokespecial, a shape no single-expression site can emit.
 */
public final class MiscTests {

    private MiscTests() {
    }

    /** getLongThreadID is denied on synthesized receivers (ambient thread id);
     * an explicit setLongThreadID first makes the read-back deterministic. */
    @StubTest(member = @Ref(value = "java/util/logging/LogRecord",
        member = "getLongThreadID", desc = "()J"))
    public static void logRecordThreadId(String id) {
        java.util.logging.LogRecord r =
            new java.util.logging.LogRecord(java.util.logging.Level.INFO, "msg");
        r.setLongThreadID(42L);
        Support.print(id, r.getLongThreadID());
    }

    /** getProperties is denied (env contents); presence of java.home is stable. */
    @StubTest(member = @Ref(value = "java/lang/System", member = "getProperties",
        desc = "()Ljava/util/Properties;"))
    public static void getProperties(String id) {
        Support.print(id, System.getProperties().getProperty("java.home") != null);
    }

    /** getMethods order is unspecified; a membership check is stable. */
    @StubTest(member = @Ref(value = "java/lang/Class", member = "getMethods",
        desc = "()[Ljava/lang/reflect/Method;"))
    public static void getMethods(String id) {
        java.lang.reflect.Method[] ms = String.class.getMethods();
        boolean found = false;
        for (java.lang.reflect.Method m : ms) {
            if (m.getName().equals("length")) {
                found = true;
            }
        }
        Support.print(id, found);
    }

    /** Filter.beforeHandler is denied via the com/sun owner blanket; the static
     * factory needs no server, just a non-null Filter result. */
    @StubTest(member = @Ref(value = "com/sun/net/httpserver/Filter",
        member = "beforeHandler",
        desc = "(Ljava/lang/String;Ljava/util/function/Consumer;)"
            + "Lcom/sun/net/httpserver/Filter;"))
    public static void beforeHandler(String id) {
        com.sun.net.httpserver.Filter f =
            com.sun.net.httpserver.Filter.beforeHandler("desc", e -> {
            });
        Support.print(id, f != null);
    }

    /** Filter.afterHandler — same static-factory reasoning. */
    @StubTest(member = @Ref(value = "com/sun/net/httpserver/Filter",
        member = "afterHandler",
        desc = "(Ljava/lang/String;Ljava/util/function/Consumer;)"
            + "Lcom/sun/net/httpserver/Filter;"))
    public static void afterHandler(String id) {
        com.sun.net.httpserver.Filter f =
            com.sun.net.httpserver.Filter.afterHandler("desc", e -> {
            });
        Support.print(id, f != null);
    }

    /** [modify] ctor on an abstract owner is only reached through a subclass's
     * super(String, Throwable) invokespecial — needs a class definition, not
     * an expression. */
    private static class MyOse extends java.io.ObjectStreamException {
        MyOse(String s, Throwable t) {
            super(s, t);
        }
    }

    @StubTest(member = @Ref(value = "java/io/ObjectStreamException",
        member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V;"))
    public static void objectStreamExceptionCtor(String id) {
        MyOse e = new MyOse("ose", null);
        Support.print(id, e.getMessage());
    }

    /** Same for the Throwable-only overload. */
    private static class MyOseCause extends java.io.ObjectStreamException {
        MyOseCause(Throwable t) {
            super(t);
        }
    }

    @StubTest(member = @Ref(value = "java/io/ObjectStreamException",
        member = "<init>", desc = "(Ljava/lang/Throwable;)V;"))
    public static void objectStreamExceptionCauseCtor(String id) {
        MyOseCause e = new MyOseCause(new IllegalStateException("cause"));
        Support.print(id, e.getCause() != null);
    }

    /** VirtualMachineError.<init>(String, Throwable) — the j9-added ctor is
     * [modify]-rewritten; only a subclass super() reaches it. */
    private static class MyVme extends VirtualMachineError {
        MyVme(String s, Throwable t) {
            super(s, t);
        }
    }

    @StubTest(member = @Ref(value = "java/lang/VirtualMachineError",
        member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    public static void vmErrorCtor(String id) {
        MyVme e = new MyVme("vme", new IllegalStateException("c"));
        Support.print(id, e.getMessage());
    }

    /** VirtualMachineError.<init>(Throwable) — same super-call coverage. */
    private static class MyVmeCause extends VirtualMachineError {
        MyVmeCause(Throwable t) {
            super(t);
        }
    }

    @StubTest(member = @Ref(value = "java/lang/VirtualMachineError",
        member = "<init>", desc = "(Ljava/lang/Throwable;)V"))
    public static void vmErrorCauseCtor(String id) {
        MyVmeCause e = new MyVmeCause(new IllegalStateException("c"));
        Support.print(id, e.getCause() != null);
    }

    /** ClassLoader.<init>(String, ClassLoader) — the j9 named-loader ctor is
     * [modify]-rewritten; only a subclass super() reaches it. */
    private static class MyLoader extends ClassLoader {
        MyLoader(String name, ClassLoader parent) {
            super(name, parent);
        }
    }

    @StubTest(member = @Ref(value = "java/lang/ClassLoader", member = "<init>",
        desc = "(Ljava/lang/String;Ljava/lang/ClassLoader;)V"))
    public static void classLoaderCtor(String id) {
        MyLoader cl = new MyLoader("named", MiscTests.class.getClassLoader());
        Support.print(id, cl.getName());
    }
}
