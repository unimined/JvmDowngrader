package xyz.wagyourtail.jvmdg.standalone;

import java.io.File;
import java.net.URL;

public class Bootstrap {
    public static void main(String[] args) {
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
}
