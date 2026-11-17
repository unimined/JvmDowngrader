package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@InitRewrite(ref = @Ref(value = "Ljava/lang/StackTraceElement;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V"), to = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V", keep = {3, 4, 5, 6})
public class J_L_StackTraceElement {

    // module/classloader info carried by the 7-arg constructor has no field to
    // live in on pre-9 runtimes, so it goes through a weak side table keyed on
    // the element instance. keys must be identity-based: StackTraceElement is
    // value-equal, so two distinct elements with equal content would collide
    // in a plain WeakHashMap and leak module info across instances.
    private static final ReferenceQueue<StackTraceElement> QUEUE = new ReferenceQueue<StackTraceElement>();
    private static final Map<ElementKey, String[]> MODULE_INFO =
        Collections.synchronizedMap(new HashMap<ElementKey, String[]>());

    private static final class ElementKey extends WeakReference<StackTraceElement> {
        private final int hash;

        ElementKey(StackTraceElement e) {
            super(e, QUEUE);
            this.hash = System.identityHashCode(e);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof ElementKey)) {
                return false;
            }
            StackTraceElement self = get();
            return self != null && self == ((ElementKey) o).get();
        }
    }

    private static void drain() {
        for (Object k = QUEUE.poll(); k != null; k = QUEUE.poll()) {
            MODULE_INFO.remove(k);
        }
    }

    /**
     * Rewrites the 7-arg constructor call: stores the module/classloader
     * arguments, delegates to the 4-arg constructor, then registers the extra
     * data in {@link #MODULE_INFO} for the getters below.
     */
    // the dropped module/classloader metadata gets side-tabled after construction
    @InitPost(target = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V", from = {0, 1, 2})
    public static void register(StackTraceElement element, String classLoaderName,
                                String moduleName, String moduleVersion) {
        drain();
        MODULE_INFO.put(new ElementKey(element), new String[]{classLoaderName, moduleName, moduleVersion});
    }

    private static String[] info(StackTraceElement element) {
        drain();
        return MODULE_INFO.get(new ElementKey(element));
    }

    @Stub
    public static String getClassLoaderName(StackTraceElement element) {
        String[] info = info(element);
        return info == null ? null : info[0];
    }

    @Stub
    public static String getModuleName(StackTraceElement element) {
        String[] info = info(element);
        return info == null ? null : info[1];
    }

    @Stub
    public static String getModuleVersion(StackTraceElement element) {
        String[] info = info(element);
        return info == null ? null : info[2];
    }

    /**
     * Reproduces the JDK 9+ {@code StackTraceElement.toString} format
     * {@code [classLoaderName/][moduleName[@moduleVersion]/]declaringClass.method(source)}
     * using the side-table module info. Elements without registered info (e.g.
     * produced by the runtime itself) fall through to the pre-9 format, which
     * is identical once the segments are omitted. Cannot delegate to
     * {@code element.toString()} — that call would be rewritten back into this
     * stub when the api jar is downgraded.
     */
    @Stub
    public static String toString(StackTraceElement element) {
        String[] info = info(element);
        StringBuilder sb = new StringBuilder();
        if (info != null) {
            if (info[0] != null && !info[0].isEmpty()) {
                sb.append(info[0]).append('/');
            }
            if (info[1] != null && !info[1].isEmpty()) {
                sb.append(info[1]);
                if (info[2] != null && !info[2].isEmpty()) {
                    sb.append('@').append(info[2]);
                }
                sb.append('/');
            }
        }
        sb.append(element.getClassName()).append('.').append(element.getMethodName()).append('(');
        if (element.isNativeMethod()) {
            sb.append("Native Method");
        } else if (element.getFileName() == null) {
            sb.append("Unknown Source");
        } else {
            sb.append(element.getFileName());
            if (element.getLineNumber() >= 0) {
                sb.append(':').append(element.getLineNumber());
            }
        }
        return sb.append(')').toString();
    }

}
