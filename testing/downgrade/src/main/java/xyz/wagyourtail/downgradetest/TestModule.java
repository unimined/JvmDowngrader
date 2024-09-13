package xyz.wagyourtail.downgradetest;

public class TestModule {
    public static void main(String[] args) {
        if (Object.class.getModule().getLayer().findModule("java.base").orElseThrow() != Object.class.getModule()) {
            throw new RuntimeException("Failing module implementation");
        }
    }
}
