package xyz.wagyourtail.jvmdg.j12;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.j12.stub.*;

public class Java12Downgrader extends VersionProvider {
    public Java12Downgrader() {
        super(Opcodes.V12, Opcodes.V11);
    }

    public void init() {
            // -- java.base --
            stub(J_I_InputStream.class);
            // Character$UnicodeBlock (more unicode spaces);
            stub(J_L_Class.class);
            // Double
            // Enum
            // Float
            // Integer
            // Long
            // String
            // ClassDesc
            // ConstantDesc
            // ConstantDescs
            // DirectMethodHandleDesc
            // DynamicCallSiteDesc
            // DynamicConstantDesc
            // MethodHandleDesc
            // MethodTypeDesc
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