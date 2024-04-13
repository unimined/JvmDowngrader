package xyz.wagyourtail.jvmdg.j8.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

@Stub(ref = @Ref("java/util/StringJoiner"))
public final class J_U_StringJoiner {

    private final String prefix;
    private final String delimiter;
    private final String suffix;
    StringBuilder builder = new StringBuilder();
    private String emptyValue;

    public J_U_StringJoiner(CharSequence delimiter) {
        this(delimiter, "", "");
    }

    public J_U_StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        this.prefix = prefix.toString();
        this.delimiter = delimiter.toString();
        this.suffix = suffix.toString();
    }

    public J_U_StringJoiner setEmptyValue(CharSequence emptyValue) {
        this.emptyValue = emptyValue.toString();
        return this;
    }

    @Override
    public String toString() {
        if (emptyValue != null && builder.length() == 0) {
            return emptyValue;
        }
        // remove last delimiter
        builder.setLength(builder.length() - delimiter.length());
        String out = prefix + builder.toString() + suffix;
        // add delimiter back
        builder.append(delimiter);
        return out;
    }

    public J_U_StringJoiner add(CharSequence newElement) {
        if (builder.length() + newElement.length() + prefix.length() + suffix.length() > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("StringJoiner too long");
        }
        builder.append(newElement).append(delimiter);
        return this;
    }

    public J_U_StringJoiner merge(J_U_StringJoiner other) {
        if (other.builder.length() > 0) {
            // remove last delimiter from other
            other.builder.setLength(other.builder.length() - other.delimiter.length());
            add(other.builder);
            // add delimiter back to other
            other.builder.append(other.delimiter);
        }
        return this;

    }

    public int length() {
        if (emptyValue != null && builder.length() == 0) {
            return emptyValue.length();
        }
        if (builder.length() == 0) {
            return prefix.length() + suffix.length();
        }
        return builder.length() + prefix.length() + suffix.length() - delimiter.length();
    }
}
