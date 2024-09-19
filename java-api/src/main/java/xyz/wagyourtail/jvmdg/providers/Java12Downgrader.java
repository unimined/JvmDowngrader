package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j12.stub.java_base.*;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java12Downgrader extends VersionProvider {
    public Java12Downgrader() {
        super(Opcodes.V12, Opcodes.V11, 0);
    }

    public void init() {
        // -- java.base --
        stub(J_I_InputStream.class);
        // Character$UnicodeBlock (more unicode spaces);
        stub(J_L_C_ClassDesc.class);
        stub(J_L_C_ConstantDesc.class);
        stub(J_L_Class.class);
        // Double
        stub(J_L_Enum$EnumDesc.class);
        // Float
        stub(J_L_I_TypeDescriptor.class);
        // Integer
        // Long
        stub(J_L_String.class);
        stub(J_L_C_ConstantDescs.class);
        stub(J_L_C_DirectMethodHandleDesc.class);
        // DynamicCallSiteDesc
        stub(J_L_C_DynamicConstantDesc.class);
        stub(J_L_C_MethodHandleDesc.class);
        stub(J_L_C_MethodTypeDesc.class);
        // MethodHandle
        // MethodType
        // TypeDescriptor
        // VarHandle
        // SecureCacheResponse
        // ServerSocket
        stub(J_N_F_Files.class);
        stub(J_N_F_FileSystems.class);
        stub(J_N_F_S_FileSystemProvider.class);
        // CompactNumberFormat
        // NumberFormat
        // NumberFormatProvider
        stub(J_U_C_CompletionStage.class);
        stub(J_U_S_Collectors.class);
        // HttpsURLConnection
        // NumberFormatProviderImpl

        // -- java.compiler --
        // SourceVersion

        // -- java.desktop --
        // FileSystemView

        // -- java.naming --
        // LdapDnsProvider
        // LdapDnsProviderResult

        // -- jdk.compiler --
        // DocTreeView
        // SystemPropertyTree
        // CaseTree
        // SwitchExpressionTree
        // Tree
        // TreeVisitor
        // DocTreeFactory
        // DocTreeScanner
        // SimpleDocTreeVisitor
    }

}
