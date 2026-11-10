package xyz.wagyourtail.jvmdg.j16.impl;

import xyz.wagyourtail.jvmdg.stub.EnumExtender;
import xyz.wagyourtail.jvmdg.stub.EnumExtension;

import java.net.StandardProtocolFamily;

/**
 * Eagerly injects {@link StandardProtocolFamily} {@code UNIX} into the enum
 * universe when the agent's {@code Bootstrap} loads {@link EnumExtension}s at
 * premain — covering consumers that would otherwise observe the un-extended
 * universe before touching the {@code UNIX} field or a unix stub.
 */
public class StandardProtocolFamilyExtension implements EnumExtension {

    @Override
    public void extend() {
        EnumExtender.extend(StandardProtocolFamily.class, "UNIX");
    }

}
