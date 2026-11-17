package xyz.wagyourtail.jvmdg.version.map;

/**
 * Registered stubs for one field member: the {@code get} accessor and/or the
 * {@code put} accessor — either side may be null. A {@code void}-returning
 * handler registers as the setter, anything else as the getter (the direction
 * is unambiguous from the signature, so it stays inside {@code @Stub}).
 */
public class FieldStubEntry {

    public final StubEntry getter;
    public final StubEntry setter;

    public FieldStubEntry(StubEntry getter, StubEntry setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public FieldStubEntry withGetter(StubEntry getter) {
        return new FieldStubEntry(getter, setter);
    }

    public FieldStubEntry withSetter(StubEntry setter) {
        return new FieldStubEntry(getter, setter);
    }
}
