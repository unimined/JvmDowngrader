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

    /**
     * MethodNode - current method
     * int - current insn index
     * ClassNode - current class
     * Set&lt;ClassNode&gt; - extra classes
     * boolean - if synthetic should be set on any generated methods
     */
    Class<?>[] MODIFY_SIG = new Class<?>[]{MethodNode.class, int.class, ClassNode.class, Set.class, boolean.class};

    Ref ref() default @Ref("");

}