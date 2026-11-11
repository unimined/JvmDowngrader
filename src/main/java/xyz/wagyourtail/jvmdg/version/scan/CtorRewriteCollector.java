package xyz.wagyourtail.jvmdg.version.scan;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitPre;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.map.CtorRewrite;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Two-phase staging for the {@code @InitRewrite} family on one stub class.
 * Class-level {@link InitRewrite}s declare what to rewrite; method-level
 * {@link InitArg}/{@link InitPre}/{@link InitPost} handlers stage here during
 * the member scan and bind against the declared rewrites in {@link #finish}
 * — link order can't be relied on, so every handler resolves by
 * {@code target} (original ctor descriptor) + optional {@code owner}.
 * <p>
 * All validation happens here at registration: malformed descriptors, unbound
 * or doubly-filled {@code to} slots, ambiguous {@code from}/{@code keep}
 * bindings, type mismatches, duplicate pre/post, orphan handlers, and
 * duplicate/conflicting ctor registrations each land in
 * {@code ctx.failures} with the rewrite's identity — nothing is left to warn
 * per-callsite at transform time.
 */
public class CtorRewriteCollector {

    private final List<InitRewrite> rewrites = new ArrayList<>();
    private final List<Pending> pendingArgs = new ArrayList<>();
    private final List<Pending> pendingPres = new ArrayList<>();
    private final List<Pending> pendingPosts = new ArrayList<>();

    /** a method-level handler awaiting its rewrite: {@code slot >= 0} only for {@code @InitArg} */
    private static class Pending {
        final Method method;
        final String target;
        final String owner;
        final int slot;
        final int[] from;

        Pending(Method method, String target, String owner, int slot, int[] from) {
            this.method = method;
            this.target = target;
            this.owner = owner;
            this.slot = slot;
            this.from = from;
        }
    }

    public void add(InitRewrite rw) {
        rewrites.add(rw);
    }

    public void addArg(Method m, InitArg a) {
        pendingArgs.add(new Pending(m, a.target(), a.owner(), a.slot(), a.from()));
    }

    public void addPre(Method m, InitPre p) {
        pendingPres.add(new Pending(m, p.target(), p.owner(), -1, p.from()));
    }

    public void addPost(Method m, InitPost p) {
        pendingPosts.add(new Pending(m, p.target(), p.owner(), -1, p.from()));
    }

    public boolean empty() {
        return rewrites.isEmpty() && pendingArgs.isEmpty() && pendingPres.isEmpty() && pendingPosts.isEmpty();
    }

    /**
     * Links staged handlers onto declared rewrites, validates the bound
     * shape, and registers each rewrite via {@code addCtor}. A rewrite that
     * fails validation produces one {@code ctx.failures} entry and registers
     * nothing.
     */
    public void finish(ScanContext ctx) {
        if (empty()) {
            return;
        }
        // parsed originals: index i ↔ rewrites[i]
        List<Type> owners = new ArrayList<>(rewrites.size());
        List<Type> origDescs = new ArrayList<>(rewrites.size());
        List<Type[]> origArgsList = new ArrayList<>(rewrites.size());
        List<String> parseFailures = new ArrayList<>();
        for (int i = 0; i < rewrites.size(); i++) {
            owners.add(null);
            origDescs.add(null);
            origArgsList.add(null);
            try {
                Ref ref = rewrites.get(i).ref();
                if (!ref.member().isEmpty() && !ref.member().equals("<init>")) {
                    throw new IllegalArgumentException("@InitRewrite ref.member must be \"<init>\" or omitted, not \"" + ref.member() + "\"");
                }
                if (ref.value().isEmpty()) {
                    throw new IllegalArgumentException("@InitRewrite requires ref.value naming the original ctor's owner");
                }
                if (ref.desc().isEmpty()) {
                    throw new IllegalArgumentException("@InitRewrite requires ref.desc — the original ctor's descriptor");
                }
                Type owner = ref.value().startsWith("L") && ref.value().endsWith(";")
                    ? Type.getType(ref.value()) : Type.getObjectType(ref.value());
                if (owner.getSort() != Type.OBJECT) {
                    throw new IllegalArgumentException("@InitRewrite ref.value must be an object type, got " + ref.value());
                }
                Type origDesc = methodDesc(ref.desc(), "ref.desc");
                owners.set(i, owner);
                origDescs.set(i, origDesc);
                origArgsList.set(i, origDesc.getArgumentTypes());
            } catch (IllegalArgumentException e) {
                parseFailures.add("@InitRewrite " + rewrites.get(i) + ": " + e.getMessage());
            }
        }

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rewrites.size(); i++) {
            Type owner = owners.get(i);
            Type origDesc = origDescs.get(i);
            if (owner == null) {
                continue; // parse failure already recorded
            }
            InitRewrite rw = rewrites.get(i);
            String key = owner.getInternalName() + " " + origDesc.getDescriptor();
            try {
                if (!seen.add(key)) {
                    throw new IllegalArgumentException("duplicate @InitRewrite for " + owner.getDescriptor() + "<init>" + origDesc.getDescriptor());
                }
                finishOne(rw, owner, origDesc, origArgsList.get(i), ctx);
            } catch (Throwable e) {
                ctx.failures.add("@InitRewrite " + owner.getDescriptor() + "<init>" + origDesc.getDescriptor() + ": " + e.getMessage());
            }
        }
        ctx.failures.addAll(parseFailures);

        // staged handlers matching no declared rewrite — or several, when the
        // class rewrites the same desc on different owners and the handler
        // left owner() empty — are registration errors
        reportUnmatched(pendingArgs, owners, origDescs, ctx.failures);
        reportUnmatched(pendingPres, owners, origDescs, ctx.failures);
        reportUnmatched(pendingPosts, owners, origDescs, ctx.failures);
    }

    private void reportUnmatched(List<Pending> pending, List<Type> owners, List<Type> origDescs,
                                 List<String> failures) {
        for (Pending p : pending) {
            int matches = 0;
            for (int i = 0; i < owners.size(); i++) {
                if (owners.get(i) != null && matches(p, origDescs.get(i), owners.get(i))) {
                    matches++;
                }
            }
            if (matches == 0) {
                failures.add("ctor-rewrite handler " + p.method.getName() +
                    " targets " + p.target + (p.owner.isEmpty() ? "" : " on " + p.owner) +
                    " but no @InitRewrite declares it");
            } else if (matches > 1) {
                failures.add("ctor-rewrite handler " + p.method.getName() +
                    " targets " + p.target + " which " + matches + " rewrites declare — set owner() to disambiguate");
            }
        }
    }

    private void finishOne(InitRewrite rw, Type owner, Type origDesc, Type[] origArgs, ScanContext ctx) throws Exception {
        Type[] toArgs = methodDesc(rw.to(), "to").getArgumentTypes();
        int n = toArgs.length;

        List<Pending> myArgs = matching(pendingArgs, origDesc, owner);
        List<Pending> myPres = matching(pendingPres, origDesc, owner);
        List<Pending> myPosts = matching(pendingPosts, origDesc, owner);

        Method[] argMethods = new Method[n];
        int[][] argFrom = new int[n][];
        int[] keep = new int[n];
        boolean[] claimed = new boolean[n];
        for (Pending p : myArgs) {
            if (p.slot < 0 || p.slot >= n) {
                throw new IllegalArgumentException("@InitArg " + p.method.getName() + " slot " + p.slot + " out of range for to=" + rw.to());
            }
            if (claimed[p.slot]) {
                throw new IllegalArgumentException("@InitArg " + p.method.getName() + " double-fills to-slot " + p.slot);
            }
            claimed[p.slot] = true;
            checkStatic(p.method, "@InitArg");
            Type ret = StubHandler.coercedReturnType(p.method);
            if (!ret.equals(toArgs[p.slot])) {
                throw new IllegalArgumentException("@InitArg " + p.method.getName() + " returns " + ret + " but to-slot " + p.slot + " needs " + toArgs[p.slot]);
            }
            argMethods[p.slot] = p.method;
            argFrom[p.slot] = bind(p.method, p.from, StubHandler.coercedParamTypes(p.method), origArgs, "@InitArg");
        }

        // keep: explicit wins; empty derives each non-@InitArg slot by a
        // unique exact-type match against the original args
        if (rw.keep().length != 0 && rw.keep().length != n) {
            throw new IllegalArgumentException("keep length " + rw.keep().length + " != to arg count " + n);
        }
        for (int i = 0; i < n; i++) {
            if (claimed[i]) {
                if (rw.keep().length != 0 && rw.keep()[i] != -1) {
                    throw new IllegalArgumentException("to-slot " + i + " is filled by both keep and @InitArg " + argMethods[i].getName());
                }
                keep[i] = -1;
            } else if (rw.keep().length != 0) {
                keep[i] = rw.keep()[i] == -1
                    ? -1 : checkKeep(rw.keep()[i], i, origArgs, toArgs);
            } else {
                keep[i] = derive(origArgs, toArgs[i], "to-slot " + i + " (" + toArgs[i] + ")");
            }
            if (keep[i] == -1 && argMethods[i] == null) {
                throw new IllegalArgumentException("to-slot " + i + " (" + toArgs[i] + ") has no original arg and no @InitArg");
            }
        }

        Method pre = null;
        int[] preFrom = new int[0];
        if (!myPres.isEmpty()) {
            if (myPres.size() > 1) {
                throw new IllegalArgumentException("more than one @InitPre — emission order would be nondeterministic");
            }
            Pending p = myPres.get(0);
            checkStatic(p.method, "@InitPre");
            checkVoid(p.method, "@InitPre");
            pre = p.method;
            preFrom = bind(p.method, p.from, StubHandler.coercedParamTypes(p.method), origArgs, "@InitPre");
        }

        Method post = null;
        int[] postFrom = new int[0];
        if (!myPosts.isEmpty()) {
            if (myPosts.size() > 1) {
                throw new IllegalArgumentException("more than one @InitPost — emission order would be nondeterministic");
            }
            Pending p = myPosts.get(0);
            checkStatic(p.method, "@InitPost");
            checkVoid(p.method, "@InitPost");
            Type[] params = StubHandler.coercedParamTypes(p.method);
            int tail = rw.downgradeVersion() ? 1 : 0;
            if (params.length < 1 + tail || !params[0].equals(owner)) {
                throw new IllegalArgumentException("@InitPost " + p.method.getName() +
                    " must take the constructed " + owner.getClassName() + " as its first param" +
                    (tail == 1 ? " (and the input version as its trailing int)" : ""));
            }
            if (tail == 1 && !params[params.length - 1].equals(Type.INT_TYPE)) {
                throw new IllegalArgumentException("@InitPost " + p.method.getName() +
                    " must end with an int param — downgradeVersion is set");
            }
            Type[] binding = new Type[params.length - 1 - tail];
            System.arraycopy(params, 1, binding, 0, binding.length);
            post = p.method;
            postFrom = bind(p.method, p.from, binding, origArgs, "@InitPost");
        } else if (rw.downgradeVersion()) {
            throw new IllegalArgumentException("downgradeVersion requires an @InitPost tail to receive the version int");
        }

        ctx.stubMapper(owner).addCtor(new MemberNameAndDesc("<init>", origDesc),
            new CtorRewrite(toArgs, keep, argMethods, argFrom, pre, preFrom, post, postFrom,
                rw.downgradeVersion(), rw.requiresRuntime()));
    }

    /** {@code to}/{@code ref.desc} parse: a method descriptor returning void */
    private static Type methodDesc(String desc, String what) {
        Type t;
        try {
            t = Type.getType(desc);
        } catch (Exception e) {
            throw new IllegalArgumentException("malformed " + what + " descriptor \"" + desc + "\"");
        }
        if (t.getSort() != Type.METHOD || !Type.VOID_TYPE.equals(t.getReturnType())) {
            throw new IllegalArgumentException(what + " must be a void method descriptor, got \"" + desc + "\"");
        }
        return t;
    }

    private void checkStatic(Method m, String ann) {
        if (!Modifier.isStatic(m.getModifiers()) || !Modifier.isPublic(m.getModifiers())) {
            throw new IllegalArgumentException(ann + " " + m.getName() + " must be public static");
        }
    }

    private void checkVoid(Method m, String ann) {
        if (m.getReturnType() != Void.TYPE) {
            throw new IllegalArgumentException(ann + " " + m.getName() + " must return void — its result would have nowhere to go");
        }
    }

    /** handlers whose target/owner identify this rewrite */
    private List<Pending> matching(List<Pending> pending, Type origDesc, Type owner) {
        List<Pending> out = new ArrayList<>();
        for (Pending p : pending) {
            if (matches(p, origDesc, owner)) {
                out.add(p);
            }
        }
        return out;
    }

    private static boolean matches(Pending p, Type origDesc, Type owner) {
        Type t;
        try {
            t = Type.getType(p.target);
        } catch (Exception e) {
            return false; // malformed target — reported as orphan below
        }
        if (!t.equals(origDesc)) {
            return false;
        }
        return p.owner.isEmpty() || owner.getInternalName().equals(internalName(p.owner));
    }

    private static String internalName(String owner) {
        return owner.startsWith("L") && owner.endsWith(";")
            ? owner.substring(1, owner.length() - 1) : owner;
    }

    /** kept-slot check: in-range and exactly the slot's type */
    private static int checkKeep(int idx, int slot, Type[] origArgs, Type[] toArgs) {
        if (idx < 0 || idx >= origArgs.length) {
            throw new IllegalArgumentException("keep[" + slot + "]=" + idx + " out of range — the original ctor has " + origArgs.length + " args");
        }
        if (!origArgs[idx].equals(toArgs[slot])) {
            throw new IllegalArgumentException("keep[" + slot + "]=" + idx + " feeds " + origArgs[idx] + " into a " + toArgs[slot] + " slot");
        }
        return idx;
    }

    /**
     * Binds a handler's params to original-arg indices: explicit {@code from}
     * validates in-range + type-exact; empty {@code from} derives each param
     * by unique type-match (an ambiguous or missing match is a registration
     * error). Params may be reused across handlers and slots.
     */
    private static int[] bind(Method m, int[] from, Type[] params, Type[] origArgs, String ann) {
        int[] bound = new int[params.length];
        if (from.length != 0) {
            if (from.length != params.length) {
                throw new IllegalArgumentException(ann + " " + m.getName() + " from length " + from.length + " != param count " + params.length);
            }
            for (int i = 0; i < params.length; i++) {
                if (from[i] < 0 || from[i] >= origArgs.length) {
                    throw new IllegalArgumentException(ann + " " + m.getName() + " from[" + i + "]=" + from[i] + " out of range");
                }
                if (!origArgs[from[i]].equals(params[i])) {
                    throw new IllegalArgumentException(ann + " " + m.getName() + " param " + i + " (" + params[i] + ") does not match orig arg " + from[i] + " (" + origArgs[from[i]] + ")");
                }
                bound[i] = from[i];
            }
        } else {
            for (int i = 0; i < params.length; i++) {
                bound[i] = derive(origArgs, params[i], ann + " " + m.getName() + " param " + i + " (" + params[i] + ")");
            }
        }
        return bound;
    }

    /** the single orig-arg index with exactly this type, or error on none/ambiguous */
    private static int derive(Type[] origArgs, Type want, String what) {
        int found = -1;
        for (int i = 0; i < origArgs.length; i++) {
            if (origArgs[i].equals(want)) {
                if (found >= 0) {
                    throw new IllegalArgumentException(what + " matches orig args " + found + " and " + i + " — declare the binding explicitly");
                }
                found = i;
            }
        }
        if (found < 0) {
            throw new IllegalArgumentException(what + " has no original arg of that type");
        }
        return found;
    }
}
