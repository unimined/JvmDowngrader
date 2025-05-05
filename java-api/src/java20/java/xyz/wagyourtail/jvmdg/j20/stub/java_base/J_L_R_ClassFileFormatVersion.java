package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

@Adapter("java/lang/reflect/ClassFileFormatVersion")
public enum J_L_R_ClassFileFormatVersion {
    RELEASE_0(45),
    RELEASE_1(45),
    RELEASE_2(46),
    RELEASE_3(47),
    RELEASE_4(48),
    RELEASE_5(49),
    RELEASE_6(50),
    RELEASE_7(51),
    RELEASE_8(52),
    RELEASE_9(53),
    RELEASE_10(54),
    RELEASE_11(55),
    RELEASE_12(56),
    RELEASE_13(57),
    RELEASE_14(58),
    RELEASE_15(59),
    RELEASE_16(60),
    RELEASE_17(61),
    RELEASE_18(62),
    RELEASE_19(63),
    RELEASE_20(64),
    RELEASE_21(65),
    RELEASE_22(66),
    RELEASE_23(67),
    RELEASE_24(68),
    RELEASE_25(69),
    RELEASE_26(70),
    RELEASE_27(71);

    private final int version;

    J_L_R_ClassFileFormatVersion(int version) {
        this.version = version;
    }

    public static J_L_R_ClassFileFormatVersion latest() {
        return RELEASE_22;
    }

    public static J_L_R_ClassFileFormatVersion valueOf(Runtime.Version rv) {
        return valueOf("RELEASE_" + rv.feature());
    }

    public static J_L_R_ClassFileFormatVersion fromMajor(int major) {
        if (major < 45 || major > latest().major()) {
            throw new IllegalArgumentException("Out of range major class file vesion " + major);
        }
        return values()[major - 44];
    }

    public int major() {
        return version;
    }

    public Runtime.Version runtimeVersion() {
        if (this.compareTo(RELEASE_6) >= 0) {
            return Runtime.Version.parse(Integer.toString(ordinal()));
        } else {
            return null;
        }
    }

}
