package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.net.URLClassLoader;

/**
 * ClassLoader forms: named URLClassLoaders, a custom ClassLoader with a named
 * parent, getName() reflection — exercises j9 ClassLoader names on downgraded
 * runtimes.
 */
public class ClassLoading {

    public static void main(String[] args) {
        Support.run(ClassLoading.class);
    }

    @FeatureTest
    static void named() {
        // named classloaders: URLClassLoader name ctors + a subclass with a
        // named parent — exercises the j9 ClassLoader.getName surface
        URLClassLoader cl = new URLClassLoader("name1", new java.net.URL[0], ClassLoading.class.getClassLoader());
        System.out.println(cl.getName());

        URLClassLoader cl2 = new URLClassLoader("name2", new java.net.URL[0], ClassLoading.class.getClassLoader(), null);
        System.out.println(cl2.getName());

        CLExt cl3 = new CLExt(ClassLoading.class.getClassLoader());
        System.out.println(cl3.getName());
    }

    @FeatureTest
    static void delegation() throws ClassNotFoundException {
        // parent delegation: java.lang classes resolve through our loader
        CLExt cl = new CLExt(ClassLoading.class.getClassLoader());
        System.out.println(cl.loadClass("java.lang.String").getName());
        // corpus classes aren't visible through an empty-child loader
        try {
            cl.loadClass("jvmdg.featuretests.manual.ClassLoading");
            System.out.println("found?");
        } catch (ClassNotFoundException e) {
            System.out.println("cnfe");
        }
    }

    private static class CLExt extends ClassLoader {

        public CLExt(ClassLoader parent) {
            super("name3", parent);
        }

    }

}
