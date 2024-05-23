package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_UnaryOperator;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.*;

public class J_U_List {

    @Stub(ref = @Ref("java/util/List"))
    public static <T> void replaceAll(List<T> self, J_U_F_UnaryOperator<T> operator) {
        Objects.requireNonNull(operator);
        final ListIterator<T> li = self.listIterator();
        while (li.hasNext()) {
            li.set(operator.apply(li.next()));
        }
    }

    @Stub(ref = @Ref("java/util/List"))
    public static <T> void sort(List<T> self, Comparator<T> c) {
        Object[] a = self.toArray();
        Arrays.sort(a, (Comparator) c);
        ListIterator<T> i = self.listIterator();
        for (Object e : a) {
            i.next();
            i.set((T) e);
        }
    }

    // spliterator handled by collection

}
