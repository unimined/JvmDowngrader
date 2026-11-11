package xyz.wagyourtail.jvmdg.asm.index;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.asm.info.ClassInfo;
import xyz.wagyourtail.jvmdg.asm.info.MethodInfo;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only query surface over the classpath as seen from one input version:
 * every query first remaps the queried type through the provider chain
 * ({@link ClassDowngrader#stubClass} — first provider in the chain to remap
 * wins; a stub-produced name is never re-remapped), then resolves against
 * the {@link ClasspathIndex}.
 * <p>
 * Cheap to construct — it only binds the downgrader, a version, and the
 * warning sink — so callers create one per resolution context rather than
 * sharing a mutable view across calls.
 */
public class NamespaceView {
    private final ClassDowngrader downgrader;
    private final int version;
    private final Set<String> warnings;

    public NamespaceView(ClassDowngrader downgrader, int version, Set<String> warnings) {
        this.downgrader = downgrader;
        this.version = version;
        this.warnings = warnings;
    }

    /** first-hit-wins query remap: the type this namespace actually resolves for {@code type} */
    public Type remapForQuery(Type type) {
        return downgrader.stubClass(version, type, warnings);
    }

    /**
     * All non-private, non-abstract methods declared on {@code type} (or its
     * remapped equivalent). Returns an empty set for bootstrap/jvmdg classes
     * (which are treated as memberless so parentOnly stubs resolve), and null
     * when the class can't be found.
     */
    public Set<MemberNameAndDesc> membersAt(Type type) throws IOException {
        Type stubbed = remapForQuery(type);
        ClasspathIndex index = downgrader.classpathIndex;
        if (!stubbed.equals(type)) {
            ClassInfo info = index.loaderInfo(stubbed.getInternalName());
            if (info == null) throw new IOException("Failed to find stubbed class: " + stubbed);
            Set<MemberNameAndDesc> members = new HashSet<>();
            for (MethodInfo o : info.getMethods()) {
                if ((o.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) != 0) continue;
                members.add(new MemberNameAndDesc(o.name, downgrader.stubClass(info.getVersion(), o.desc, warnings)));
            }
            return members;
        }
        if (ClassDowngrader.bootstrapClassLoader.getResource(type.getInternalName() + ".class") != null) {
            // assume all java api classes are... "empty". this makes parentOnly stubs work properly.
            return new HashSet<>();
        }
        if (type.getInternalName().startsWith("xyz/wagyourtail/jvmdg")) {
            // also all jvmdg classes are "empty"
            return new HashSet<>();
        }
        ClassInfo info = index.loaderInfo(type.getInternalName());
        if (info == null) return null;
        Set<MemberNameAndDesc> members = new HashSet<>();
        for (MethodInfo o : info.getMethods()) {
            if ((o.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) != 0) continue;
            members.add(new MemberNameAndDesc(o.name, downgrader.stubClass(info.getVersion(), o.desc, warnings)));
        }
        return members;
    }

    /**
     * Direct supertype pairs {@code (type, isInterface)} of {@code type} (or
     * its remapped equivalent); null when the class can't be found.
     */
    public List<Pair<Type, Boolean>> supertypesAt(Type type) throws IOException {
        Type stubbed = remapForQuery(type);
        ClasspathIndex index = downgrader.classpathIndex;
        if (!stubbed.equals(type)) {
            ClassInfo info = index.loaderInfo(stubbed.getInternalName());
            if (info == null) throw new IOException("Failed to find stubbed class: " + stubbed);
            List<Pair<Type, Boolean>> types = new ArrayList<>();
            types.add(new Pair<>(Type.getObjectType(info.getSuperName()), Boolean.FALSE));
            for (String anInterface : info.getInterfaces()) {
                types.add(new Pair<>(Type.getObjectType(anInterface), Boolean.TRUE));
            }
            return types;
        }
        ClassInfo info = index.loaderInfo(type.getInternalName());
        if (info == null) return null;
        List<Pair<Type, Boolean>> types = new ArrayList<>();
        int vers = info.getVersion();
        types.add(new Pair<>(downgrader.stubClass(vers, Type.getObjectType(info.getSuperName()), warnings), Boolean.FALSE));
        for (String anInterface : info.getInterfaces()) {
            types.add(new Pair<>(downgrader.stubClass(vers, Type.getObjectType(anInterface), warnings), Boolean.TRUE));
        }
        return types;
    }

    /**
     * Whether {@code type} (or its remapped equivalent) is an interface;
     * null when the class can't be found.
     */
    public Boolean isInterfaceAt(Type type) throws IOException {
        Type stubbed = remapForQuery(type);
        ClasspathIndex index = downgrader.classpathIndex;
        ClassInfo info = index.loaderInfo(stubbed.getInternalName());
        if (info == null) {
            if (!stubbed.equals(type)) {
                throw new IOException("Failed to find stubbed class: " + stubbed);
            }
            return null;
        }
        return info.isInterface();
    }
}
