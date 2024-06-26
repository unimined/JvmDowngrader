package jnitest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;

public class JniFunctions {
    static {
        try {
            System.loadLibrary("jvmdg");
        } catch (UnsatisfiedLinkError ex) {
            String libName = "jni-greeter";
            URL url = JniFunctions.class.getClassLoader().getResource(libFilename(libName));
            try {
                File file = Files.createTempFile("jvmdg", "jni").toFile();
                file.deleteOnExit();
                file.delete();
                try (InputStream in = url.openStream()) {
                    Files.copy(in, file.toPath());
                }
                System.load(file.getCanonicalPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String libFilename(String libName) {
        return "libjvmdg.so";
    }

    public static native MethodHandles.Lookup getImplLookup();

    public static void main(String[] args) {
        MethodHandles.Lookup test = getImplLookup();
        System.out.println(test);
    }

}