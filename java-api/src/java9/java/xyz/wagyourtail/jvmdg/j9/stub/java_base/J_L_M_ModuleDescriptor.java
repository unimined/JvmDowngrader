package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.jvmdg.j9.intl.NameChecks;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Adapter("java/lang/module/ModuleDescriptor")
public class J_L_M_ModuleDescriptor implements Comparable<J_L_M_ModuleDescriptor> {
    private final String name;
    private final Version version;
    private final String rawVersion;
    private final EnumSet<Modifier> modifiers;
    private final Set<Requires> requiresSet;
    private final Set<Exports> exportsSet;
    private final Set<Opens> opensSet;
    private final Set<String> uses;
    private final Set<Provides> provides;
    private final Set<String> packages;
    private final String mainClass;

    private J_L_M_ModuleDescriptor(
            String name,
            Version version,
            String rawVersion,
            Set<Modifier> modifiers,
            Set<Requires> requiresSet,
            Set<Exports> exportsSet,
            Set<Opens> opensSet,
            Set<String> uses,
            Set<Provides> provides,
            Set<String> packages,
            String mainClass
    ) {
        this.name = name;
        this.version = version;
        this.rawVersion = rawVersion;
        this.modifiers = EnumSet.copyOf(modifiers);
        this.requiresSet = Collections.unmodifiableSet(requiresSet);
        this.exportsSet = Collections.unmodifiableSet(exportsSet);
        this.opensSet = Collections.unmodifiableSet(opensSet);
        this.uses = Collections.unmodifiableSet(uses);
        this.provides = Collections.unmodifiableSet(provides);
        this.packages = Collections.unmodifiableSet(packages);
        this.mainClass = mainClass;
    }

    public String name() {
        return name;
    }

    public Set<Modifier> modifiers() {
        return Collections.unmodifiableSet(modifiers);
    }

    public boolean isOpen() {
        return modifiers.contains(Modifier.OPEN);
    }

    public boolean isAutomatic() {
        return modifiers.contains(Modifier.AUTOMATIC);
    }

    public Set<Requires> requires() {
        return requiresSet;
    }

    public Set<Exports> exports() {
        return exportsSet;
    }

    public Set<Opens> opens() {
        return opensSet;
    }

    public Set<String> uses() {
        return uses;
    }

    public Set<Provides> provides() {
        return provides;
    }

    public Optional<Version> version() {
        return Optional.ofNullable(version);
    }

    public Optional<String> rawVersion() {
        if (version != null) {
            return Optional.of(version.toString());
        } else {
            return Optional.ofNullable(rawVersion);
        }
    }

    public String toNameAndVersion() {
        return name + (version != null ? "@" + version : "");
    }

    public Optional<String> mainClass() {
        return Optional.ofNullable(mainClass);
    }

    public Set<String> packages() {
        return packages;
    }

