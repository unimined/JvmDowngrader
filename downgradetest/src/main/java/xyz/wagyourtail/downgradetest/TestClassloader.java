package xyz.wagyourtail.downgradetest;

import java.net.URLClassLoader;

public class TestClassloader {

    public static void main(String[] args) {
        URLClassLoader cl = new URLClassLoader("name1", new java.net.URL[0], TestClassloader.class.getClassLoader());
        System.out.println(cl.getName());

        URLClassLoader cl2 = new URLClassLoader("name2", new java.net.URL[0], TestClassloader.class.getClassLoader(), null);
        System.out.println(cl2.getName());

        CLExt cl3 = new CLExt(TestClassloader.class.getClassLoader());
        System.out.println(cl3.getName());

    }

    private static class CLExt extends ClassLoader {

        public CLExt(ClassLoader parent) {
            super("name3", parent);
        }

    }

}
