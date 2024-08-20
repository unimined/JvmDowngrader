package xyz.wagyourtail.downgradetest;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

public class TestProcessHandle {

    public static void main(String[] args) throws IOException {
        ProcessHandle currentProcessHandle = ProcessHandle.current();
        System.out.println(currentProcessHandle.pid() == ManagementFactory.getRuntimeMXBean().getPid());
        List<String> lst = Arrays.asList(currentProcessHandle.info().arguments().get());
        System.out.println(lst.subList(lst.size() - 3, lst.size()));

        ProcessBuilder pb;
        // check if windows
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            pb = new ProcessBuilder("timeout", "1");
        } else {
            pb = new ProcessBuilder("sleep", "1");
        }
        Process p = pb.start();
        currentProcessHandle.children().map(e -> String.join(" ", e.info().arguments().get())).forEach(System.out::println);
        p.toHandle().onExit().thenAccept(e -> System.out.println(e.info().commandLine()));
//        Thread.sleep(1);
    }
}
