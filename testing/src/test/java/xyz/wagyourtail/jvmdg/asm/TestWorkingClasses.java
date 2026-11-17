package xyz.wagyourtail.jvmdg.asm;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.asm.index.WorkingClasses;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class TestWorkingClasses {

    private static ClassNode named(String name) {
        ClassNode node = new ClassNode();
        node.name = name;
        return node;
    }

    @Test
    public void iteratesInNameOrder() {
        WorkingClasses set = new WorkingClasses();
        set.add(named("z/C"));
        set.add(named("a/B"));
        set.add(named("m/D"));
        Iterator<ClassNode> it = set.iterator();
        assertEquals("a/B", it.next().name);
        assertEquals("m/D", it.next().name);
        assertEquals("z/C", it.next().name);
        assertFalse(it.hasNext());
    }

    @Test
    public void duplicateNameKeepsFirst() {
        WorkingClasses set = new WorkingClasses();
        ClassNode first = named("a/B");
        ClassNode second = named("a/B");
        assertTrue(set.add(first));
        assertFalse(set.add(second));
        assertSame(first, set.get("a/B"));
        assertEquals(1, set.size());
    }

    @Test
    public void setSemanticsHold() {
        WorkingClasses set = new WorkingClasses();
        ClassNode node = named("a/B");
        set.add(node);
        assertTrue(set.contains(node));
        assertFalse(set.contains(named("a/C")));
        assertEquals(node, set.get("a/B"));
        assertNull(set.get("a/C"));
        assertTrue(set.remove(node));
        assertFalse(set.contains(node));
        set.add(node);
        set.clear();
        assertTrue(set.isEmpty());
    }

}
