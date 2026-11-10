package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.util.concurrent.Semaphore;

/**
 * ScopedValue (j25 final): where/call/run nesting, rebinding inside a scope,
 * and the no-inheritance-across-threads guarantee.
 */
public class ScopedValues {

    public static void main(String[] args) {
        Support.run(ScopedValues.class);
    }

    @FeatureTest
    static void scopes() {
        // ScopedValue.where/call/run nesting, rebinding, Carrier return,
        // orElse defaults — and no inheritance across a spawned thread
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
