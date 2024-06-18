package xyz.wagyourtail.jvmdg.version;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Coverage {
    private final VersionProvider versionProvider;
    private final int javaVersion;
    private final String path;

    private Map<Type, String> classes;
    private Map<FullyQualifiedMemberNameAndDesc, String> members;

    public Coverage(int inputVersion, VersionProvider versionProvider) {
        this.javaVersion = Utils.classVersionToMajorVersion(inputVersion);
        this.path = "/META-INF/coverage/" + javaVersion+ "/missing.txt";
        this.versionProvider = versionProvider;
    }

    private void readLine(String line, Map<Type, String> classes, Map<FullyQualifiedMemberNameAndDesc, String> members) throws IOException {
        String[] parts = line.split(";", 3);
        if (parts.length != 3) throw new IOException("Invalid line: " + line);
        String mod = parts[0];
        String name = parts[1] + ";";
        if (parts[2].equals(";")) {
            classes.put(Type.getType(name), mod);
        } else {
            String[] nameAndDesc = parts[2].split(";", 2);
            if (nameAndDesc.length != 2) throw new IOException("Invalid line: " + line);
            String desc = nameAndDesc[1].substring(0, nameAndDesc[1].lastIndexOf(";"));
            members.put(new FullyQualifiedMemberNameAndDesc(Type.getType(name), nameAndDesc[0], Type.getType(desc)), mod);
        }
    }

    private synchronized void load() {
        try {
            if (classes != null) return;
            Map<Type, String> classes = new LinkedHashMap<>();
            Map<FullyQualifiedMemberNameAndDesc, String> members = new LinkedHashMap<>();
            try (InputStream is = versionProvider.getClass().getResourceAsStream(path)) {
                if (is != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            readLine(line, classes, members);
                        }
                    }
                }
            }
            this.classes = classes;
            this.members = members;
        } catch (IOException e) {
            Utils.<RuntimeException>sneakyThrow(e);
        }
    }

    @Nullable
    public String checkClass(Type type) {
        load();
        return classes.get(type);
    }

    @Nullable
    public String checkMember(FullyQualifiedMemberNameAndDesc member) {
        load();
        return members.get(member);
    }

    public void warnClass(Type type, Set<String> warnings) {
        String mod = checkClass(type);
        if (mod != null) {
            warnings.add("Class " + type.getClassName() + " from " + mod + " in " + javaVersion + " is not covered by the api stubs");
        }
    }

    public void warnMember(FullyQualifiedMemberNameAndDesc member, Set<String> warnings) {
        String mod = checkMember(member);
        if (mod != null) {
            warnings.add("Member " + member + " from " + mod + " in " + javaVersion + " is not covered by the api stubs");
        }
    }

}
