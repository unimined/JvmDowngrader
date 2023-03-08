package xyz.wagyourtail.jvmdg.replace;

import org.objectweb.asm.tree.AnnotationNode;
import xyz.wagyourtail.jvmdg.Ref;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Replace {

    int javaVersion();

    Ref ref() default @Ref("");

    boolean idBSM() default false;

}