package xyz.wagyourtail.jvmdg.j16.stub.java_base;


import xyz.wagyourtail.jvmdg.j16.RecordComponents;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.ArrayList;
import java.util.List;

public class J_L_Class {


    @Stub
    public static J_L_R_RecordComponent[] getRecordComponents(Class<?> clazz) {
        if (!isRecord(clazz)) {
            return null;
        }
        RecordComponents components = clazz.getAnnotation(RecordComponents.class);
        if (components == null) {
            return null;
        }
        List<J_L_R_RecordComponent> recordComponents = new ArrayList<>();
        for (RecordComponents.Value value : components.value()) {
            var name = value.name();
            var type = value.type();
            recordComponents.add(new J_L_R_RecordComponent(clazz, name, type));
        }
        return recordComponents.toArray(new J_L_R_RecordComponent[0]);
    }

    @Stub
    public static boolean isRecord(Class<?> clazz) {
        return J_L_Record.class.isAssignableFrom(clazz);
    }

}