    @Override
    public int compareTo(@NotNull J_L_M_ModuleDescriptor o) {
        if (this == o) return 0;
        int i = name.compareTo(o.name);
        if (i != 0) return i;

        i = compare(version, o.version);
        if (i != 0) return i;

        i = compare(rawVersion, o.rawVersion);
        if (i != 0) return i;

        i = Long.compare(longHash(modifiers), longHash(o.modifiers));
        if (i != 0) return i;

        i = compare(requiresSet, o.requiresSet);
        if (i != 0) return i;

        i = compare(packages, o.packages);
        if (i != 0) return i;

        i = compare(exportsSet, o.exportsSet);
        if (i != 0) return i;

        i = compare(opensSet, o.opensSet);
        if (i != 0) return i;

        i = compare(uses, o.uses);
        if (i != 0) return i;

        i = compare(provides, o.provides);
        if (i != 0) return i;

        return compare(mainClass, o.mainClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof J_L_M_ModuleDescriptor)) return false;
        J_L_M_ModuleDescriptor that = (J_L_M_ModuleDescriptor) o;
        return Objects.equals(name, that.name) && Objects.equals(version, that.version) && Objects.equals(rawVersion, that.rawVersion) && Objects.equals(modifiers, that.modifiers) && Objects.equals(requiresSet, that.requiresSet) && Objects.equals(exportsSet, that.exportsSet) && Objects.equals(opensSet, that.opensSet) && Objects.equals(uses, that.uses) && Objects.equals(provides, that.provides) && Objects.equals(packages, that.packages) && Objects.equals(mainClass, that.mainClass);
    }

    private transient int hash = 0;

    @Override
    public int hashCode() {
        if (hash != 0) {
            return hash;
        }
        int hash = name.hashCode() * 43;
        hash = hash * 43 + modifiers.hashCode();
        hash = hash * 43 + requiresSet.hashCode();
        hash = hash * 43 + packages.hashCode();
        hash = hash * 43 + exportsSet.hashCode();
        hash = hash * 43 + opensSet.hashCode();
        hash = hash * 43 + uses.hashCode();
        hash = hash * 43 + provides.hashCode();
        if (version != null) {
            hash = hash * 43 + version.hashCode();
        } else {
            hash *= 43;
        }
        if (rawVersion != null) {
            hash = hash * 43 + rawVersion.hashCode();
        } else {
            hash *= 43;
        }
        if (mainClass != null) {
            hash = hash * 43 + mainClass.hashCode();
        } else {
            hash *= 43;
        }
        if (hash == 0) {
            hash = -1;
        }
        this.hash = hash;
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isOpen()) {
            sb.append("open ");
        }
        sb.append("module { name: ").append(toNameAndVersion());
        if (!requiresSet.isEmpty()) {
            sb.append(", requires: ").append(requiresSet);
        }
        if (!uses.isEmpty()) {
            sb.append(", uses: ").append(uses);
        }
        if (!exportsSet.isEmpty()) {
            sb.append(", exports: ").append(exportsSet);
        }
        if (!opensSet.isEmpty()) {
            sb.append(", opens: ").append(opensSet);
        }
        if (!provides.isEmpty()) {
            sb.append(", provides: ").append(provides);
        }
        sb.append(" }");
        return sb.toString();
    }

    private static long longHash(EnumSet<?> enums) {
        long hash = 0;
        for (Enum<?> anEnum : enums) {
            hash |= 1L << anEnum.ordinal();
        }
        return hash;
    }

    private static <T extends Object & Comparable<T>> int compare(T a, T b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    private static <T extends Object & Comparable<T>> int compare(Set<T> a, Set<T> b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        T[] aArray = (T[]) a.toArray();
        T[] bArray = (T[]) b.toArray();
        Arrays.sort(aArray);
        Arrays.sort(bArray);
        return J_U_Arrays.compare(aArray, bArray);
    }

    @Adapter("java/lang/module/ModuleDescriptor$Modifier")
    public enum Modifier {
        OPEN, AUTOMATIC, SYNTHETIC, MANDATED
    }

    @Adapter("java/lang/module/ModuleDescriptor$Builder")
    public static final class Builder {
        final String name;
        final boolean strict;
        final Set<Modifier> modifiers;
        final Set<String> packages = new HashSet<>();
        final Map<String, Requires> requires = new HashMap<>();
        final Map<String, Exports> exports = new HashMap<>();
        final Map<String, Opens> opens = new HashMap<>();
        final Set<String> uses = new HashSet<>();
        final Map<String, Provides> provides = new HashMap<>();
        Version version;
        String rawVersion;
        String mainClass;

        Builder(String name, boolean strict, Set<Modifier> modifiers) {
            this.name = name;
            this.strict = strict;
            this.modifiers = modifiers;
            assert !(modifiers.contains(Modifier.OPEN) && modifiers.contains(Modifier.AUTOMATIC));
        }

        public Builder requires(Requires requires) {
            if (modifiers.contains(Modifier.AUTOMATIC)) {
                throw new IllegalStateException("Automatic modules cannot declare dependencies");
            }
            if (name.equals(requires.name)) {
                throw new IllegalArgumentException("Dependence on self");
            }
            if (this.requires.containsKey(requires.name)) {
                throw new IllegalArgumentException("Dependence upon " + requires.name + " already declared");
            }
            this.requires.put(requires.name, requires);
            return this;
        }

        public Builder requires(Set<Requires.Modifier> mods, String name, Version version) {
            Objects.requireNonNull(version);
            if (strict) {
                NameChecks.checkModuleName(name);
            }
            return requires(new Requires(mods, name, version, null));
        }

        public Builder requires(Set<Requires.Modifier> mods, String name) {
            if (strict) {
                NameChecks.checkModuleName(name);
            }
            return requires(new Requires(mods, name, null, null));
        }

        public Builder requires(String name) {
            return requires(EnumSet.noneOf(Requires.Modifier.class), name);
        }

        public Builder exports(Exports exports) {
            if (this.modifiers.contains(Modifier.AUTOMATIC)) {
                throw new IllegalStateException("Automatic modules cannot export packages");
            }
            if (this.exports.containsKey(exports.source)) {
                throw new IllegalArgumentException("Exported package " + exports.source + " already declared");
            }
            this.exports.put(exports.source, exports);
            this.packages.add(exports.source);
            return this;
        }

        public Builder exports(Set<Exports.Modifier> mods, String source, Set<String> targets) {
            if (strict) {
                NameChecks.checkPackageName(source);
                for (String target : targets) {
                    NameChecks.checkModuleName(target);
                }
            }
            return exports(new Exports(mods, source, targets));
        }

        public Builder exports(Set<Exports.Modifier> mods, String source) {
            if (strict) {
                NameChecks.checkPackageName(source);
            }
            return exports(new Exports(mods, source, Collections.emptySet()));
        }

        public Builder exports(String source, Set<String> targets) {
            return exports(EnumSet.noneOf(Exports.Modifier.class), source, targets);
        }

        public Builder exports(String source) {
            return exports(EnumSet.noneOf(Exports.Modifier.class), source);
        }

        public Builder opens(Opens opens) {
            if (this.modifiers.contains(Modifier.AUTOMATIC) || this.modifiers.contains(Modifier.OPEN)) {
                throw new IllegalStateException("Open or Automatic modules cannot open packages");
            }
            if (this.opens.containsKey(opens.source)) {
                throw new IllegalArgumentException("Open package " + opens.source + " already declared");
            }
            this.opens.put(opens.source, opens);
            this.packages.add(opens.source);
            return this;
        }

        public Builder opens(Set<Opens.Modifier> mods, String source, Set<String> targets) {
            if (strict) {
                NameChecks.checkPackageName(source);
                for (String target : targets) {
                    NameChecks.checkModuleName(target);
                }
            }
            return opens(new Opens(mods, source, targets));
        }

        public Builder opens(Set<Opens.Modifier> mods, String source) {
            if (strict) {
                NameChecks.checkPackageName(source);
            }
            return opens(new Opens(mods, source, Collections.emptySet()));
        }

        public Builder opens(String source, Set<String> targets) {
            return opens(EnumSet.noneOf(Opens.Modifier.class), source, targets);
        }

        public Builder opens(String source) {
            return opens(EnumSet.noneOf(Opens.Modifier.class), source);
        }

        public Builder uses(String service) {
            if (modifiers.contains(Modifier.AUTOMATIC)) {
                throw new IllegalStateException("Automatic modules cannot declare service dependences");
            }
            NameChecks.checkServiceTypeName(service);
            if (this.uses.contains(service)) {
                throw new IllegalArgumentException("Service " + service + " already declared");
            }
            this.uses.add(service);
            return this;
        }

        public Builder provides(Provides provides) {
            if (this.provides.containsKey(provides.service)) {
                throw new IllegalArgumentException("Providers of service " + provides.service + " already provided");
            }
            this.provides.put(provides.service, provides);
            for (String provider : provides.providers()) {
                packages.add(packageName(provider));
            }
            return this;
        }

        private static String packageName(String cn) {
            int index = cn.lastIndexOf('.');
            return (index == -1) ? "" : cn.substring(0, index);
        }

        public Builder provides(String service, List<String> providers) {
            if (providers.isEmpty()) {
                throw new IllegalArgumentException("Empty providers set");
            }
            if (strict) {
                NameChecks.checkServiceTypeName(service);
                for (String provider : providers) {
                    NameChecks.checkServiceProviderName(provider);
                }
            } else {
                if (packageName(service).isEmpty()) {
                    throw new IllegalArgumentException(service + ": unnamed package");
                }
                for (String provider : providers) {
                    if (packageName(provider).isEmpty()) {
                        throw new IllegalArgumentException(provider + ": unnamed package");
                    }
                }
            }
            return provides(new Provides(service, providers));
        }

        public Builder packages(Set<String> packages) {
            if (strict) {
                for (String pkg : packages) {
                    NameChecks.checkPackageName(pkg);
                }
            }
            this.packages.addAll(packages);
            return this;
        }

        public Builder version(Version version) {
            this.version = Objects.requireNonNull(version);
            this.rawVersion = null;
            return this;
        }

        public Builder version(String version) {
            try {
                this.version = Version.parse(version);
                this.rawVersion = null;
            } catch (IllegalArgumentException e) {
                if (strict) {
                    throw e;
                }
                this.version = null;
                this.rawVersion = version;
            }
            return this;
        }

        public Builder mainClass(String mainClass) {
            if (strict) {
                NameChecks.checkClassName("main class name", mainClass);
            } else {
                if (packageName(mainClass).isEmpty()) {
                    throw new IllegalArgumentException(mainClass + ": unnamed package");
                }
            }
            packages.add(packageName(mainClass));
            this.mainClass = mainClass;
            return this;
        }

        public J_L_M_ModuleDescriptor build() {
            Set<Requires> requires = new HashSet<>(this.requires.values());
            Set<Exports> exports = new HashSet<>(this.exports.values());
            Set<Opens> opens = new HashSet<>(this.opens.values());
            Set<Provides> provides = new HashSet<>(this.provides.values());
            if (strict && !name.equals("java.base") && !this.requires.containsKey("java.base")) {
                requires.add(new Requires(EnumSet.of(Requires.Modifier.MANDATED), "java.base", null, null));
            }

            return new J_L_M_ModuleDescriptor(
                    name,
                    version,
                    rawVersion,
                    modifiers,
                    requires,
                    exports,
                    opens,
                    uses,
                    provides,
                    packages,
                    mainClass
            );
        }
    }

    @Adapter("java/lang/module/ModuleDescriptor$Requires")
    public final static class Requires implements Comparable<Requires> {
        private final EnumSet<Modifier> modifiers;
        private final String name;
        private final Version version;
        private final String rawVersion;

        private Requires(Set<Modifier> modifiers, String name, Version version, String rawVersion) {
            this.modifiers = EnumSet.copyOf(modifiers);
            this.name = name;
            this.version = version;
            this.rawVersion = rawVersion;
        }

        public Set<Modifier> modifiers() {
            return Collections.unmodifiableSet(modifiers);
        }

        public String name() {
            return name;
        }

        public Optional<Version> compiledVersion() {
            return Optional.ofNullable(version);
        }

        public Optional<String> rawCompiledVersion() {
            if (version != null) {
                return Optional.of(version.toString());
            } else {
                return Optional.ofNullable(rawVersion);
            }
        }

        @Override
        public int compareTo(@NotNull Requires o) {
            if (this == o) return 0;
            int i = name.compareTo(o.name);
            if (i != 0) return i;

            i = Long.compare(longHash(modifiers), longHash(o.modifiers));
            if (i != 0) return i;

            i = compare(version, o.version);
            if (i != 0) return i;

            return compare(rawVersion, o.rawVersion);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Requires)) return false;
            Requires requires = (Requires) o;
            return Objects.equals(modifiers, requires.modifiers) && Objects.equals(name, requires.name) && Objects.equals(version, requires.version) && Objects.equals(rawVersion, requires.rawVersion);
        }

        @Override
        public int hashCode() {
            int hash = name.hashCode() * 43 + modifiers.hashCode();
            if (version != null) {
                hash = hash * 43 + version.hashCode();
            }
            if (rawVersion != null) {
                hash = hash * 43 + rawVersion.hashCode();
            }
            return hash;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Modifier mod : modifiers) {
                sb.append(mod.toString().toLowerCase()).append(" ");
            }
            if (version != null) {
                sb.append(name).append("@(").append(version).append(")");
            } else {
                sb.append(name);
            }
            return sb.toString();
        }

        @Adapter("java/lang/module/ModuleDescriptor$Requires$Modifier")
        public  enum Modifier {
            TRANSITIVE, STATIC, SYNTHETIC, MANDATED
        }
    }

    @Adapter("java/lang/module/ModuleDescriptor$Exports")
    public final static class Exports implements Comparable<Exports> {
        private final EnumSet<Modifier> modifiers;
        private final String source;
        private final Set<String> targets;

        private Exports(Set<Modifier> modifiers, String source, Set<String> targets) {
            this.modifiers = EnumSet.copyOf(modifiers);
            this.source = source;
            this.targets = Collections.unmodifiableSet(targets);
        }

        public Set<Modifier> modifiers() {
            return Collections.unmodifiableSet(modifiers);
        }

        public boolean isQualified() {
            return !targets.isEmpty();
        }

        public String source() {
            return source;
        }

        public Set<String> targets() {
            return targets;
        }

        @Override
        public int compareTo(@NotNull Exports o) {
            if (this == o) return 0;
            int i = source.compareTo(o.source);
            if (i != 0) return i;

            i = Long.compare(longHash(modifiers), longHash(o.modifiers));
            if (i != 0) return i;

            return compare(targets, o.targets);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Exports)) return false;
            Exports exports = (Exports) o;
            return Objects.equals(modifiers, exports.modifiers) && Objects.equals(source, exports.source) && Objects.equals(targets, exports.targets);
        }

        @Override
        public int hashCode() {
            int hash = source.hashCode() * 43 + modifiers.hashCode();
            hash = hash * 43 + targets.hashCode();
            return hash;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Modifier mod : modifiers) {
                sb.append(mod.toString().toLowerCase()).append(" ");
            }
            sb.append(source);
            if (!targets.isEmpty()) {
                sb.append(" to ").append(targets);
            }
            return sb.toString();
        }

        @Adapter("java/lang/module/ModuleDescriptor$Exports$Modifier")
        public enum Modifier {
            SYNTHETIC, MANDATED
        }
    }

    @Adapter("java/lang/module/ModuleDescriptor$Opens")
    public final static class Opens implements Comparable<Opens> {
        private final EnumSet<Modifier> modifiers;
        private final String source;
        private final Set<String> targets;

        private Opens(Set<Modifier> modifiers, String source, Set<String> targets) {
            this.modifiers = EnumSet.copyOf(modifiers);
            this.source = source;
            this.targets = Collections.unmodifiableSet(targets);
        }

        public Set<Modifier> modifiers() {
            return Collections.unmodifiableSet(modifiers);
        }

        public boolean isQualified() {
            return !targets.isEmpty();
        }

        public String source() {
            return source;
        }

        public Set<String> targets() {
            return targets;
        }

        @Override
        public int compareTo(@NotNull Opens o) {
            if (this == o) return 0;
            int i = source.compareTo(o.source);
            if (i != 0) return i;

            i = Long.compare(longHash(modifiers), longHash(o.modifiers));
            if (i != 0) return i;

            return compare(targets, o.targets);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Opens)) return false;
            Opens opens = (Opens) o;
            return Objects.equals(modifiers, opens.modifiers) && Objects.equals(source, opens.source) && Objects.equals(targets, opens.targets);
        }

        @Override
        public int hashCode() {
            int hash = source.hashCode() * 43 + modifiers.hashCode();
            hash = hash * 43 + targets.hashCode();
            return hash;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Modifier mod : modifiers) {
                sb.append(mod.toString().toLowerCase()).append(" ");
            }
            sb.append(source);
            if (!targets.isEmpty()) {
                sb.append(" to ").append(targets);
            }
            return sb.toString();
        }

        @Adapter("java/lang/module/ModuleDescriptor$Opens$Modifier")
        public enum Modifier {
            SYNTHETIC, MANDATED
        }
    }

    @Adapter("java/lang/module/ModuleDescriptor$Provides")
    public final static class Provides implements Comparable<Provides> {
        private final String service;
        private final List<String> providers;

        private Provides(String service, List<String> providers) {
            this.service = service;
            this.providers = Collections.unmodifiableList(providers);
        }

        public String service() {
            return service;
        }

        public List<String> providers() {
            return providers;
        }

        @Override
        public int compareTo(@NotNull Provides o) {
            if (this == o) return 0;
            int i = service.compareTo(o.service);
            if (i != 0) return i;

            return J_U_Arrays.compare(providers.toArray(new String[0]), o.providers.toArray(new String[0]));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Provides)) return false;
            Provides provides = (Provides) o;
            return Objects.equals(service, provides.service) && Objects.equals(providers, provides.providers);
        }

        @Override
        public int hashCode() {
            int hash = service.hashCode() * 43;
            hash = hash * 43 + providers.hashCode();
            return hash;
        }

        @Override
        public String toString() {
            return service + " with " + providers;
        }
    }

    @Adapter("java/lang/module/ModuleDescriptor$Version")
    public final static class Version implements Comparable<Version> {
        private final String version;
        private final List<Object> sequence;
        private final List<Object> pre;
        private final List<Object> build;

        private Version(String version) {
            if (version == null) {
                throw new IllegalArgumentException("Null version string");
            }
            if (version.isEmpty()) {
                throw new IllegalArgumentException("Empty version string");
            }
            this.version = version;
            int i = 0;
            int l = version.length() - 1;
            char c = version.charAt(i);
            if (!Character.isDigit(c)) {
                throw new IllegalArgumentException(version + ": Version string does not start with a number");
            }
            List<Object> seq = new ArrayList<>(4);
            StringBuilder sb = new StringBuilder();
            while (c != '-' && c != '+' && i < l) {
                while (c != '.' && c != '-' && c != '+' && i < l) {
                    sb.append(c);
                    c = version.charAt(++i);
                }
                try {
                    seq.add(Integer.parseInt(sb.toString()));
                } catch (NumberFormatException e) {
                    seq.add(sb.toString());
                }
                sb.setLength(0);
            }
            sequence = Collections.unmodifiableList(seq);
            if (c == '-' && i >= version.length()) {
                throw new IllegalArgumentException(version + ": Empty pre-release");
            }

            List<Object> pre = new ArrayList<>(2);
            while (c != '+' && i < l) {
                sb.setLength(0);
                c = version.charAt(++i);
                while (c != '.' && c != '-' && c != '+' && i < l) {
                    sb.append(c);
                    c = version.charAt(++i);
                }
                try {
                    pre.add(Integer.parseInt(sb.toString()));
                } catch (NumberFormatException e) {
                    pre.add(sb.toString());
                }
            }
            this.pre = Collections.unmodifiableList(pre);
            if (c == '+' && i >= version.length()) {
                throw new IllegalArgumentException(version + ": Empty pre-release");
            }

            List<Object> build = new ArrayList<>(2);
            while (i < l) {
                sb.setLength(0);
                c = version.charAt(++i);
                while (c != '.' && c != '-' && c != '+' && i < l) {
                    sb.append(c);
                    c = version.charAt(++i);
                }
                try {
                    build.add(Integer.parseInt(sb.toString()));
                } catch (NumberFormatException e) {
                    build.add(sb.toString());
                }
            }
            this.build = Collections.unmodifiableList(build);
        }

        public static Version parse(String version) {
            return new Version(version);
        }

        private static int cmp(Object a, Object b) {
            if (a instanceof Integer && b instanceof Integer) {
                return Integer.compare((Integer) a, (Integer) b);
            } else {
                return a.toString().compareTo(b.toString());
            }
        }

        @Override
        public int compareTo(@NotNull Version o) {
            int c = J_U_Arrays.compare(sequence.toArray(), o.sequence.toArray(), Version::cmp);
            if (c != 0) return c;
            if (this.pre.isEmpty() && !o.pre.isEmpty()) return 1;
            if (!this.pre.isEmpty() && o.pre.isEmpty()) return -1;
            c = J_U_Arrays.compare(pre.toArray(), o.pre.toArray(), Version::cmp);
            if (c != 0) return c;
            return J_U_Arrays.compare(build.toArray(), o.build.toArray(), Version::cmp);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Version && compareTo((Version) obj) == 0;
        }

        @Override
        public int hashCode() {
            return version.hashCode();
        }

        @Override
        public String toString() {
            return version;
        }
    }
}
