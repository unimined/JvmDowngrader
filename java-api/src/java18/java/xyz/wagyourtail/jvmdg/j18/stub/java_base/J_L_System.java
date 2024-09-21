package xyz.wagyourtail.jvmdg.j18.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Properties;

/**
 * JEP 400
 */
public class J_L_System {

    @Stub(ref = @Ref("java/lang/System"))
    public static String getProperty(String key) {
        if (key.equals("native.encoding")) {
            // check if native.encoding is actually set
            var prop = System.getProperty(key);
            if (prop != null) {
                return prop;
            }
            // return fallback
            return System.getProperty("file.encoding");
        }
        return System.getProperty(key);
    }

    @Stub(ref = @Ref("java/lang/System"))
    public static Properties getProperties() {
        var props = System.getProperties();
        if (!props.containsKey("native.encoding")) {
            props.put("native.encoding", props.getProperty("file.encoding"));
        }
        return props;
    }

}
