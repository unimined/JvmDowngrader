package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_StringBuffer {
    
    @Stub
    public static StringBuffer repeat(StringBuffer self, int codepoint, int count) {
        return self.append(Character.toString(codepoint).repeat(count));
    }

    @Stub
    public static StringBuffer repeat(StringBuffer self, CharSequence str, int count) {
        return self.append(str.toString().repeat(count));
    }
    
}
