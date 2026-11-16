package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

/**
 * Constant-dynamic (condy): ldc condy and condy-in-indy-bsArgs can only be
 * produced by bytecode weaving — javac never emits them. The j11 transform
 * rewrites ldc condy into indy via the ldcCondyToIndy bootstrap and nested
 * condy via nestedCondyInIndy.
 *
 * <p>The woven methods live in {@code CondyWoven}, a class emitted directly
 * by {@code FeatureCorpusGenerator} at build time (no annotation weaving).
 */
public class Condy {

    public static void main(String[] args) {
        Support.run(Condy.class);
    }

    @FeatureTest
    static void all() {
        // ldc-condy + nested condy-in-bsArgs call sites live in CondyWoven —
        // exercises the j11 condy→indy rewrites end-to-end
        CondyWoven.testExplicitCast();
        CondyWoven.testNestedCondy();
    }

    enum Type {
        VALUE_A,
        VALUE_B
    }

}
