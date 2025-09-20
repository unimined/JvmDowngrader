package xyz.wagyourtail.downgradetest;

public class TestScopedValue {

    static void main() {
        var value1 = ScopedValue.newInstance();
        var value2 = ScopedValue.newInstance();

        ScopedValue.where(value1, "1").where(value2, "2").run(() -> {

            System.out.println(value1.get());
            ScopedValue.where(value1, "3").run(() -> {
                System.out.println(value1.get());
                System.out.println(value2.get());
            });
            System.out.println(value1.get());


        });

        System.out.println(value1.orElse("4"));

    }

}
