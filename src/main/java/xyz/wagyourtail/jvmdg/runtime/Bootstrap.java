package xyz.wagyourtail.jvmdg.runtime;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Bootstrap {
    static final Flags flags = new Flags();
    static final ClassDowngrader currentVersionDowngrader = ClassDowngrader.getCurrentVersionDowngrader(flags);
    private static final Logger LOGGER = Logger.getLogger("JVMDowngrader");

    static {
        LOGGER.setLevel(flags.printDebug ? Level.ALL : Level.WARNING);
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
            currentVersionDowngrader.getClassLoader().addDelegate(cp);
            Class.forName(mainClass, false, currentVersionDowngrader.getClassLoader()).getMethod("main", String[].class).invoke(
                    null,
                    (Object) newArgs
            );
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static String sha1(Path p) {
        try (InputStream stream = Files.newInputStream(p)) {
            MessageDigest digestSha1 = MessageDigest.getInstance("SHA-1");
            digestSha1.update(Utils.readAllBytes(stream));
            byte[] hashBytes = digestSha1.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void premain(String args, Instrumentation instrumentation) throws IOException, URISyntaxException, UnmodifiableClassException {
        LOGGER.info("Starting JVMDowngrader Bootstrap in agent mode.");
        // downgrade api
        Path zip = flags.findJavaApi();
        String zipSha = sha1(zip);
        Path tmp = Constants.DIR.toPath().resolve("java-api-downgraded-" + currentVersionDowngrader.target + "-" + zipSha.substring(0, 8) + ".jar");
        boolean downgrade = false;
        if (!Files.exists(tmp)) {
            LOGGER.warning("Downgrading java-api.jar as its hash changed or this is first launch, this may take a minute...");
            downgrade = true;
        }
        if (downgrade) {
            Files.createDirectories(tmp.getParent());
            for (File file : tmp.getParent().toFile().listFiles()) {
                if (file.isDirectory()) continue;
                file.delete();
            }
            Files.createDirectories(tmp.getParent());
            ZipDowngrader.downgradeZip(currentVersionDowngrader, zip, new HashSet<URL>(), tmp);
        }
        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(tmp.toFile()));
        // add self
        URL self = ClassDowngrader.class.getProtectionDomain().getCodeSource().getLocation();
        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(self.toURI().getPath()));
        instrumentation.addTransformer(new ClassDowngradingAgent(), instrumentation.isRetransformClassesSupported());
        if (!instrumentation.isModifiableClass(CodeSource.class) || !instrumentation.isRetransformClassesSupported()) {
            LOGGER.severe("CodeSource is not modifiable, this will prevent loading signed classes properly!!!");
        } else {
            LOGGER.info("CodeSource is modifiable, attempting to retransform it to fix code signing.");
            instrumentation.retransformClasses(CodeSource.class);
        }
        LOGGER.info("JVMDowngrader Bootstrap agent loaded.");
    }

    public static void agentmain(String args, Instrumentation instrumentation) throws URISyntaxException, IOException, UnmodifiableClassException {
        premain(args, instrumentation);
    }
}
