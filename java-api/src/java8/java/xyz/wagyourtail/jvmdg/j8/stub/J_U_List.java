package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_UnaryOperator;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.*;

public class J_U_List {

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/List"), subtypes = true)
    public static <T> void replaceAll(List<T> self, J_U_F_UnaryOperator<T> operator) {
        Objects.requireNonNull(operator);
        final ListIterator<?> li = self.listIterator();
        while (li.hasNext()) {
            li.set(operator.apply(li.next()));
        }
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/List"), subtypes = true)
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
