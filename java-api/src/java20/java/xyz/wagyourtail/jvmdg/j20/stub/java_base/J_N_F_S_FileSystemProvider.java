package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

public class J_N_F_S_FileSystemProvider {

    private static boolean followLinks(LinkOption... options) {
        boolean followLinks = true;
        for (LinkOption opt : options) {
            if (opt == LinkOption.NOFOLLOW_LINKS) {
                followLinks = false;
                continue;
            }
            if (opt == null)
                throw new NullPointerException();
            throw new AssertionError("Should not get here");
        }
        return followLinks;
    }

    @Stub
    public static boolean exists(FileSystemProvider provider, Path path, LinkOption... options) {
        try {
            if (followLinks(options)) {
                provider.checkAccess(path);
            } else {
                provider.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    @Stub
    public static <A extends BasicFileAttributes> A readAttributesIfExists(FileSystemProvider provider, Path path, Class<A> type, LinkOption... options) throws IOException {
        try {
            return provider.readAttributes(path, type, options);
        } catch (NoSuchFileException ignored) {
            return null;
        }
    }

}
