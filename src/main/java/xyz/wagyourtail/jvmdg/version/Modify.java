package xyz.wagyourtail.jvmdg.version;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.*;
import java.util.Set;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Modify {

    Class<?>[] MODIFY_SIG = new Class<?>[] { MethodNode.class, int.class, ClassNode.class, Set.class };

    int javaVersion();

    Ref ref() default @Ref("");

}