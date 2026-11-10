package xyz.wagyourtail.jvmdg.j9.intl.varhandle;

import xyz.wagyourtail.jvmdg.exc.PartialStubError;
import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_I_VarHandle;

import java.lang.invoke.WrongMethodTypeException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Objects;

/**
 * VarHandle implementation backing {@code MethodHandles.byteBufferViewVarHandle}:
 * coordinates are {@code (ByteBuffer, int)} where the index is a byte offset.
 *
 * <p>Direct (non-array-backed) buffers are accessed through raw memory addresses:
 * {@link sun.misc.Unsafe} treats a {@code null} base object plus an offset as an
 * absolute address, so {@link #arrayView} returns {@code null} for them and
 * {@link #viewOffset} yields the buffer's address + index.
 */
public class ByteBufferViewVarHandle extends ByteArrayViewVarHandle {

    /**
     * Field offset of {@code java.nio.Buffer.address}, which holds the native
     * memory address for direct buffers. Read via Unsafe so no
     * {@code setAccessible} or {@code sun.nio.ch} dependency is needed.
     * {@code -1} on runtimes lacking the field (non-OpenJDK), where direct
     * buffers fall back to {@link PartialStubError}.
     */
    private static final long ADDRESS_OFFSET;
    static {
        long off;
        try {
            off = unsafe.objectFieldOffset(java.nio.Buffer.class.getDeclaredField("address"));
        } catch (NoSuchFieldException e) {
            off = -1;
        }
        ADDRESS_OFFSET = off;
    }

    public ByteBufferViewVarHandle(Class<?> viewClass, boolean reverseBytes) {
        super(viewClass, reverseBytes);
    }

    @Override
    protected Class<?>[] coordinateClasses() {
        return new Class<?>[] { ByteBuffer.class, int.class };
    }

    @Override
    void checkCoord(Object owner, int index, Class<?> type, boolean read, boolean write) {
        Objects.requireNonNull(owner);
        if (!(owner instanceof ByteBuffer) || type != this.type) {
            throw new WrongMethodTypeException();
        }
        ByteBuffer bb = (ByteBuffer) owner;
        // writes to read-only buffers must fail even though they may have a
        // valid address — reads on them are still allowed
        if (write && bb.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (index < 0 || index > bb.capacity() - arrayIndexScale) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    Object arrayView(Object owner) {
        ByteBuffer bb = (ByteBuffer) owner;
        if (bb.hasArray()) {
            return bb.array();
        }
        // direct buffer: a non-zero address means Unsafe can reach the memory
        // with a null base; address 0 (e.g. read-only heap buffers) or a
        // runtime without the address field can't be supported
        if (ADDRESS_OFFSET < 0 || unsafe.getLong(bb, ADDRESS_OFFSET) == 0) {
            throw PartialStubError.create();
        }
        return null;
    }

    @Override
    long viewOffset(Object owner, int index) {
        ByteBuffer bb = (ByteBuffer) owner;
        // heap buffer: array base offset + the buffer's offset into its array;
        // direct buffer: raw memory address (paired with the null arrayView base)
        long base = bb.hasArray() ? fieldOffset + bb.arrayOffset() : unsafe.getLong(bb, ADDRESS_OFFSET);
        return base + index;
    }

}
