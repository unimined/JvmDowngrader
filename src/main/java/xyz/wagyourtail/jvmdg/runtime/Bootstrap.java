package xyz.wagyourtail.jvmdg.runtime;

import xyz.wagyourtail.jvmdg.ClassDowngrader;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Bootstrap {
    private static final Logger LOGGER = Logger.getLogger("JVMDowngrader");

    static {
        LOGGER.setLevel(Boolean.parseBoolean(System.getProperty("jvmdg.log", "true")) ? Level.ALL : Level.OFF);
    }

    public static void main(String[] args) {
        LOGGER.info("Starting JVMDowngrader Bootstrap in standalone mode.");
        try {
            String[] newArgs = new String[args.length - 2];
            String[] classpath = args[0].split(File.pathSeparator);
            String mainClass = args[1];
            System.arraycopy(args, 2, newArgs, 0, args.length - 2);
            URL[] cp = new URL[classpath.length];
            for (int i = 0; i < classpath.length; i++) {
                cp[i] = new File(classpath[i]).toURI().toURL();
            }
            ClassDowngrader.classLoader.addDelegate(cp);
            Class.forName(mainClass, false, ClassDowngrader.classLoader).getMethod("main", String[].class).invoke(
                null,
                (Object) newArgs
            );
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void premain(String args, Instrumentation instrumentation) {
        LOGGER.info("Starting JVMDowngrader Bootstrap in agent mode.");
        instrumentation.addTransformer(new ClassDowngradingAgent());
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        premain(args, instrumentation);
    }
}
