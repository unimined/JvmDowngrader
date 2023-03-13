package xyz.wagyourtail.jvmdg;

public class Pair<T, U> {

    final T first;
    final U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }
}
