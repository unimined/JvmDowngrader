package xyz.wagyourtail.jvmdg.version;

import java.lang.reflect.Method;

/**
 * Helpers for {@link Modify} methods.
 */
public final class ModifyUtils {

    private ModifyUtils() {
    }

    /**
     * Subsequence-matches a {@code @Modify} method's parameter types against
     * {@link Modify#MODIFY_SIG}: each declared parameter must match the next
     * available signature entry in order. Returns, for each parameter, the
     * index of the {@code MODIFY_SIG} argument it binds to.
     *
     * @throws IllegalArgumentException if a parameter can't be matched
     */
    public static int[] matchModifyParams(Method method) {
        Class<?>[] params = method.getParameterTypes();
        int[] indices = new int[params.length];
        for (int i = 0, j = 0; i < params.length; i++, j++) {
            if (i >= Modify.MODIFY_SIG.length) {
                throw new IllegalArgumentException("@Modify method " + method + " has too many parameters");
            }
            while (j < Modify.MODIFY_SIG.length && params[i] != Modify.MODIFY_SIG[j]) {
                ++j;
            }
            if (j >= Modify.MODIFY_SIG.length) {
                throw new IllegalArgumentException("@Modify method " + method + " parameter " + i + " must be of type " + Modify.MODIFY_SIG[i].getName());
            }
            indices[i] = j;
        }
        return indices;
    }
}
