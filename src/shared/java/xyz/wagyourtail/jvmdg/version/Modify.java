package xyz.wagyourtail.jvmdg.version;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Modify {

    Class<?>[] MODIFY_SIG = new Class<?>[]{MethodNode.class, int.class, ClassNode.class, Set.class};

    Ref ref() default @Ref("");

}