package xyz.wagyourtail.jvmdg.j11.stub.java_net_http;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;

@Adapter("Ljava/net/http/HttpHeaders;")
public class J_N_H_HttpHeaders {
    Map<String, List<String>> headers;

    public J_N_H_HttpHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public static J_N_H_HttpHeaders of(Map<String, List<String>> map, BiPredicate<String, String> filter) {
        Map<String, List<String>> filtered = new TreeMap<>();
        for (Map.Entry<String, List<String>> e : map.entrySet()) {
            String key = e.getKey().trim();
            if (key.isEmpty()) {
                throw new IllegalArgumentException("empty key");
            }
            List<String> adding = new ArrayList<>();
            for (String value : e.getValue()) {
                if (value == null) {
                    throw new IllegalArgumentException("null value for key " + key);
                }
                if (filter.test(key, value)) {
                    adding.add(value);
                }
            }
            if (!adding.isEmpty()) {
                if (filtered.put(key.toLowerCase(Locale.ROOT), adding) != null) {
                    throw new IllegalArgumentException("duplicate key: " + key);
                }
            }
        }
        return new J_N_H_HttpHeaders(Map.copyOf(filtered));
    }

    public Optional<String> firstValue(String name) {
        return headers.containsKey(name) ? Optional.of(headers.get(name).get(0)) : Optional.empty();
    }

    public OptionalLong firstValueAsLong(String name) {
        return headers.containsKey(name) ? OptionalLong.of(Long.parseLong(headers.get(name).get(0))) : OptionalLong.empty();
    }

    public List<String> allValues(String name) {
        return headers.get(name);
    }

    public Map<String, List<String>> map() {
        return headers;
    }

    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof J_N_H_HttpHeaders)) {
            return false;
        } else {
            J_N_H_HttpHeaders that = (J_N_H_HttpHeaders)o;
            return this.headers.equals(that.headers);
        }
    }

    public final int hashCode() {
        int h = 0;
        for (Map.Entry<String, List<String>> e : map().entrySet()) {
            h += entryHash(e);
        }
        return h;
    }

    @Override
    public String toString() {
        return super.toString() + " { " + headers + " }";
    }

    private static int entryHash(Map.Entry<String, List<String>> e) {
        String key = e.getKey();
        List<String> value = e.getValue();
        // we know that by construction key and values can't be null
        int keyHash = key.toLowerCase(Locale.ROOT).hashCode();
        int valueHash = value.hashCode();
        return keyHash ^ valueHash;
    }


}
