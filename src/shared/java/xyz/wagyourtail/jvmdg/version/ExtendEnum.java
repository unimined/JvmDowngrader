package xyz.wagyourtail.jvmdg.version;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a fake enum as a runtime extension of a JDK enum missing a constant
 * (e.g. {@code StandardProtocolFamily.UNIX}). Registration then wires:
 * <ul>
 *   <li>a field stub for the constant's {@code GETSTATIC}, dispatched to a
 *       static getter on this class conventionally named after the lowercased
 *       constant ({@code UNIX} &rarr; {@code unix()}) and declared to return
 *       the real enum type;</li>
 *   <li>inject-then-delegate shadows for the enum-universe readers
 *       ({@code values}, {@code valueOf}, {@code Enum.valueOf},
 *       {@code Class.getEnumConstants}, {@code EnumSet.noneOf}/{@code allOf},
 *       {@code EnumMap(Class)}) so the injected constant is installed before
 *       any covered reader can observe the stale universe.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtendEnum {

    /**
     * reference to the missing enum constant: owner = the real enum,
     * member = the constant name, desc = the real enum descriptor.
     */
    Ref value();

}
