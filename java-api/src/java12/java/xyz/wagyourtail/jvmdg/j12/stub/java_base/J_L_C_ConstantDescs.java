package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.invoke.ConstantBootstraps;

@Adapter("java/lang/constant/ConstantDescs")
public class J_L_C_ConstantDescs {

    public static final String DEFAULT_NAME = "_";

    public static final J_L_C_ClassDesc CD_Object = J_L_C_ClassDesc.of("java.lang.Object");
    public static final J_L_C_ClassDesc CD_String = J_L_C_ClassDesc.of("java.lang.String");
    public static final J_L_C_ClassDesc CD_Class = J_L_C_ClassDesc.of("java.lang.Class");
    public static final J_L_C_ClassDesc CD_Number = J_L_C_ClassDesc.of("java.lang.Number");
    public static final J_L_C_ClassDesc CD_Integer = J_L_C_ClassDesc.of("java.lang.Integer");
    public static final J_L_C_ClassDesc CD_Long = J_L_C_ClassDesc.of("java.lang.Long");
    public static final J_L_C_ClassDesc CD_Float = J_L_C_ClassDesc.of("java.lang.Float");
    public static final J_L_C_ClassDesc CD_Double = J_L_C_ClassDesc.of("java.lang.Double");
    public static final J_L_C_ClassDesc CD_Short = J_L_C_ClassDesc.of("java.lang.Short");
    public static final J_L_C_ClassDesc CD_Byte = J_L_C_ClassDesc.of("java.lang.Byte");
    public static final J_L_C_ClassDesc CD_Character = J_L_C_ClassDesc.of("java.lang.Character");
    public static final J_L_C_ClassDesc CD_Boolean = J_L_C_ClassDesc.of("java.lang.Boolean");
    public static final J_L_C_ClassDesc CD_Void = J_L_C_ClassDesc.of("java.lang.Void");
    public static final J_L_C_ClassDesc CD_Throwable = J_L_C_ClassDesc.of("java.lang.Throwable");
    public static final J_L_C_ClassDesc CD_Exception = J_L_C_ClassDesc.of("java.lang.Exception");
    public static final J_L_C_ClassDesc CD_Enum = J_L_C_ClassDesc.of("java.lang.Enum");
//    public static final J_L_C_ClassDesc CD_VarHandle = J_L_C_ClassDesc.of("java.lang.VarHandle");
    public static final J_L_C_ClassDesc CD_MethodHandles = J_L_C_ClassDesc.of("java.lang.MethodHandles");
    public static final J_L_C_ClassDesc CD_MethodHandles_Lookup = J_L_C_ClassDesc.of("java.lang.MethodHandles.Lookup");
    public static final J_L_C_ClassDesc CD_MethodHandle = J_L_C_ClassDesc.of("java.lang.MethodHandle");
    public static final J_L_C_ClassDesc CD_MethodType = J_L_C_ClassDesc.of("java.lang.MethodType");
    public static final J_L_C_ClassDesc CD_CallSite = J_L_C_ClassDesc.of("java.lang.CallSite");
    public static final J_L_C_ClassDesc CD_Collection = J_L_C_ClassDesc.of("java.lang.Collection");
    public static final J_L_C_ClassDesc CD_List = J_L_C_ClassDesc.of("java.lang.List");
    public static final J_L_C_ClassDesc CD_Set = J_L_C_ClassDesc.of("java.lang.Set");
    public static final J_L_C_ClassDesc CD_Map = J_L_C_ClassDesc.of("java.lang.Map");
    public static final J_L_C_ClassDesc CD_ConstantDesc = J_L_C_ClassDesc.of(J_L_C_ConstantDesc.class.getName());
    public static final J_L_C_ClassDesc CD_ClassDesc = J_L_C_ClassDesc.of(J_L_C_ClassDesc.class.getName());
    public static final J_L_C_ClassDesc CD_EnumDesc = J_L_C_ClassDesc.of(J_L_Enum$EnumDesc.class.getName());
    public static final J_L_C_ClassDesc CD_MethodTypeDesc = J_L_C_ClassDesc.of(J_L_C_MethodTypeDesc.class.getName());
    public static final J_L_C_ClassDesc CD_MethodHandleDesc = J_L_C_ClassDesc.of(J_L_C_MethodHandleDesc.class.getName());
    public static final J_L_C_ClassDesc CD_DirectMethodHandleDesc = J_L_C_ClassDesc.of(J_L_C_DirectMethodHandleDesc.class.getName());
//    public static final J_L_C_ClassDesc CD_VarHandleDesc
    public static final J_L_C_ClassDesc CD_MethodHandleDesc_Kind = CD_MethodHandleDesc.nested("Kind");
    public static final J_L_C_ClassDesc CD_DynamicConstantDesc = J_L_C_ClassDesc.of(J_L_C_DynamicConstantDesc.class.getName());
//    public static final CD_DynamicCallSiteDesc
    public static final J_L_C_ClassDesc CD_ConstantBootstraps = J_L_C_ClassDesc.of(ConstantBootstraps.class.getName());

