package xyz.wagyourtail.downgradetest;

import java.util.*;

public class TestCollection {

    public static void main(String[] args) {
        System.out.println(List.copyOf(List.of(1, 2, 3)));
        System.out.println(Set.of(3));
        System.out.println(Map.ofEntries(Map.entry("a", 1)));

        // java 21, SequencedCollection tests
        LinkedHashSet<Integer> set = new LinkedHashSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        System.out.println(set);
        Set<Integer> revSet = set.reversed();
        System.out.println(revSet);
        revSet.add(4);
        System.out.println(set);
        System.out.println(revSet);

        List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        System.out.println(list);
        List<Integer> revList = list.reversed();
        System.out.println(revList);
        revList.add(4);
        System.out.println(list);
        System.out.println(revList);

        Deque<Integer> deque = new ArrayDeque<>();
        deque.add(1);
        deque.add(2);
        deque.add(3);
        System.out.println(deque);
        Deque<Integer> revDeque = deque.reversed();
        System.out.println(revDeque);
        revDeque.add(4);
        System.out.println(deque);
        System.out.println(revDeque);
    }

}
