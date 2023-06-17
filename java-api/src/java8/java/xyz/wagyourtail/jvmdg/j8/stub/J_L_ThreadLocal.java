package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Supplier;

public class J_L_ThreadLocal {

    public static <S> ThreadLocal<S> withInitial(J_U_F_Supplier<? extends S> supplier) {
        ThreadLocal l =  new ThreadLocal<>();
        l.set(supplier.get());
        return l;
    }

}
