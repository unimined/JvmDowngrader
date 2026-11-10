package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.lang.reflect.Proxy;

/**
 * java.lang.reflect.Proxy: a runtime-generated class implementing a corpus
 * interface — InvocationHandler dispatch, Object methods routed through the
 * handler, and default interface methods (which also route through the
 * handler rather than executing the default body).
 */
public class Proxies {

    public interface Contract {
        String hello(String name);

        default String helloDefault() {
            return "default:" + hello("d");
        }
    }

    private static Contract proxy() {
        return (Contract) Proxy.newProxyInstance(Proxies.class.getClassLoader(),
            new Class<?>[]{Contract.class},
            (proxy, method, args) -> "handled:" + method.getName() + ":" + (args == null ? "" : args[0]));
    }

    public static void main(String[] args) {
        Support.run(Proxies.class);
    }

    @FeatureTest
    static void dispatch() {
        // abstract + Object methods (toString) all route through the handler;
        // isProxyClass recognizes the runtime-generated class
        Contract p = proxy();
        System.out.println(p.hello("a"));
        System.out.println(p.toString());
        System.out.println(p.getClass().isInterface());
        System.out.println(Proxy.isProxyClass(p.getClass()));
    }

    @FeatureTest
    static void defaultMethod() {
        // default methods are routed through the handler too — the proxy does
        // NOT execute the interface's default body
        System.out.println(proxy().helloDefault());
    }
}
