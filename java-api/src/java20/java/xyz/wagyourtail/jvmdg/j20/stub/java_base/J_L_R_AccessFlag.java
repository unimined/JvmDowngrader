package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static xyz.wagyourtail.jvmdg.j20.stub.java_base.J_L_R_AccessFlag.Location.*;

@Adapter("java/lang/reflect/AccessFlag")
public enum J_L_R_AccessFlag {
    PUBLIC(
            Modifier.PUBLIC, true, Set.of(CLASS, FIELD, METHOD, INNER_CLASS),
            version -> {
                if (version == J_L_R_ClassFileFormatVersion.RELEASE_0) {
                    return Set.of(CLASS, FIELD, METHOD);
                } else {
                    return Set.of(CLASS, FIELD, METHOD, INNER_CLASS);
                }
            }
    ),
    PRIVATE(
            Modifier.PRIVATE, true, Set.of(FIELD, METHOD, INNER_CLASS),
            version -> {
                if (version == J_L_R_ClassFileFormatVersion.RELEASE_0) {
                    return Set.of(FIELD, METHOD);
                } else {
                    return Set.of(FIELD, METHOD, INNER_CLASS);
                }
            }
    ),
    PROTECTED(
            Modifier.PROTECTED, true, Set.of(FIELD, METHOD, INNER_CLASS),
            version -> {
                if (version == J_L_R_ClassFileFormatVersion.RELEASE_0) {
                    return Set.of(FIELD, METHOD);
                } else {
                    return Set.of(FIELD, METHOD, INNER_CLASS);
                }
            }
    ),
    STATIC(
            Modifier.STATIC, true, Set.of(FIELD, METHOD, INNER_CLASS),
            version -> {
                if (version == J_L_R_ClassFileFormatVersion.RELEASE_0) {
                    return Set.of(FIELD, METHOD);
                } else {
                    return Set.of(FIELD, METHOD, INNER_CLASS);
                }
            }
    ),
    FINAL(
            Modifier.FINAL, true, Set.of(CLASS, FIELD, METHOD, INNER_CLASS),
            version -> {
                if (version == J_L_R_ClassFileFormatVersion.RELEASE_0) {
                    return Set.of(CLASS, FIELD, METHOD);
                } else {
                    return Set.of(CLASS, FIELD, METHOD, INNER_CLASS);
                }
            }
    ),
    SUPER(
            0x20, false, Set.of(CLASS), null
    ),
    OPEN(
            0x20, false, Set.of(Location.MODULE),
            version -> {
                if (version.compareTo(J_L_R_ClassFileFormatVersion.RELEASE_9) >= 0) {
                    return Set.of(Location.MODULE);
                } else {
                    return Collections.emptySet();
                }
            }
    ),
    TRANSITIVE(
            0x0020, false, Set.of(MODULE_REQUIRES),
            version -> {
                if (version.compareTo(J_L_R_ClassFileFormatVersion.RELEASE_9) >= 0) {
                    return Set.of(MODULE_REQUIRES);
                } else {
                    return Collections.emptySet();
                }
            }
    ),
    SYNCHRONIZED(
            Modifier.SYNCHRONIZED, true, Set.of(METHOD), null
    ),
    STATIC_PHASE(
            0x0040, false, Set.of(MODULE_REQUIRES),
            version -> {
                if (version.compareTo(J_L_R_ClassFileFormatVersion.RELEASE_9) >= 0) {
                    return Set.of(MODULE_REQUIRES);
                } else {
                    return Collections.emptySet();
                }
            }
    ),
    VOLATILE(
            Modifier.VOLATILE, true, Set.of(FIELD), null
    ),
    BRIDGE(
            0x40, true, Set.of(METHOD),
            version -> {
                if (version.compareTo(J_L_R_ClassFileFormatVersion.RELEASE_5) >= 0) {
                    return Set.of(METHOD);
                } else {
                    return Collections.emptySet();
                }
            }
    ),
    TRANSIENT(
            Modifier.TRANSIENT, true, Set.of(FIELD), null
    ),
    VARARGS(
            0x80, true, Set.of(METHOD),
            version -> {
                if (version.compareTo(J_L_R_ClassFileFormatVersion.RELEASE_5) >= 0) {
                    return Set.of(METHOD);
                } else {
                    return Collections.emptySet();
                }
            }
    ),
    NATIVE(
            Modifier.NATIVE, true, Set.of(METHOD), null
    ),
    INTERFACE(
            Modifier.INTERFACE, true, Set.of(CLASS, INNER_CLASS),
            version -> {
                if (version == J_L_R_ClassFileFormatVersion.RELEASE_0) {
                    return Set.of(CLASS);
                } else {
                    return Set.of(CLASS, INNER_CLASS);
                }
            }
    ),
    ABSTRACT(
            Modifier.ABSTRACT, true, Set.of(CLASS, METHOD, INNER_CLASS),
            version -> {
                if (version == J_L_R_ClassFileFormatVersion.RELEASE_0) {
                    return Set.of(CLASS, METHOD);
                } else {
                    return Set.of(CLASS, METHOD, INNER_CLASS);
                }
            }
    ),
    STRICT(
            Modifier.STRICT, true, Set.of(),
            version -> {
                if (version.compareTo(J_L_R_ClassFileFormatVersion.RELEASE_2) >= 0 && version.compareTo(J_L_R_ClassFileFormatVersion.RELEASE_16) <= 0) {
                    return Set.of(METHOD);
                } else {
                    return Collections.emptySet();
                }
            }
    ),
    SYNTHETIC(
            0x1000, false, Set.of(CLASS, FIELD, METHOD, INNER_CLASS, METHOD_PARAMETER, Location.MODULE, MODULE_REQUIRES, MODULE_EXPORTS, MODULE_OPENS),
            version -> {
                if (version.compareTo(J_L_R_ClassFileFormatVersion.RELEASE_9) >= 0) {
                    return Set.of(CLASS, FIELD, METHOD, INNER_CLASS, METHOD_PARAMETER, Location.MODULE, MODULE_REQUIRES, MODULE_EXPORTS, MODULE_OPENS);
                } else {
                    return switch (version) {
                        case RELEASE_7 -> Set.of(CLASS, FIELD, METHOD, INNER_CLASS);
                        case RELEASE_8 -> Set.of(CLASS, FIELD, METHOD, INNER_CLASS, METHOD_PARAMETER);
                        default -> Collections.emptySet();
                    };
                }
            }
    ),
    ANNOTATION(
            0x2000, false, Set.of(CLASS, INNER_CLASS),
            version -> {
                if (version.compareTo(J_L_R_ClassFileFormatVersion.RELEASE_5) >= 0) {
                    return Set.of(CLASS, INNER_CLASS);
                } else {
                    return Collections.emptySet();
                }
            }
    ),
    ENUM(
            0x4000, false, Set.of(CLASS, FIELD, INNER_CLASS),
            version -> {
                if (version.compareTo(J_L_R_ClassFileFormatVersion.RELEASE_5) >= 0) {
                    return Set.of(CLASS, FIELD, INNER_CLASS);
                } else {
                    return Collections.emptySet();
                }
            }
    ),
    MANDATED(
            0x8000, false, Set.of(METHOD_PARAMETER, Location.MODULE, MODULE_REQUIRES, MODULE_EXPORTS, MODULE_OPENS),
            version -> {
                if (version.compareTo(J_L_R_ClassFileFormatVersion.RELEASE_9) >= 0) {
                    return Set.of(CLASS);
                } else if (version == J_L_R_ClassFileFormatVersion.RELEASE_8) {
                    return Set.of(METHOD_PARAMETER);
                } else {
                    return Collections.emptySet();
                }
            }
    ),
    MODULE(
            0x8000, false, Set.of(CLASS),
            version -> {
                if (version.compareTo(J_L_R_ClassFileFormatVersion.RELEASE_9) >= 0) {
                    return Set.of(CLASS);
                } else {
                    return Collections.emptySet();
                }
            }
    ),
    ;

