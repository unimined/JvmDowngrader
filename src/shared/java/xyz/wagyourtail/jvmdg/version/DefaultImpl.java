package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code public static} method as a fallback implementation for an
 * interface member that some implementor may not declare at the target
 * version. The annotated body is materialized into every class that
 * (transitively) implements the interface without declaring the member, so a
 * callsite compiled against the newer interface shape can never hit
 * {@code AbstractMethodError}.
 * <p>
 * <b>Why it exists:</b> at the source version the member's body lives on the
 * interface (a {@code default} method) or is guaranteed by implementors that
 * were compiled against it. At the target version that guarantee disappears
 * in two ways:
 * <ul>
 *   <li><b>default methods below v52</b> — interface bodies are illegal, so a
 *     {@code default} method degrades to an abstract member and every
 *     implementor needs the body materialized (Java 8 {@code java.util.function}
 *     interfaces → Java 7).</li>
 *   <li><b>methods that gained a default later</b> — the member is a normal
 *     abstract interface method at the target version and always was, but
 *     implementors compiled against the older interface were never required
 *     to declare it (Java 9 {@code Path} gained defaults for {@code toFile},
 *     {@code iterator}, {@code resolve}, … that are abstract on Java 8).</li>
 * </ul>
 * In both cases the callsite still invokes the member through the interface
 * and the missing body has to come from somewhere — this annotation supplies
 * it.
 * <p>
 * <b>What it does to bytecode:</b>
 * <ul>
 *   <li>{@code INVOKEINTERFACE}/{@code INVOKEVIRTUAL} call sites are left
 *     untouched — the member stays an ordinary interface call, resolved at
 *     runtime by...</li>
 *   <li>...a {@code public} forwarding method inserted into each implementor
 *     lacking it ({@code aload_0}, args, {@code INVOKESTATIC} of this method,
 *     typed return) during {@code insertAbstractMethods}.</li>
 *   <li>{@code INVOKESPECIAL} call sites ({@code Interface.super.m()} — the
 *     only shape that names the interface body directly) are rewritten to an
 *     {@code INVOKESTATIC} of this method, since no interface body survives.</li>
 * </ul>
 * <p>
 * <b>Where to use it:</b> on a {@code public static} method inside a class
 * registered via {@code stub(Class)} for the interface being downgraded (e.g.
 * the {@code *Defaults} holder nested in a {@code @Adapter} interface). The
 * signature is the interface method's signature with the receiver prepended:
 * {@code andThen(Consumer after)} stubs as
 * {@code static J_U_F_Consumer andThen(J_U_F_Consumer self, J_U_F_Consumer after)}.
 * {@code @Coerce}/{@code @Adapter}-annotated params and return types register
 * as the real JDK types, as with {@link Stub}. {@link #ref} retargets the
 * registration at another owner/member (e.g. a {@code Spliterator$OfInt}
 * default implemented once on the outer stub class).
 * <p>
 * <b>Contrast:</b> {@link Stub} redirects call sites to the stub and inserts
 * nothing into implementors — use it when the target member exists on the
 * target runtime and only needs a different implementation invoked. A
 * {@code <init>} target here is meaningless and rejected at registration;
 * use a ctor {@link Stub#init() retarget} or {@code @Modify} instead.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultImpl {

    /**
     * reference to the interface member this implements, if it can't be
     * auto-resolved from the method name and receiver-param signature.
     */
    Ref ref() default @Ref();

}
