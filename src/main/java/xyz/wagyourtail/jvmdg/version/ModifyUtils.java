package xyz.wagyourtail.jvmdg.version;

import java.lang.reflect.Method;

/**
 * Helpers for {@link Modify} methods.
 */
public final class ModifyUtils {

    /**
     * The invocable {@code @Modify} signature: {@link Modify#MODIFY_SIG} plus
     * a trailing {@link ModifyContext}. Handlers declare any subsequence of
     * these parameter types.
     */
    public static final Class<?>[] MODIFY_SIG;

    static {
        MODIFY_SIG = new Class<?>[Modify.MODIFY_SIG.length + 1];
        System.arraycopy(Modify.MODIFY_SIG, 0, MODIFY_SIG, 0, Modify.MODIFY_SIG.length);
        MODIFY_SIG[MODIFY_SIG.length - 1] = ModifyContext.class;
    }

    private ModifyUtils() {
    }

    /**
     * Subsequence-matches a {@code @Modify} method's parameter types against
     * {@link #MODIFY_SIG}: each declared parameter must match the next
     * available signature entry in order. Returns, for each parameter, the
     * index of the {@code MODIFY_SIG} argument it binds to.
     *
     * @throws IllegalArgumentException if a parameter can't be matched
     */
    public static int[] matchModifyParams(Method method) {
        Class<?>[] params = method.getParameterTypes();
        int[] indices = new int[params.length];
        for (int i = 0, j = 0; i < params.length; i++, j++) {
            if (i >= MODIFY_SIG.length) {
                throw new IllegalArgumentException("@Modify method " + method + " has too many parameters");
            }
            while (j < MODIFY_SIG.length && params[i] != MODIFY_SIG[j]) {
                ++j;
            }
            if (j >= MODIFY_SIG.length) {
                throw new IllegalArgumentException("@Modify method " + method + " parameter " + i + " must be of type " + MODIFY_SIG[i].getName());
            }
            indices[i] = j;
        }
        return indices;
    }
}
