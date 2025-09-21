package xyz.wagyourtail.jvmdg.j25.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.ClassFileFormatVersion;
import java.util.*;

public class J_L_R_AccessFlag {

    @Stub(ref = @Ref("java/lang/reflect/AccessFlag"))
    public static Set<AccessFlag> maskToAccessFlags(int mask, AccessFlag.Location location, ClassFileFormatVersion format) {
        Set<AccessFlag> result = EnumSet.noneOf(AccessFlag.class);
        for (AccessFlag flag : Location.flags(location, format)) {
            if ((mask & flag.mask()) != 0) {
                result.add(flag);
                mask &= ~flag.mask();
            }
        }
        if (mask != 0) {
            throw new IllegalArgumentException("Unmatched bit position 0x" + Integer.toHexString(mask) + " for location " + location);
        }
        return Collections.unmodifiableSet(result);
    }

    public static class Location {
        private static final EnumMap<AccessFlag.Location, EnumSet<AccessFlag>> flagsByLocation = new EnumMap<>(AccessFlag.Location.class);
        static {
            for (AccessFlag flag : AccessFlag.values()) {
                for (AccessFlag.Location location : flag.locations()) {
                    flagsByLocation.computeIfAbsent(location, _ -> EnumSet.noneOf(AccessFlag.class)).add(flag);
                }
            }
        }

        @Stub
        public static Set<AccessFlag> flags(AccessFlag.Location self) {
            return Collections.unmodifiableSet(flagsByLocation.get(self));
        }

        @Stub
        public static Set<AccessFlag> flags(AccessFlag.Location location, ClassFileFormatVersion formatVersion) {
            EnumSet<AccessFlag> result = EnumSet.noneOf(AccessFlag.class);
            for (AccessFlag flag : AccessFlag.values()) {
                if (flag.locations(formatVersion).contains(location)) {
                    result.add(flag);
                }
            }
            return Collections.unmodifiableSet(result);
        }

        @Stub
        public static int flagsMask(AccessFlag.Location location) {
            return flags(location).stream().mapToInt(AccessFlag::mask).reduce(0, (a, b) -> a | b);
        }

        @Stub
        public static int flagsMask(AccessFlag.Location location, ClassFileFormatVersion formatVersion) {
            return flags(location, formatVersion).stream().mapToInt(AccessFlag::mask).reduce(0, (a, b) -> a | b);
        }
    }
}
