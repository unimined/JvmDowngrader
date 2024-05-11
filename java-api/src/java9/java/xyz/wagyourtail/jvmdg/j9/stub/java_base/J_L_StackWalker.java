package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Adapter("java/lang/StackWalker")
public class J_L_StackWalker {

    static final EnumSet<Option> DEFAULT_EMPTY = EnumSet.noneOf(Option.class);
    private static final J_L_StackWalker DEFAULT = new J_L_StackWalker(DEFAULT_EMPTY);
    private final EnumSet<Option> options;
    private final boolean retainClassRef;

    private J_L_StackWalker(EnumSet<Option> options) {
        this.options = options;
        this.retainClassRef = options.contains(Option.RETAIN_CLASS_REFERENCE);
    }

    public static J_L_StackWalker getInstance() {
        return DEFAULT;
    }

    public static J_L_StackWalker getInstance(Option option) {
        return new J_L_StackWalker(EnumSet.of(option));
    }

    public static J_L_StackWalker getInstance(Set<Option> options) {
        return new J_L_StackWalker(EnumSet.copyOf(options));
    }

    private static boolean isReflectionFrame(String className) {
        return className.equals(Method.class.getName()) ||
            className.equals(Constructor.class.getName()) ||
            className.startsWith("sun.reflect.") ||
            className.startsWith("jdk.internal.reflect.") ||
            className.startsWith("java.lang.invoke.LambdaForm");
    }

    public <T> T walk(Function<? super Stream<StackFrame>, ? extends T> function) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        Class<?>[] classRef = SecurityManagerAdapter.INSTANCE.getClassContext();
        boolean showHidden = options.contains(Option.SHOW_HIDDEN_FRAMES);
        boolean showReflect = options.contains(Option.SHOW_REFLECT_FRAMES) || showHidden;
//        System.out.println("TRACE " + stackTrace.length);
//        for (int i = 0; i < stackTrace.length; i++) {
//            System.out.println("  " + stackTrace[i]);
//        }
//        System.out.println("CLASSREF " + classRef.length);
//        for (int i = 0; i < classRef.length; i++) {
//            System.out.println("  " + classRef[i]);
//        }
//        System.out.println("DONE");
        AtomicInteger classRefOffset = new AtomicInteger();
        Stream<StackFrame> stackStream = IntStream.range(2, stackTrace.length).mapToObj(i -> {
            StackFrameImpl frame = new StackFrameImpl();
            frame.element = stackTrace[i];
            if (isReflectionFrame(stackTrace[i].getClassName())) {
                classRefOffset.addAndGet(-1);
            }
            frame.classRef = classRef[i + classRefOffset.get()];
            return (StackFrame) frame;
        }).filter(e -> {
            if (showHidden) {
                return true;
            } else {
                if (e.getClassName().equals("xyz.wagyourtail.jvmdg.runtime.Bootstrap")) {
                    return false;
                }
            }
            if (showReflect) {
                return true;
            }
            return !isReflectionFrame(e.getClassName());
        }).peek(e -> {
            if (!retainClassRef) {
                ((StackFrameImpl) e).classRef = null;
            }
        });
        return function.apply(stackStream);
    }

    public void forEach(Consumer<? super StackFrame> action) {
        walk(stream -> {
            stream.skip(1).forEach(action);
            return null;
        });
    }

    public Class<?> getCallerClass() {
        return walk(stream -> stream.skip(2).findFirst().get().getDeclaringClass());
    }

    @Adapter("java/lang/StackWalker$Option")
    public enum Option {
        RETAIN_CLASS_REFERENCE,
        SHOW_REFLECT_FRAMES,
        SHOW_HIDDEN_FRAMES
    }

    @Adapter("java/lang/StackWalker$StackFrame")
    public interface StackFrame {
        String getClassName();

        String getMethodName();

        Class<?> getDeclaringClass();

        int getByteCodeIndex();

        String getFileName();

        int getLineNumber();

        boolean isNativeMethod();

        StackTraceElement toStackTraceElement();
    }

    static class StackFrameImpl implements StackFrame {
        StackTraceElement element;
        Class<?> classRef;


        @Override
        public String getClassName() {
            return element.getClassName();
        }

        @Override
        public String getMethodName() {
            return element.getMethodName();
        }

        @Override
        public Class<?> getDeclaringClass() {
            if (classRef != null) {
                return classRef;
            }
            throw new UnsupportedOperationException("This stack walker does not have RETAIN_CLASS_REFERENCE access");
        }

        @Override
        public int getByteCodeIndex() {
            return isNativeMethod() ? -1 : 0;
        }

        @Override
        public String getFileName() {
            return element.getFileName();
        }

        @Override
        public int getLineNumber() {
            return element.getLineNumber();
        }

        @Override
        public boolean isNativeMethod() {
            return element.isNativeMethod();
        }

        @Override
        public StackTraceElement toStackTraceElement() {
            return element;
        }

        @Override
        public String toString() {
            return element.toString();
        }
    }

    @SuppressWarnings("removal")
    static class SecurityManagerAdapter extends SecurityManager {
        static final SecurityManagerAdapter INSTANCE = new SecurityManagerAdapter();

        @Override
        public Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }
}
