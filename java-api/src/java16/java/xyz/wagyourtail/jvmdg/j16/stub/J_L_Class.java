package xyz.wagyourtail.jvmdg.j16.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class J_L_Class {


    @Stub(opcVers = Opcodes.V16)
    public static J_L_R_RecordComponent[] getRecordComponents(Class<?> clazz) {
        // check if the field exists
        Field[] fields = clazz.getDeclaredFields();
        List<J_L_R_RecordComponent> components = new ArrayList<>();
        for (Field f : fields) {
            // if not static
            if ((f.getModifiers() & Opcodes.ACC_STATIC) == 0) {
                components.add(new J_L_R_RecordComponent(clazz, f));
            }
        }
        return components.toArray(new J_L_R_RecordComponent[0]);

    }

    @Stub(opcVers = Opcodes.V16)
    public static boolean isRecord(Class<?> clazz) {
        return J_L_Record.class.isAssignableFrom(clazz);
    }

}
