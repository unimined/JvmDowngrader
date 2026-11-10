package xyz.wagyourtail.jvmdg.collection;

import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jvmdg.util.Function;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class TestEnumerations {

    private static <T> Enumeration<T> enumeration(T... items) {
        return Collections.enumeration(Arrays.asList(items));
    }

    @Test
    public void mapEnumerationAppliesMapper() {
        MapEnumeration<Integer, String> e = new MapEnumeration<Integer, String>(enumeration(1, 2, 3),
            new Function<Integer, String>() {
                @Override
                public String apply(Integer integer) {
                    return "v" + integer;
                }
            });
        assertEquals(Arrays.asList("v1", "v2", "v3"), Collections.list(e));
    }

    @Test
    public void mapEnumerationThrowsAfterExhaustion() {
        MapEnumeration<Integer, Integer> e = new MapEnumeration<Integer, Integer>(enumeration(1),
            new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer integer) {
                    return integer;
                }
            });
        assertEquals(Integer.valueOf(1), e.nextElement());
        assertFalse(e.hasMoreElements());
        assertThrows(NoSuchElementException.class, e::nextElement,
            "Enumeration contract: nextElement after exhaustion must throw, not return null");
    }

    @Test
    public void flatMapEnumerationFlattens() {
        FlatMapEnumeration<List<Integer>, Integer> e = new FlatMapEnumeration<List<Integer>, Integer>(
            enumeration(Arrays.asList(1, 2), Collections.<Integer>emptyList(), Arrays.asList(3)),
            new Function<List<Integer>, Enumeration<Integer>>() {
                @Override
                public Enumeration<Integer> apply(List<Integer> ints) {
                    return Collections.enumeration(ints);
                }
            });
        assertEquals(Arrays.asList(1, 2, 3), Collections.list(e));
    }

    @Test
    public void flatMapEnumerationThrowsAfterExhaustion() {
        FlatMapEnumeration<List<Integer>, Integer> e = new FlatMapEnumeration<List<Integer>, Integer>(
            enumeration(Collections.singletonList(1)),
            new Function<List<Integer>, Enumeration<Integer>>() {
                @Override
                public Enumeration<Integer> apply(List<Integer> ints) {
                    return Collections.enumeration(ints);
                }
            });
        assertEquals(Integer.valueOf(1), e.nextElement());
        assertThrows(NoSuchElementException.class, e::nextElement);
    }

    @Test
    public void flatMapEnumerationEmptyInnerIsSkipped() {
        FlatMapEnumeration<List<Integer>, Integer> e = new FlatMapEnumeration<List<Integer>, Integer>(
            enumeration(Collections.<Integer>emptyList(), Collections.<Integer>emptyList()),
            new Function<List<Integer>, Enumeration<Integer>>() {
                @Override
                public Enumeration<Integer> apply(List<Integer> ints) {
                    return Collections.enumeration(ints);
                }
            });
        assertFalse(e.hasMoreElements());
        assertThrows(NoSuchElementException.class, e::nextElement);
    }
}
