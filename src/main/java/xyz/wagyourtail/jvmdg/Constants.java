package xyz.wagyourtail.jvmdg;

import java.io.File;

public class Constants {

    @Deprecated
    public static final String QUIET = "jvmdg.quiet";
    public static final String CLASS_VERSION = "jvmdg.classVersion";
    public static final String LOG_ANSI_COLORS = "jvmdg.logAnsiColors";
    public static final String LOG_LEVEL = "jvmdg.logLevel";
    public static final String JAVA_API = "jvmdg.javaApi";
    public static final String ALLOW_MAVEN_LOOKUP = "jvmdg.maven";

    public static final String IGNORE_WARNINGS = "jvmdg.ignoreWarnings";

    public static final String DEBUG = "jvmdg.debug";
    public static final String DEBUG_SKIP_STUB = "jvmdg.debug.skipStub";
    public static final String DEBUG_SKIP_STUBS = "jvmdg.debug.skipStubs";
    public static final String DEBUG_DUMP_CLASSES = "jvmdg.debug.dumpClasses";

    public static final String MULTI_RELEASE_ORIGINAL = "jvmdg.multiReleaseOriginal";
    public static final String MULTI_RELEASE_VERSIONS = "jvmdg.multiReleaseVersions";

    public static final String SHADE_INLINING = "jvmdg.shadeInlining";


    public static final File DIR = new File(".jvmdg");
    public static final File DEBUG_DIR = new File(DIR, "debug");

}
