package xyz.wagyourtail.jvmdg.providers;

import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.stub.all.stub.J_L_Class;

public abstract class AllVersionDowngrader extends VersionProvider {

    protected AllVersionDowngrader(int inputVersion, int outputVersion, int priotity) {
        super(inputVersion, outputVersion, priotity);
    }

    @Override
    public void preInit() {
        stub(J_L_Class.class);
    }

}