    private final int mask;
    private final boolean sourceModifier;

    private final Set<Location> locations;
    private final Function<J_L_R_ClassFileFormatVersion, Set<Location>> locationsFunction;

    J_L_R_AccessFlag(int mask, boolean sourceModifier, Set<Location> locations, Function<J_L_R_ClassFileFormatVersion, Set<Location>> locationsFunction) {
        this.mask = mask;
        this.sourceModifier = sourceModifier;
        this.locations = locations;
        this.locationsFunction = locationsFunction;
    }

    public static Set<J_L_R_AccessFlag> maskToAccessFlags(int mask, Location location) {
        Set<J_L_R_AccessFlag> result = EnumSet.noneOf(J_L_R_AccessFlag.class);
        for (J_L_R_AccessFlag flag : values()) {
            if (!flag.locations.contains(location)) continue;
            if ((mask & flag.mask) != 0) {
                result.add(flag);
                mask &= ~flag.mask;
            }
        }
        if (mask != 0) {
            throw new IllegalArgumentException("Unmatched bit position 0x" + Integer.toHexString(mask) + " for location " + location);
        }
        return Collections.unmodifiableSet(result);
    }

    public int mask() {
        return mask;
    }

    public boolean sourceModifier() {
        return sourceModifier;
    }

    public Set<Location> locations() {
        return locations;
    }

    public Set<Location> locations(J_L_R_ClassFileFormatVersion version) {
        if (locationsFunction == null) {
            return locations;
        } else {
            return locationsFunction.apply(version);
        }
    }

    @Adapter("java/lang/reflect/AccessFlag$Location")
    public enum Location {
        CLASS,
        FIELD,
        METHOD,
        INNER_CLASS,
        METHOD_PARAMETER,
        MODULE,
        MODULE_REQUIRES,
        MODULE_EXPORTS,
        MODULE_OPENS
    }

}