    private static final J_L_C_ClassDesc[] INDY_BSM = new J_L_C_ClassDesc[] {
        CD_MethodHandles_Lookup,
        CD_String,
        CD_MethodType
    };

    private static final J_L_C_ClassDesc[] CONDY_BSM = new J_L_C_ClassDesc[] {
        CD_MethodHandles_Lookup,
        CD_String,
        CD_Class
    };

    public static final J_L_C_DirectMethodHandleDesc BSM_PRIMITIVE_CLASS = ofConstantBootstrap(
        CD_ConstantBootstraps,
        "primitiveClass",
        CD_Class
    );

    public static final J_L_C_DirectMethodHandleDesc BSM_ENUM_CONSTANT = ofConstantBootstrap(
        CD_ConstantBootstraps,
        "enumConstant",
        CD_Enum
    );

    public static final J_L_C_DirectMethodHandleDesc BSM_GET_STATIC_FINAL = ofConstantBootstrap(
        CD_ConstantBootstraps,
        "getStaticFinal",
        CD_Object,
        CD_Class
    );

    public static final J_L_C_DirectMethodHandleDesc BSM_NULL_CONSTANT = ofConstantBootstrap(
        CD_ConstantBootstraps,
        "nullConstant",
        CD_Object
    );

//    public static final J_L_C_DirectMethodHandleDesc BSM_VARHANDLE_FIELD

    public static final J_L_C_DirectMethodHandleDesc BSM_INVOKE = ofConstantBootstrap(
        CD_ConstantBootstraps,
        "invoke",
        CD_Object,
        CD_MethodHandle,
        CD_Object.arrayType()
    );

    public static final J_L_C_ClassDesc CD_int = J_L_C_ClassDesc.ofDescriptor("I");
    public static final J_L_C_ClassDesc CD_long = J_L_C_ClassDesc.ofDescriptor("J");
    public static final J_L_C_ClassDesc CD_float = J_L_C_ClassDesc.ofDescriptor("F");
    public static final J_L_C_ClassDesc CD_double = J_L_C_ClassDesc.ofDescriptor("D");
    public static final J_L_C_ClassDesc CD_short = J_L_C_ClassDesc.ofDescriptor("S");
    public static final J_L_C_ClassDesc CD_byte = J_L_C_ClassDesc.ofDescriptor("B");
    public static final J_L_C_ClassDesc CD_char = J_L_C_ClassDesc.ofDescriptor("C");
    public static final J_L_C_ClassDesc CD_boolean = J_L_C_ClassDesc.ofDescriptor("Z");
    public static final J_L_C_ClassDesc CD_void = J_L_C_ClassDesc.ofDescriptor("V");

    public static final J_L_C_ConstantDesc NULL = J_L_C_DynamicConstantDesc.ofNamed(BSM_NULL_CONSTANT, "_", CD_Object);
    public static final J_L_C_DynamicConstantDesc<Boolean> TRUE = J_L_C_DynamicConstantDesc.ofNamed(BSM_GET_STATIC_FINAL, "TRUE", CD_Boolean, CD_Boolean);
    public static final J_L_C_DynamicConstantDesc<Boolean> FALSE = J_L_C_DynamicConstantDesc.ofNamed(BSM_GET_STATIC_FINAL, "FALSE", CD_Boolean, CD_Boolean);

    static final J_L_C_DirectMethodHandleDesc AS_TYPE = J_L_C_MethodHandleDesc.ofMethod(
        J_L_C_DirectMethodHandleDesc.Kind.VIRTUAL,
        CD_MethodHandle,
        "asType",
        J_L_C_MethodTypeDesc.of(CD_MethodHandle, CD_MethodType)
    );

    public static J_L_C_DirectMethodHandleDesc ofCallsiteBootstrap(
        J_L_C_ClassDesc owner,
        String name,
        J_L_C_ClassDesc returnType,
        J_L_C_ClassDesc... args
    ) {
        return J_L_C_MethodHandleDesc.ofMethod(
            J_L_C_DirectMethodHandleDesc.Kind.STATIC,
            owner,
            name,
            J_L_C_MethodTypeDesc.of(
                returnType,
                args
            ).insertParameterTypes(0, INDY_BSM)
        );
    }

    public static J_L_C_DirectMethodHandleDesc ofConstantBootstrap(
        J_L_C_ClassDesc owner,
        String name,
        J_L_C_ClassDesc returnType,
        J_L_C_ClassDesc... params
    ) {
        return J_L_C_MethodHandleDesc.ofMethod(
            J_L_C_DirectMethodHandleDesc.Kind.STATIC,
            owner,
            name,
            J_L_C_MethodTypeDesc.of(
                returnType,
                params
            ).insertParameterTypes(0, CONDY_BSM)
        );
    }

}
