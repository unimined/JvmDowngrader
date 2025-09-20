package xyz.wagyourtail.downgradetest;

import java.util.concurrent.Semaphore;

public class TestScopedValue {

    static void main() {
        var value1 = ScopedValue.newInstance();
        var value2 = ScopedValue.newInstance();

        var scope = ScopedValue.where(value1, "1").where(value2, "2").call(() -> {

            System.out.println(value1.get());
            ScopedValue.where(value1, "3").run(() -> {
                System.out.println(value1.get());
                System.out.println(value2.get());
            });
            System.out.println(value1.get());

            return ScopedValue.where(value1, "5");
        });

        scope.run(() -> {
            System.out.println(value1.orElse("4"));
            System.out.println(value2.orElse("4"));

            Semaphore sem = new Semaphore(0);
            new Thread(() -> {
                System.out.println(value1.orElse("7"));
                System.out.println(value2.orElse("7"));
                sem.release();
            }).start();
            try {
                sem.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        });

        System.out.println(value1.orElse("6"));

    }

}
