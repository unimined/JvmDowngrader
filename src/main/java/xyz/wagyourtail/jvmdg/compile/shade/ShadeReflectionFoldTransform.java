package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.config.ShadeReflection;
import xyz.wagyourtail.jvmdg.pipeline.ClassTransform;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Constant-folds literal class names feeding {@code Class.forName(String…)}
 * and {@code Lookup.findClass(String)} through the {@code reflection.idx}
 * type rows: {@code ldc "java.x.Y"} becomes the row's shaded target literal
 * (e.g. {@code "shaded.xyz.J_X_Y"}) when that target made the closure. The
 * call itself is untouched — the fold only rewrites the name argument, so
 * init/classloader semantics are identical to the table-lookup path.
 *
 * <p>Runs after {@code shade:prefix} since the folded name carries the
 * position's prefix remap. A row whose impl didn't make the closure folds
 * to nothing — the literal stays and the lookup degrades natively, the
 * same miss=native contract the pruning gives. Skipped entirely under
 * {@code Shade-Reflection: none} (a folded name would surface the shaded
 * class name in the CNFE instead of the requested one).</p>
 *
 * <p>An {@code ldc} folds only when it is the *nearest* string source
 * feeding the next reflection call — a same-method {@code ldc} of the same
 * literal for unrelated use stays untouched.</p>
 */
public class ShadeReflectionFoldTransform implements ClassTransform {

    /** how far past an {@code ldc} a consuming call may sit (extra args interleave) */
    private static final int LOOKAHEAD = 6;

    private final ShadeContext context;

    /** position → dotted source name → dotted shaded target name */
    private final Map<Integer, Map<String, String>> foldMaps = new HashMap<Integer, Map<String, String>>();

    public ShadeReflectionFoldTransform(ShadeContext context) {
        this.context = context;
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) {
        if (ctx.flags.shadeReflection == ShadeReflection.NONE) {
            return node;
        }
        int position = context.positionOf(node, ctx.flags);
        Map<String, String> fold = foldMap(position);
        if (fold.isEmpty()) {
            return node;
        }
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof LdcInsnNode)) {
                    continue;
                }
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (!(ldc.cst instanceof String)) {
                    continue;
                }
                String folded = fold.get(ldc.cst);
                if (folded != null && isNearestNameSource(ldc)) {
                    ldc.cst = folded;
                }
            }
        }
        return node;
    }

    /**
     * The ldc is a fold candidate only when the next reflection-dispatch
     * call follows it with no other string {@code ldc} in between — the
     * last string source wins the argument slot, so an interleaved literal
     * means our ldc feeds something else.
     */
    private static boolean isNearestNameSource(LdcInsnNode ldc) {
        int budget = LOOKAHEAD;
        for (AbstractInsnNode insn = ldc.getNext(); insn != null && budget-- > 0; insn = insn.getNext()) {
            if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String) {
                return false;
            }
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("java/lang/Class".equals(call.owner) && "forName".equals(call.name)) {
                    return true;
                }
                return "java/lang/invoke/MethodHandles$Lookup".equals(call.owner)
                    && "findClass".equals(call.name);
            }
        }
        return false;
    }

    private Map<String, String> foldMap(int position) {
        Map<String, String> cached = foldMaps.get(position);
        if (cached == null) {
            cached = buildFoldMap(position);
            foldMaps.put(position, cached);
        }
        return cached;
    }

    /**
     * T-rows → {@code source-dotted → target-dotted-after-prefix}. Rows
     * whose impl isn't in the slot's remap (pruned or never rooted) simply
     * have no fold entry — their literals fall back to the table/native.
     */
    private Map<String, String> buildFoldMap(int position) {
        SimpleRemapper remapper = context.remappers.get(position);
        if (remapper == null) {
            return new HashMap<String, String>();
        }
        Map<String, String> out = new HashMap<String, String>();
        try {
            for (ShadeClosureBarrier.ReflRow row : ShadeClosureBarrier.loadReflRows(context.apiRoots)) {
                if (row.owner.equals(row.matchName) && row.impls.size() == 1) {
                    // T row: source type → its stub target
                    String target = remapper.map(row.impls.get(0).getOwner().getInternalName());
                    if (target != null) {
                        out.put(row.owner.replace('/', '.'), target.replace('/', '.'));
                    }
                }
            }
        } catch (IOException e) {
            // a missing/unreadable table just means no folds — the calls fall
            // back to runtime lookup exactly like a row miss
        }
        return out;
    }

    @Override
    public String toString() {
        return "shade:reflection-fold";
    }
}
