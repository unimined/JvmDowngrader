package xyz.wagyourtail.jvmdg.j22.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.file.Path;

public class J_N_F_Path {

    @Stub
    public static Path resolve(Path self, String first, String... more) {
        Path result = self.resolve(first);
        for (String path : more) {
            result = result.resolve(path);
        }
        return result;
    }

    @Stub
    public static Path resolve(Path self, Path first, Path... more) {
        Path result = self.resolve(first);
        for (Path path : more) {
            result = result.resolve(path);
        }
        return result;
    }

}
