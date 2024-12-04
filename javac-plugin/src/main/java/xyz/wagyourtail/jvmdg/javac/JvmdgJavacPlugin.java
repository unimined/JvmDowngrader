package xyz.wagyourtail.jvmdg.javac;

import com.sun.source.util.Plugin;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.main.JavaCompiler;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.cli.Arguments;
import xyz.wagyourtail.jvmdg.cli.Main;
import xyz.wagyourtail.jvmdg.compile.PathDowngrader;
import xyz.wagyourtail.jvmdg.util.Lazy;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

public class JvmdgJavacPlugin extends Main implements Plugin, Closeable {
    private BasicJavacTask task;
    private String[] args;

    private final Lazy<JavaFileManager> fileManager = new Lazy<JavaFileManager>() {
        @Override
        protected JavaFileManager init() {
            return task.getContext().get(JavaFileManager.class);
        }
    };

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
    protected Arguments buildArgumentList() {
        Arguments args = super.buildArgumentList();
        Arguments downgrade = args.getChild("downgrade");
        Arguments target = downgrade.getChild("--target");
        // remove --target
        downgrade.removeChild(target);
        downgrade.removeChild(downgrade.getChild("--classpath"));
        args.getChild("shade").removeChild(target);
        args.removeChild(args.getChild("bootstrap"));
        return args;
    }

    @Override
    public void getTargets(Map<String, List<String[]>> args, Map<Path, Path> targets, List<FileSystem> fileSystems) throws IOException {
        if (!Constants.DIR.exists() && !Constants.DIR.mkdirs()) {
            throw new IOException("Failed to create directory: " + Constants.DIR);
        }
        Path files = Files.createTempDirectory(Constants.DIR.toPath(), "downgrade").toAbsolutePath();
        System.out.println(files);
        files.toFile().deleteOnExit();

        targets.put(tempFiles.poll().toPath(), files);
        tempFiles.push(files.toFile());
    }

    @Override
    public void init(JavacTask t, String... args) {
        this.task = (BasicJavacTask) t;

        JavaCompiler compiler = JavaCompiler.instance(task.getContext());
        compiler.closeables = compiler.closeables.prepend(this);

        this.args = args;
    }

    @Override
    public void close() throws IOException {
        execute();
    }

    @Override
    @SuppressWarnings("UrlHashCode")
    public Set<URL> getClasspath(Map<String, List<String[]>> args) throws MalformedURLException {
        Set<URL> classpathURLs = new HashSet<>();

        try {
            // the set argument must be mutable, since GradleStandardJavaFileManager calls remove
            for (JavaFileObject jfo : fileManager.get().list(StandardLocation.CLASS_PATH, "",
                new HashSet<>(Collections.singletonList(JavaFileObject.Kind.CLASS)), true)) {
                classpathURLs.add(jfo.toUri().toURL());
            }
        } catch (IOException e) {
            Utils.sneakyThrow(e);
        }

        return classpathURLs;
    }

    private void execute() throws IOException {
        File root = new File(
            fileManager.get().getJavaFileForOutput(
                StandardLocation.CLASS_OUTPUT,
                "", JavaFileObject.Kind.CLASS, null
            ).toUri()
        ).getParentFile();

        tempFiles.add(root);

        try {
            parseArgs(args);
        } catch (Exception e) {
            Utils.sneakyThrow(e);
        }

        File output = tempFiles.pollFirst();

        assert output != null;
        if (output.equals(root)) return;

        Stream<Path> walk = Files.walk(output.toPath());
            walk.filter(Files::isRegularFile)
                .forEach(p -> {
                    try {
                        Path target = root.toPath().resolve(output.toPath().relativize(p));
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
