package xyz.wagyourtail.downgradetest;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestCollection {

    public static void main(String[] args) {
        System.out.println(List.copyOf(List.of(1, 2, 3)));
        System.out.println(Set.of(3));
        System.out.println(Map.ofEntries(Map.entry("a", 1)));
    }

}
