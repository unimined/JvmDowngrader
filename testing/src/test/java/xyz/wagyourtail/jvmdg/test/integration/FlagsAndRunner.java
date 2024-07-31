package xyz.wagyourtail.jvmdg.test.integration;

import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.test.JavaRunner;

import java.util.stream.Collectors;

public record FlagsAndRunner(Flags flags, JavaRunner.JavaVersion targetVersion) {

    public String readableSlug() {
        if (flags.debugSkipStubs.isEmpty()) {
            return Integer.toString(targetVersion.getMajorVersion());
        } else {
            return targetVersion.getMajorVersion() + "-fake-" + flags.debugSkipStubs.stream().map(e -> Integer.toString(JavaRunner.JavaVersion.fromOpcode(e).getMajorVersion())).collect(Collectors.joining("-"));
        }
    }

}
