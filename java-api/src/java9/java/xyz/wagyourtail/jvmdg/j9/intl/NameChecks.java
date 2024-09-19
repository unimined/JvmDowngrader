package xyz.wagyourtail.jvmdg.j9.intl;

import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_U_Set;

import java.util.Set;

public class NameChecks {

    private static final Set<String> RESERVED = J_U_Set.of(
        "abstract",
        "assert",
        "boolean",
        "break",
        "byte",
        "case",
        "catch",
        "char",
        "class",
        "const",
        "continue",
        "default",
        "do",
        "double",
        "else",
        "enum",
        "extends",
        "final",
        "finally",
        "float",
        "for",
        "goto",
        "if",
        "implements",
        "import",
        "instanceof",
        "int",
        "interface",
        "long",
        "native",
        "new",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "short",
        "static",
        "strictfp",
        "super",
        "switch",
        "synchronized",
        "this",
        "throw",
        "throws",
        "transient",
        "try",
        "void",
        "volatile",
        "while",
        "true",
        "false",
        "null",
        "_"
    );

    public static void checkModuleName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null module name");
        }
        String[] parts = name.split("\\.", -1);
        for (String part : parts) {
            if (!isJavaIdentifier(part)) {
                throw new IllegalArgumentException(name + ": Invalid module name: " + part + " is not a Java identifier");
            }
        }
    }

    public static void checkPackageName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null package name");
        }
        String[] parts = name.split("\\.", -1);
        for (String part : parts) {
            if (!isJavaIdentifier(part)) {
                throw new IllegalArgumentException(name + ": Invalid package name: " + part + " is not a Java identifier");
            }
        }
    }

    public static void checkServiceTypeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null service type name");
        }
        String[] parts = name.split("\\.", -1);
        for (String part : parts) {
            if (!isJavaIdentifier(part)) {
                throw new IllegalArgumentException(name + ": Invalid service type name: " + part + " is not a Java identifier");
            }
        }
    }

    public static void checkServiceProviderName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null service provider name");
        }
        String[] parts = name.split("\\.", -1);
        for (String part : parts) {
            if (!isJavaIdentifier(part)) {
                throw new IllegalArgumentException(name + ": Invalid service provider name: " + part + " is not a Java identifier");
            }
        }
    }

    public static void checkClassName(String title, String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null " + title + " name");
        }
        String[] parts = name.split("\\$", -1);
        for (String part : parts) {
            if (!isJavaIdentifier(part)) {
                throw new IllegalArgumentException(name + ": Invalid " + title + " name: " + part + " is not a Java identifier");
            }
        }
    }

    public static void checkJavaIdentifier(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null identifier");
        }
        if (!isJavaIdentifier(name)) {
            throw new IllegalArgumentException(name + ": Invalid identifier");
        }
    }

    public static boolean isJavaIdentifier(String name) {
        if (name.isEmpty()) return false;
        if (RESERVED.contains(name)) return false;
        int firstCodePoint = name.codePointAt(0);
        if (!Character.isJavaIdentifierStart(firstCodePoint)) return false;
        for (int i = Character.charCount(firstCodePoint); i < name.length(); i += Character.charCount(name.codePointAt(i))) {
            if (!Character.isJavaIdentifierPart(name.codePointAt(i))) return false;
        }
        return true;
    }

}
