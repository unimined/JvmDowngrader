package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@JEP(277)
@Adapter("java/lang/Deprecated")
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface J_L_Deprecated {

    boolean forRemoval() default false;
    String since() default "";

}
