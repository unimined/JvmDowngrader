package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.util.ServiceLoader;

/**
 * ServiceLoader SPI: {@code META-INF/services/jvmdg.featuretests.manual.Spi$Contract}
 * ships in the corpus jar and lists two providers. Iterating the loader
 * exercises the service-config resource path plus provider class-load/init
 * inside downgraded user code — distinct from a plain getResource.
 */
public class Spi {

    public interface Contract {
        String who();
    }

    public static class Impl implements Contract {
        public Impl() {
        }

        @Override
        public String who() {
            return "spiImpl";
        }
    }

    public static class Impl2 implements Contract {
        public Impl2() {
        }

        @Override
        public String who() {
            return "spiImpl2";
        }
    }

    public static void main(String[] args) {
        Support.run(Spi.class);
    }

    @FeatureTest
    static void load() {
        // file order determines iteration order -> deterministic output
        for (Contract c : ServiceLoader.load(Contract.class)) {
            System.out.println(c.getClass().getSimpleName() + ":" + c.who());
        }
    }

    @FeatureTest
    static void loadTyped() {
        // explicit-classloader load — independent of TCCL, covers the same
        // resource+init path through a different entry point
        ServiceLoader<Contract> loader = ServiceLoader.load(Contract.class, Spi.class.getClassLoader());
        System.out.println(loader.iterator().hasNext());
        for (Contract c : loader) {
            System.out.println(c.who());
        }
    }
}
