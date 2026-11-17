package xyz.wagyourtail.jvmdg.asm.index;

import org.objectweb.asm.tree.ClassNode;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * A {@link Set} of {@link ClassNode}s that also offers O(1) lookup by
 * internal name — replaces the linear scans over {@code extra} in the stub
 * resolvers. Backed by a {@link TreeMap} on {@link ClassNode#name}, so
 * iteration is deterministic (name order) and lookups are by name.
 *
 * <p>Set semantics: adding a second node with a name already present is a
 * no-op (like {@code TreeSet.add} with the same comparator result).
 */
public class WorkingClasses extends AbstractSet<ClassNode> {

    private final TreeMap<String, ClassNode> byName = new TreeMap<>();

    public ClassNode get(String internalName) {
        return byName.get(internalName);
    }

    @Override
    public boolean add(ClassNode node) {
        if (byName.containsKey(node.name)) {
            return false;
        }
        byName.put(node.name, node);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof ClassNode)) {
            return false;
        }
        return byName.remove(((ClassNode) o).name) != null;
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof ClassNode && byName.containsKey(((ClassNode) o).name);
    }

    @Override
    public Iterator<ClassNode> iterator() {
        return byName.values().iterator();
    }

    @Override
    public int size() {
        return byName.size();
    }

    @Override
    public void clear() {
        byName.clear();
    }

}
