package xyz.wagyourtail.downgradetest;

@Deprecated(forRemoval = true, since = "1.0")
public class TestDeprecated {

    @Deprecated(since = "1.1")
    public void test() {
    }

    static void main() throws NoSuchMethodException {
        Deprecated dep = TestDeprecated.class.getAnnotation(Deprecated.class);
        System.out.println(dep.since());
        System.out.println(dep.forRemoval());

        Deprecated dep2 = TestDeprecated.class.getMethod("test").getAnnotation(Deprecated.class);
        System.out.println(dep2.since());
        System.out.println(dep2.forRemoval());
    }

}
