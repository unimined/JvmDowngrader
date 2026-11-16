package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

/**
 * Module introspection: getModule/getLayer/findModule — the j9 Module stub
 * must report the unnamed/boot layer consistently.
 */
public class Modules {

    public static void main(String[] args) {
        Support.run(Modules.class);
    }

    @FeatureTest
    static void moduleOf() {
        // getModule/getLayer/findModule round-trip — the j9 Module stub must
        // resolve java.base consistently through the layer
        if (Object.class.getModule().getLayer().findModule("java.base").orElseThrow() != Object.class.getModule()) {
            throw new RuntimeException("Failing module implementation");
        }
        System.out.println("ok");
    }

    @FeatureTest
    static void named() {
        // isNamed/getName on java.base — stub must report a named module
        System.out.println(Object.class.getModule().isNamed());
        System.out.println(Object.class.getModule().getName());
    }
}
