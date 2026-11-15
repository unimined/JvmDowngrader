package xyz.wagyourtail.jvmdg.version;

import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.scan.AdapterHandler;
import xyz.wagyourtail.jvmdg.version.scan.DefaultImplHandler;
import xyz.wagyourtail.jvmdg.version.scan.InitArgHandler;
import xyz.wagyourtail.jvmdg.version.scan.InitArgsHandler;
import xyz.wagyourtail.jvmdg.version.scan.IndyDesugarHandler;
import xyz.wagyourtail.jvmdg.version.scan.InitFactoryHandler;
import xyz.wagyourtail.jvmdg.version.scan.InitPostHandler;
import xyz.wagyourtail.jvmdg.version.scan.InitPostsHandler;
import xyz.wagyourtail.jvmdg.version.scan.InitPreHandler;
import xyz.wagyourtail.jvmdg.version.scan.InitPresHandler;
import xyz.wagyourtail.jvmdg.version.scan.InitRewriteHandler;
import xyz.wagyourtail.jvmdg.version.scan.InitRewritesHandler;
import xyz.wagyourtail.jvmdg.version.scan.ModifyHandler;
import xyz.wagyourtail.jvmdg.version.scan.ScanContext;
import xyz.wagyourtail.jvmdg.version.scan.AnnotationHandler;
import xyz.wagyourtail.jvmdg.version.scan.StubHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reflective scan that turns one stub-provider class into registrations,
 * dispatching each primary annotation to its {@link AnnotationHandler}:
 * {@code @Adapter} class → {@code classStubs} type remap, {@code @Stub}
 * methods → the stub registry (via the owner's {@code ClassMapping}),
 * {@code @Modify} methods → the modify registry, inner classes scanned
 * recursively. A method carrying more than one primary annotation is a
 * registration error — one annotation = one operation.
 * <p>
 * This is the producer seam for {@code stubs.idx}: once registrations are
 * declarative data ({@link xyz.wagyourtail.jvmdg.version.map.StubEntry}), an
 * index-based scanner feeds the same registry without loading or reflecting
 * on the stub classes.
 */
public class StubScanner {

    /** fixed ordered handler list — internal, not a public SPI */
    private static final List<AnnotationHandler> HANDLERS = Collections.unmodifiableList(
        Arrays.<AnnotationHandler>asList(
            new AdapterHandler(),
            new StubHandler(),
            new DefaultImplHandler(),
            new InitFactoryHandler(),
            new InitRewriteHandler(),
            new InitRewritesHandler(),
            new InitArgHandler(),
            new InitArgsHandler(),
            new InitPreHandler(),
            new InitPresHandler(),
            new InitPostHandler(),
            new InitPostsHandler(),
            new ModifyHandler(),
            new IndyDesugarHandler()
        ));

    private final VersionProvider vp;

    public StubScanner(VersionProvider vp) {
        this.vp = vp;
    }

    public void scan(Class<?> clazz) {
        Set<String> warnings = new LinkedHashSet<>();
        // registration failures are collected and reported once per stub class
        // instead of one warn+stacktrace per member
        List<String> failures = new ArrayList<>();
        try {
            FullyQualifiedMemberNameAndDesc fqm = FullyQualifiedMemberNameAndDesc.of(clazz);
            JEP jep = clazz.getAnnotation(JEP.class);
            if (vp.downgrader.flags.debugSkipStub.contains(fqm) || (jep != null && vp.downgrader.flags.debugSkipJeps.contains(jep.value()))) {
                return;
            }
            ScanContext ctx = new ScanContext(vp, clazz, warnings, failures);
            for (AnnotationHandler handler : HANDLERS) {
                Annotation a = clazz.getAnnotation(handler.annotation());
                if (a != null) {
                    try {
                        handler.onClass(clazz, a, ctx);
                    } catch (Throwable e) {
                        failures.add("class-level " + a.annotationType().getSimpleName() + ": " + e);
                    }
                }
            }
            try {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (vp.downgrader.flags.debugSkipStub.contains(FullyQualifiedMemberNameAndDesc.of(method))) continue;
                    jep = method.getAnnotation(JEP.class);
                    if (jep != null && vp.downgrader.flags.debugSkipJeps.contains(jep.value())) continue;
                    try {
                        List<Annotation> present = new ArrayList<>(HANDLERS.size());
                        for (AnnotationHandler handler : HANDLERS) {
                            Annotation a = method.getAnnotation(handler.annotation());
                            if (a != null) {
                                present.add(a);
                            }
                        }
                        if (present.size() > 1) {
                            StringBuilder names = new StringBuilder();
                            for (Annotation a : present) {
                                names.append('@').append(a.annotationType().getSimpleName()).append(' ');
                            }
                            throw new IllegalArgumentException("multiple primary stub annotations on one method: " + names);
                        }
                        if (!present.isEmpty() &&
                            (method.isAnnotationPresent(Internal.class) ||
                                method.isAnnotationPresent(CoverageIgnore.class))) {
                            throw new IllegalArgumentException(
                                "@Internal/@CoverageIgnore is not valid on a stub-annotated member — stub entries are classified by StubKind");
                        }
                        if (!present.isEmpty()) {
                            Annotation a = present.get(0);
                            for (AnnotationHandler handler : HANDLERS) {
                                if (handler.annotation().isInstance(a)) {
                                    handler.onMethod(method, a, ctx);
                                    break;
                                }
                            }
                        }
                    } catch (Throwable e) {
                        failures.add(method.getName() + ": " + e);
                    }
                }
            } catch (Throwable e) {
                failures.add("resolving methods: " + e);
            }
            for (AnnotationHandler handler : HANDLERS) {
                try {
                    handler.finishClass(ctx);
                } catch (Throwable e) {
                    failures.add(handler.annotation().getSimpleName() + " finishClass: " + e);
                }
            }
            try {
                // inner classes
                for (Class<?> inner : clazz.getDeclaredClasses()) {
                    scan(inner);
                }
            } catch (Throwable e) {
                failures.add("resolving inner classes: " + e);
            }
        } catch (Throwable e) {
            failures.add("creating stub: " + e);
        }
        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String failure : failures) {
                sb.append("\n    ").append(failure);
            }
            vp.logger.error(failures.size() + " stub registration failure(s) in " + clazz.getName() + sb);
        }
        if (!warnings.isEmpty() && vp.logger.is(Logger.Level.WARN)) {
            if (vp.downgrader.flags.checkInIgnoreWarnings(clazz.getName())) return;
            StringBuilder sb = new StringBuilder();
            for (String warning : warnings) {
                sb.append("    ").append(warning).append("\n");
            }
            vp.logger.warn("Warnings for " + clazz.getName() + " (" + warnings.size() + ") : \n" + sb);
        }
    }
}
