package xyz.wagyourtail.jvmdg.version.map;

import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Coverage;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ClassMapping implements MemberResolver {

    public final Type current;
    protected final Lazy<Map<MemberNameAndDesc, Pair<Boolean, Type>>> members;
    protected final Lazy<List<ClassMapping>> parents;
    protected final VersionProvider vp;
    /** this owner's slice of the provider registry — re-read until present so a mapping created before registration still sees entries */
    private StubRegistry.Owner stubs;

    private final Coverage coverage;
    private final MemberStubTransform memberStubTransform = new MemberStubTransform();

    public ClassMapping(final Coverage coverage, final Lazy<List<ClassMapping>> parents, final Type current, final boolean isInterface, final IOFunction<Type, Set<MemberNameAndDesc>> members, VersionProvider vp) {
        this.parents = parents;
        this.current = current;
        this.coverage = coverage;
        this.members = new Lazy<Map<MemberNameAndDesc, Pair<Boolean, Type>>>() {

            @Override
            protected Map<MemberNameAndDesc, Pair<Boolean, Type>> init() {
                try {
                    Map<MemberNameAndDesc, Pair<Boolean, Type>> mems = new HashMap<>();
                    Set<MemberNameAndDesc> m = members.apply(current);
                    if (m != null) {
//                        mems.addAll(m);
                        for (MemberNameAndDesc member : m) {
                            mems.put(member, new Pair<>(isInterface, current));
                        }
                    }
                    // fix multi-inheritance
                    for (ClassMapping parent : parents.get()) {
                        for (Map.Entry<MemberNameAndDesc, Pair<Boolean, Type>> member : parent.members.get().entrySet()) {
                            if (!mems.containsKey(member.getKey()) || !member.getValue().getFirst()) {
                                mems.put(member.getKey(), member.getValue());
                            }
                        }
                    }
                    return mems;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        };
        this.vp = vp;
    }

    /** read slice — re-reads while absent so entries registered after this mapping's construction stay visible */
    private StubRegistry.Owner stubs() {
        if (stubs == null) {
            stubs = vp.stubRegistry.owner(current);
        }
        return stubs;
    }

    /** write slice — creates the owner's registry entry on first use */
    private StubRegistry.Owner stubsForWrite() {
        if (stubs == null) {
            stubs = vp.stubRegistry.forOwner(current);
        }
        return stubs;
    }

    public boolean isAssignableFrom(Type target) {
        if (target.equals(current)) {
            return true;
        }
        for (ClassMapping parent : parents.get()) {
            if (parent.isAssignableFrom(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rebuilds a member with adapter (fake) types in its desc translated back
     * to their real (JDK) types. Api-internal call sites carry fake types
     * after api downgrading, while registered members and pre-transform call
     * sites carry real ones — matching requires the real form.
     */
    @Override
    public MemberNameAndDesc unstubMember(String name, Type desc) {
        if (desc.getSort() == Type.METHOD) {
            Type[] args = desc.getArgumentTypes();
            Type ret = desc.getReturnType();
            boolean diff = false;
            for (int i = 0; i < args.length; i++) {
                Type u = vp.unstubClass(args[i]);
                if (u != args[i]) {
                    args[i] = u;
                    diff = true;
                }
            }
            Type uret = vp.unstubClass(ret);
            if (diff || uret != ret) {
                desc = Type.getMethodType(uret, args);
            }
        } else {
            desc = vp.unstubClass(desc);
        }
        return new MemberNameAndDesc(name, desc);
    }

    public void addStub(MemberNameAndDesc member, StubEntry entry) {
        int modifiers = entry.method.getModifiers();
        if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            throw new RuntimeException("@Stub " + entry.method + " must be public static");
        }
        StubRegistry.Owner stubs = stubsForWrite();
        if (member.isMethod()) {
            if (member.getName().equals("forEachRemaining") && member.getDesc().getDescriptor().contains("Consumer")) {
                new Exception("addStub " + member + " via " + entry.method + (entry.abstractDefault ? " [abstract]" : " [method]")).printStackTrace();
            }
            stubs.methodStub.put(member, entry);
        } else {
            FieldStubEntry field = stubs.fieldStub.get(member);
            if (entry.method.getReturnType().equals(Void.TYPE)) {
                field = field == null ? new FieldStubEntry(null, entry) : field.withSetter(entry);
            } else {
                field = field == null ? new FieldStubEntry(entry, null) : field.withGetter(entry);
            }
            stubs.fieldStub.put(member, field);
        }
    }

    /** registers a ctor callsite entry; a second entry on the same ctor (e.g. {@code @InitFactory} + {@code @InitRewrite}) is a registration error */
    public void addCtor(MemberNameAndDesc member, CtorEntry entry) {
        if (stubsForWrite().ctor.putIfAbsent(member, entry) != null) {
            throw new RuntimeException("conflicting ctor entries on " +
                member.toFullyQualified(current));
        }
    }

    public void addModify(MemberNameAndDesc member, Method method) {
        int modifiers = method.getModifiers();
        if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            throw new RuntimeException("@Modify " + method + " must be public static");
        }
        stubsForWrite().memberModify.put(member, method);
    }

    public void warnMember(MemberNameAndDesc member, Set<String> warnings, boolean invoke_static) {
        FullyQualifiedMemberNameAndDesc fqn = member.toFullyQualified(current);
        String mod = coverage.checkMember(fqn);
        if (mod != null) {
            coverage.warnMember(fqn, warnings);
        }
        if (!invoke_static && !member.getName().equals("<init>")) {
            for (ClassMapping parent : parents.get()) {
                parent.warnMember(member, warnings, false);
            }
        }
    }

    /** emission lives in {@link MemberStubTransform} — this stays as the call-site facade while stubMethods migrates */
    public void transform(MethodNode method, int index, ClassNode classNode, Set<ClassNode> extra, boolean runtimeAvailable, Set<String> warnings) {
        memberStubTransform.transformInsn(this, method, index, classNode, extra, runtimeAvailable, warnings);
    }

    /**
     * Raw {@code memberModify} lookup for the indy bootstrap path — no
     * wildcard fallback, matching the original inline site.
     */
    @Override
    public Method rawModify(MemberNameAndDesc member) {
        StubRegistry.Owner stubs = stubs();
        return stubs == null ? null : stubs.memberModify.get(member);
    }

    public StubEntry getParentStubFor(MemberNameAndDesc member, boolean runtimeAvailable, int opcode, Set<String> warnings) {
        for (ClassMapping parent : parents.get()) {
            StubEntry node = parent.getStubFor(member, runtimeAvailable, opcode, warnings);
            if (node != null) {

                // check if explicitly excluded from stubbing
                // ie. method exists on the subtype in question
                for (String child : node.excludeChild) {
                    if (child.startsWith("L") && child.endsWith(";")) {
                        child = child.substring(1, child.length() - 1);
                    }
                    if (isAssignableFrom(Type.getObjectType(child))) {
                        return null;
                    }
                }

                return node;
            }
        }
        return null;
    }

    @Override
    public StubEntry getStubFor(MemberNameAndDesc member, boolean runtimeAvailable, int opcode, Set<String> warnings) {
        opcode = ASMUtils.toHandleTag(opcode);
        try {
            StubRegistry.Owner stubs = stubs();
            StubEntry entry;
            if (member.isMethod()) {
                entry = stubs == null ? null : stubs.methodStub.get(member);
                if (entry == null) {
                    if (opcode != Opcodes.H_INVOKESTATIC && !member.getName().equals("<init>")) {
                        Map<MemberNameAndDesc, Pair<Boolean, Type>> members = this.members.get();
                        if (members != null && members.containsKey(member)) {
                            return null;
                        }
                        return getParentStubFor(member, runtimeAvailable, opcode, warnings);
                    }
                    warnMember(member, warnings, opcode == Opcodes.H_INVOKESTATIC);
                    return null;
                }
            } else {
                FieldStubEntry stub = stubs == null ? null : stubs.fieldStub.get(member);
                switch (opcode) {
                    case Opcodes.H_GETFIELD:
                        if (stub == null) {
                            Map<MemberNameAndDesc, Pair<Boolean, Type>> members = this.members.get();
                            if (members != null && members.containsKey(member)) {
                                return null;
                            }
                            return getParentStubFor(member, runtimeAvailable, opcode, warnings);
                        }
                    case Opcodes.H_GETSTATIC:
                        if (stub == null) {
                            return null;
                        }
                        entry = stub.getter;
                        break;
                    case Opcodes.H_PUTFIELD:
                        if (stub == null) {
                            Map<MemberNameAndDesc, Pair<Boolean, Type>> members = this.members.get();
                            if (members != null && members.containsKey(member)) {
                                return null;
                            }
                            return getParentStubFor(member, runtimeAvailable, opcode, warnings);
                        }
                    case Opcodes.H_PUTSTATIC:
                        if (stub == null) {
                            return null;
                        }
                        entry = stub.setter;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected Handle: " + opcode);
                }
                if (entry == null) {
                    return null;
                }
            }
            if (!runtimeAvailable && entry.requiresRuntime) {
                warnings.add(entry.method + " requires runtime transformation but runtime is not available...");
                return null;
            }
            // NEWINVOKESPECIAL is the <init> flavour of invokespecial — a
            // ctor handle must respect noSpecial the same way
            if ((opcode == Opcodes.H_INVOKESPECIAL || opcode == Opcodes.H_NEWINVOKESPECIAL)
                && entry.noSpecial) {
                return null;
            }
            return entry;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CtorEntry getCtorFor(MemberNameAndDesc member, boolean runtimeAvailable, Set<String> warnings) {
        try {
            StubRegistry.Owner stubs = stubs();
            CtorEntry entry = stubs == null ? null : stubs.ctor.get(member);
            if (entry != null && !runtimeAvailable && entry.requiresRuntime) {
                warnings.add(member + " requires runtime transformation but runtime is not available...");
                return null;
            }
            return entry;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Method getParentModifyFor(MemberNameAndDesc member, int opcode, Set<String> warnings) {
        for (ClassMapping parent : parents.get()) {
            Method node = parent.getModifyFor(member, opcode, warnings);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    @Override
    public Method getModifyFor(MemberNameAndDesc member, int opcode, Set<String> warnings) {
        opcode = ASMUtils.toHandleTag(opcode);
        boolean isStatic = false;
        switch (opcode) {
            case Opcodes.H_GETSTATIC:
            case Opcodes.H_PUTSTATIC:
            case Opcodes.H_INVOKESTATIC:
                isStatic = true;
        }
        try {
            StubRegistry.Owner stubs = stubs();
            Method m = stubs == null ? null : stubs.memberModify.get(member);
            if (m == null && stubs != null) {
                m = stubs.memberModify.get(null);
            }
            if (m == null) {
                if (!isStatic && (member == null || !member.getName().equals("<init>"))) {
                    Map<MemberNameAndDesc, Pair<Boolean, Type>> members = this.members.get();
                    if (members != null && members.containsKey(member)) {
                        return null;
                    }
                    return getParentModifyFor(member, opcode, warnings);
                }
                warnMember(member, warnings, opcode == Opcodes.H_INVOKESTATIC);
                return null;
            }
            int modifiers = m.getModifiers();
            if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                throw new RuntimeException("modify method must be public static");
            }
            return m;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<MemberNameAndDesc, StubEntry> getMethodStubMap() {
        StubRegistry.Owner stubs = stubs();
        return stubs == null ? Collections.<MemberNameAndDesc, StubEntry>emptyMap() : stubs.methodStub;
    }

    public Map<MemberNameAndDesc, FieldStubEntry> getFieldStubMap() {
        StubRegistry.Owner stubs = stubs();
        return stubs == null ? Collections.<MemberNameAndDesc, FieldStubEntry>emptyMap() : stubs.fieldStub;
    }

    public Map<MemberNameAndDesc, CtorEntry> getCtorMap() {
        StubRegistry.Owner stubs = stubs();
        return stubs == null ? Collections.<MemberNameAndDesc, CtorEntry>emptyMap() : stubs.ctor;
    }

    public Map<MemberNameAndDesc, Method> getMethodModifyMap() {
        StubRegistry.Owner stubs = stubs();
        return stubs == null ? Collections.<MemberNameAndDesc, Method>emptyMap() : stubs.memberModify;
    }

    @VisibleForTesting
    public List<ClassMapping> getParents() {
        return parents.get();
    }

    public List<StubEntry> getStubTargets() {
        List<StubEntry> methods = new ArrayList<>();
        for (ClassMapping parent : parents.get()) {
            methods.addAll(parent.getStubTargets());
        }
        StubRegistry.Owner stubs = stubs();
        if (stubs != null) {
            methods.addAll(stubs.methodStub.values());
        }
        return methods;
    }

    public Map<MemberNameAndDesc, StubEntry> getAbstracts() {
        Map<MemberNameAndDesc, StubEntry> methods = new HashMap<>();
        for (ClassMapping parent : parents.get()) {
            methods.putAll(parent.getAbstracts());
        }
        StubRegistry.Owner stubs = stubs();
        if (stubs != null) {
            for (Map.Entry<MemberNameAndDesc, StubEntry> entry : stubs.methodStub.entrySet()) {
                if (entry.getValue().abstractDefault) {
                    methods.put(entry.getKey(), entry.getValue());
                }
            }
        }
        // remove if can resolve a non-abstract already
        for (MemberNameAndDesc memberNameAndDesc : this.members.get().keySet()) {
            methods.remove(memberNameAndDesc);
        }
        return methods;
    }

    public Map<MemberNameAndDesc, Pair<Boolean, Type>> getMembers() {
        return members.get();
    }

    @Override
    public VersionProvider provider() {
        return vp;
    }

}
