package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

@Adapter("java/lang/constant/DynamicConstantDesc")
public abstract class J_L_C_DynamicConstantDesc<T> implements J_L_C_ConstantDesc {
    private final J_L_C_DirectMethodHandleDesc bsm;
    private final String constantName;
    private final J_L_C_ClassDesc constantType;
    private final J_L_C_ConstantDesc[] bsmArgs;

    protected J_L_C_DynamicConstantDesc(
        J_L_C_DirectMethodHandleDesc bsm,
        String constantName,
        J_L_C_ClassDesc constantType,
        J_L_C_ConstantDesc... bsmArgs
    ) {
        this.bsm = Objects.requireNonNull(bsm);
        this.constantName = Objects.requireNonNull(constantName);
        this.constantType = Objects.requireNonNull(constantType);
        this.bsmArgs = Objects.requireNonNull(bsmArgs).clone();
    }

    public static <T> J_L_C_ConstantDesc ofCanonical(
        J_L_C_DirectMethodHandleDesc bsm,
        String constantName,
        J_L_C_ClassDesc constantType,
        J_L_C_ConstantDesc... bsmArgs
    ) {
        return ofNamed(bsm, constantName, constantType, bsmArgs);
    }

    public static <T> J_L_C_DynamicConstantDesc<T> ofNamed(
        J_L_C_DirectMethodHandleDesc bsm,
        String constantName,
        J_L_C_ClassDesc constantType,
        J_L_C_ConstantDesc... bsmArgs
    ) {
        return new J_L_C_DynamicConstantDesc<>(bsm, constantName, constantType, bsmArgs) {
        };
    }

    public static <T> J_L_C_DynamicConstantDesc<T> of(J_L_C_DirectMethodHandleDesc bsm, J_L_C_ConstantDesc[] args) {
        return ofNamed(bsm, "_", bsm.invocationType().returnType(), args);
    }

    public static <T> J_L_C_DynamicConstantDesc<T> of(J_L_C_DirectMethodHandleDesc bsm) {
        return of(bsm);
    }

    public String constantName() {
        return constantName;
    }

    public J_L_C_ClassDesc constantType() {
        return constantType;
    }

    public J_L_C_DirectMethodHandleDesc bootstrapMethod() {
        return bsm;
    }

    public J_L_C_ConstantDesc[] bootstrapArgs() {
        return bsmArgs.clone();
    }

    public List<J_L_C_ConstantDesc> bootstrapArgsList() {
        return List.of(bsmArgs);
    }

    public T resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        try {
            MethodHandle h = (MethodHandle) bsm.resolveConstantDesc(lookup);
            if (h.type().parameterCount() < 2 || !MethodHandles.Lookup.class.isAssignableFrom(h.type().parameterType(0))) {
                throw new BootstrapMethodError(
                    "Invalid bsm declared: " + bsm
                );
            }
            Object[] args = new Object[bsmArgs.length + 3];
            args[0] = lookup;
            args[1] = constantName;
            args[2] = constantType.resolveConstantDesc(lookup);
            for (int i = 0; i < bsmArgs.length; i++) {
                args[i + 3] = bsmArgs[i].resolveConstantDesc(lookup);
            }
            return (T) h.invokeWithArguments(args);
        } catch (Throwable e) {
            if (e instanceof Error) {
                throw (Error) e;
            }
            throw new BootstrapMethodError(e);
        }
    }


}
