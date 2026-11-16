package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * ProcessHandle introspection + child enumeration. Runs on the batch driver's
 * sequential phase (marked SEQUENTIAL in DowngradeTests / "--seqmain" in
 * cli.Main) — enumerating children must not observe sibling mains' processes.
 */
public class ProcessTests {

    public static void main(String[] args) {
        Support.run(ProcessTests.class);
    }

    @FeatureTest
    static void self() throws IOException, InterruptedException, ExecutionException {
        // ProcessHandle.current/children/onExit + argv tail (the 4KB /proc
        // read quirk) — sequential only: children() must not see sibling mains
        ProcessHandle currentProcessHandle = ProcessHandle.current();
        System.out.println(currentProcessHandle.pid() == ManagementFactory.getRuntimeMXBean().getPid());
        List<String> lst = Arrays.asList(currentProcessHandle.info().arguments().orElse(new String[]{"aaa", "bbb", "ccc", "ddd"}));
        System.out.println(lst.subList(lst.size() - 3, lst.size()));

        ProcessBuilder pb;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            pb = new ProcessBuilder("timeout", "5");
        } else {
            pb = new ProcessBuilder("sleep", "5");
        }
        Process p = pb.start();
        currentProcessHandle.children().map(e -> String.join(" ", e.info().arguments().orElse(new String[0]))).forEach(System.out::println);
        System.out.println(p.toHandle().onExit().get().info().commandLine().orElse("missing"));
    }
}
