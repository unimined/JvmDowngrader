package xyz.wagyourtail.jvmdg.test;

import java.lang.reflect.InvocationTargetException;

public class Bootstrap {

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        Class.forName(args[0]).getMethod("main", String[].class).invoke(null, (Object) newArgs);
    }
}
