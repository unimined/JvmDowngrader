package xyz.wagyourtail.jvmdg.j10.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.management.RuntimeMXBean;

public class J_L_M_RuntimeMXBean {

    @Stub
    public static long getPid(RuntimeMXBean bean) {
        String name = bean.getName();
        name = name.substring(0, name.indexOf('@'));
        return Long.parseLong(name);
    }

}
