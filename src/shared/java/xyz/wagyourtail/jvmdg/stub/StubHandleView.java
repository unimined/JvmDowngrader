package xyz.wagyourtail.jvmdg.stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Source-API view over the {@code MethodHandle}s the reflection layer
 * produces — the companion to {@link StubMethodView}: where the table hands
 * out a receiver-first {@code findStatic} on an impl, a later {@code
 * Lookup.revealDirect} on that handle would natively expose the impl class's
 * internals. Instead the produced handle registers a {@link
 * MethodHandleInfo} view answering what the source member would have:
 * original owner, name, signature, declaration flags, and the source
 * {@code REF_*} kind. Unregistered handles pass straight through — the
 * lookup is one weak-identity-map probe.
 */
public final class StubHandleView {

    private StubHandleView() {
    }

    private static final class Info implements MethodHandleInfo {
        /** the member {@link #reflectAs} answers — the impl's own {@code Method}, or the target {@code Constructor} for ctor handles */
        final Member impl;
        /** the source owner's runtime stand-in */
        final Class<?> declaring;
        final String name;
        /** the source signature, in runtime-facing types */
        final MethodType type;
        /** the source {@code REF_*} kind */
        final int kind;
        final int mods;

        Info(Member impl, Class<?> declaring, String name,
             MethodType type, int kind, int mods) {
            this.impl = impl;
            this.declaring = declaring;
            this.name = name;
            this.type = type;
            this.kind = kind;
            this.mods = mods;
        }

        @Override
        public int getReferenceKind() {
            return kind;
        }

        @Override
        public Class<?> getDeclaringClass() {
            return declaring;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public MethodType getMethodType() {
            return type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends Member> T reflectAs(Class<T> expected, MethodHandles.Lookup lookup) {
            if (!expected.isInstance(impl)) {
                throw new ClassCastException("handle reflects as " + impl.getClass().getName()
                    + ", not " + expected.getName());
            }
            return (T) impl;
        }

        @Override
        public int getModifiers() {
            return mods & Modifier.methodModifiers();
        }

        @Override
        public String toString() {
            return MethodHandleInfo.toString(kind, declaring, name, type);
        }
    }

    /** produced handle → its source view; weak so handles don't pin the table */
    private static final Map<MethodHandle, Info> HANDLES =
        Collections.synchronizedMap(new WeakHashMap<MethodHandle, Info>());

    /**
     * Called by the reflection table as it hands a produced handle out —
     * {@code reflectAs} answers the impl's own {@code Member} (the same
     * object {@code getMethod}/{@code getConstructor} hands out, so the
     * two views stay identity-consistent).
     */
    public static void register(MethodHandle handle, Member impl, Class<?> declaring,
                                String name, MethodType type, int kind, int mods) {
        HANDLES.put(handle, new Info(impl, declaring, name, type, kind, mods));
    }

    /**
     * {@code Lookup.revealDirect} — the registered view for a produced
     * handle, or {@code null} for anything else.
     */
    public static MethodHandleInfo info(MethodHandle handle) {
        return HANDLES.get(handle);
    }
}
