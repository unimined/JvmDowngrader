package xyz.wagyourtail.jvmdg.javac;

import com.sun.source.util.Plugin;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.main.JavaCompiler;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.cli.Main;
import xyz.wagyourtail.jvmdg.compile.PathDowngrader;
import xyz.wagyourtail.jvmdg.util.Utils;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class JvmdgJavacPlugin implements Plugin, Closeable {
    private BasicJavacTask task;
    private Flags flags;

    @Override
    public String getName() {
        return "jvmdg";
    }

    static {
        int vmVersion = Utils.classVersionToMajorVersion(Utils.getCurrentClassVersion());
        if(vmVersion > 8) {
            try {
                //noinspection JavaReflectionMemberAccess
                Method getModule = Class.class.getDeclaredMethod("getModule");
                openModule(getModule.invoke(JavacTask.class));
            } catch (Throwable t) {
                Utils.sneakyThrow(t);
            }
        }
    }

    @Override
    public void init(JavacTask t, String... args) {
        this.task = (BasicJavacTask) t;

        JavaCompiler compiler = JavaCompiler.instance(task.getContext());
        compiler.closeables = compiler.closeables.prepend(this);

        this.flags = new Flags();

        try {
            Main.parseArgs(args, flags);
        } catch (Throwable e) {
            Utils.sneakyThrow(e);
        }
    }

    @Override
    public void close() throws IOException {
        runDowngrade();
    }

    @SuppressWarnings("UrlHashCode")
    private void runDowngrade() throws IOException {
        final JavaFileManager fileManager = task.getContext().get(JavaFileManager.class);

        File root = new File(
            fileManager.getJavaFileForOutput(
                StandardLocation.CLASS_OUTPUT,
                "", JavaFileObject.Kind.CLASS, null
            ).toUri()
        ).getParentFile();

        Set<URL> classpathURLs = new HashSet<>();

        // the set argument must be mutable, since GradleStandardJavaFileManager calls remove
        for(JavaFileObject jfo : fileManager.list(StandardLocation.CLASS_PATH, "",
            new HashSet<>(Collections.singletonList(JavaFileObject.Kind.CLASS)), true)) {
            classpathURLs.add(jfo.toUri().toURL());
        }

        Path tempOutput = Files.createTempDirectory("downgrade");
        tempOutput.toFile().deleteOnExit();

        try(ClassDowngrader cd = ClassDowngrader.downgradeTo(flags)) {
            cd.logger.debug("classpath: " + classpathURLs);

            PathDowngrader.downgradePaths(
                cd,
                Collections.singletonList(root.toPath()),
                Collections.singletonList(tempOutput),
                classpathURLs
            );
        }

        Files.walk(tempOutput)
            .filter(Files::isRegularFile)
            .forEach(p -> {
                try {
                    Path target = root.toPath().resolve(tempOutput.relativize(p));
                    Files.createDirectories(target.getParent());
                    Files.move(p, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (Throwable t) {
                    Utils.sneakyThrow(t);
                }
            });
    }

    public static void openModule(Object o) throws Throwable {
        Class<?> moduleClass = o.getClass();
        if(!moduleClass.getName().equals("java.lang.Module")) {
            throw new IllegalArgumentException("Not a module: " + o);
        }
        MethodHandle implAddOpens = Utils.getImplLookup().findVirtual(moduleClass, "implAddOpens",
            MethodType.methodType(void.class, String.class));

        @SuppressWarnings("unchecked")
        Set<String> packages = (Set<String>) moduleClass.getDeclaredMethod("getPackages").invoke(o);

        for(String pn : packages) {
            implAddOpens.invoke(o, pn);
        }
    }
}
