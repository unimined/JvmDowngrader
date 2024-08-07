package xyz.wagyourtail.jvmdg.test.integration;

import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.test.JavaRunner;

public record FlagsAndRunner(JavaRunner.JavaVersion targetVersion, Flags flags) {

    public String readableSlug() {
        StringBuilder sb = new StringBuilder();
        sb.append(flags.classVersion);
        if (!flags.debugSkipStubs.isEmpty()) {
            sb.append("-fake-");
            for (int i : flags.debugSkipStubs) {
                sb.append(JavaRunner.JavaVersion.fromOpcode(i).getMajorVersion());
                sb.append("-");
            }
            sb.setLength(sb.length() - 1);
        }
        if (!flags.multiReleaseVersions.isEmpty()) {
            sb.append("-mr-");
            for (int i : flags.multiReleaseVersions) {
                sb.append(i);
                sb.append("-");
            }
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

}
