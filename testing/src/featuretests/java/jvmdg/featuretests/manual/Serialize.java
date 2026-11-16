package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Java serialization round-trips: a POJO with serialVersionUID, a transient
 * field (skipped, rebuilt in {@code readObject}), and a record (deserialized
 * through its canonical constructor). Exercises reflection-heavy paths —
 * ctor bypass, field access, serialVersionUID — in transformed code.
 */
public class Serialize {

    public static class Point implements Serializable {
        private static final long serialVersionUID = 1L;

        final int x;
        final int y;
        transient String cache;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
            this.cache = "prebuilt:" + x;
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            this.cache = "rebuilt:" + x;
        }
    }

    public record Rec(String name, int v) implements Serializable {
        private static final long serialVersionUID = 2L;
    }

    public static void main(String[] args) {
        Support.run(Serialize.class);
    }

    private static <T> T roundTrip(T o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bout)) {
            oos.writeObject(o);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()))) {
            @SuppressWarnings("unchecked")
            T t = (T) ois.readObject();
            return t;
        }
    }

    @FeatureTest
    static void pojoRoundTrip() throws IOException, ClassNotFoundException {
        // serialVersionUID + transient field + private readObject hook —
        // ctor-bypass instantiation and field injection in transformed code
        Point p = roundTrip(new Point(3, 4));
        System.out.println(p.x + ":" + p.y);
        // transient field is skipped, then rebuilt by readObject
        System.out.println(p.cache);
    }

    @FeatureTest
    static void recordRoundTrip() throws IOException, ClassNotFoundException {
        // records deserialize through the canonical constructor — validates
        // the transformed record's ctor signature
        Rec r = roundTrip(new Rec("abc", 7));
        System.out.println(r);
    }
}
