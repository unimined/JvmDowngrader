package xyz.wagyourtail.jvmdg.j16.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.lang.reflect.Field;

public class J_L_Class {


    @Stub(opcVers = Opcodes.V16)
    public static J_L_R_RecordComponent[] getRecordComponents(Class<?> clazz) {
        // check if the field exists
        try {
            Field f = clazz.getDeclaredField("recordComponents$jvmdowngrader");
            String typeStr = (String) f.get(null);
            String[] types = typeStr.split(":");
            J_L_R_RecordComponent[] components = new J_L_R_RecordComponent[types.length];
            for (int i = 0; i < types.length; i++) {
                components[i] = new J_L_R_RecordComponent(clazz, types[i]);
            }
            return components;

        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Stub(opcVers = Opcodes.V16)
    public static boolean isRecord(Class<?> clazz) {
        return J_L_Record.class.isAssignableFrom(clazz);
    }

}
