package xyz.wagyourtail.downgradetest;

import com.sun.net.httpserver.Filter;

public class TestFilter {

    public static void main(String[] args) {
        System.out.println(Filter.afterHandler("test", (e) -> System.out.println("test")).description());
    }

}
