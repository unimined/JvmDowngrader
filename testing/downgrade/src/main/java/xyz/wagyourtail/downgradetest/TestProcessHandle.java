package xyz.wagyourtail.downgradetest;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

public class TestProcessHandle {

    public static void main(String[] args) {
        ProcessHandle currentProcessHandle = ProcessHandle.current();
        System.out.println(currentProcessHandle.pid() == ManagementFactory.getRuntimeMXBean().getPid());
        List<String> lst = Arrays.asList(currentProcessHandle.info().arguments().get());
        System.out.println(lst.subList(lst.size() - 3, lst.size()));
    }
}
