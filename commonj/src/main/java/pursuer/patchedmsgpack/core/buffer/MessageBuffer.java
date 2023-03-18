//
// MessagePack for Java
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package pursuer.patchedmsgpack.core.buffer;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static pursuer.patchedmsgpack.core.Preconditions.checkNotNull;

/**
 * MessageBuffer class is an abstraction of memory with fast methods to serialize and deserialize primitive values
 * to/from the memory. All MessageBuffer implementations ensure short/int/float/long/double values are written in
 * big-endian order.
 * <p>
 * Applications can allocate a new buffer using {@link #allocate(int)} method, or wrap an byte array or ByteBuffer
 * using {@link #wrap(byte[], int, int)} methods. {@link #wrap(ByteBuffer)} method supports both direct buffers and
 * array-backed buffers.
 * <p>
 * MessageBuffer class itself is optimized for little-endian CPU archtectures so that JVM (HotSpot) can take advantage
 * of the fastest JIT format which skips TypeProfile checking. To ensure this performance, applications must not import
 * unnecessary classes such as MessagePackBE. On big-endian CPU archtectures, it automatically uses a subclass that
 * includes TypeProfile overhead but still faster than stndard ByteBuffer class. On JVMs older than Java 7 and JVMs
 * without Unsafe API (such as Android), implementation falls back to an universal implementation that uses ByteBuffer
 * internally.
 */
public abstract class MessageBuffer
{
    static final boolean isUniversalBuffer;
    static final int javaVersion = getJavaVersion();

    /**
     * Reference to MessageBuffer Constructors
     */
    private static final Constructor<?> mbArrConstructor;

    /**
     * The offset from the object memory header to its byte array data
     */
    static final int ARRAY_BYTE_BASE_OFFSET;

    private static final String UNIVERSAL_MESSAGE_BUFFER = "pursuer.patchedmsgpack.core.buffer.MessageBufferU";
    private static final String DEFAULT_MESSAGE_BUFFER = "pursuer.patchedmsgpack.core.buffer.MessageBuffer";

    static {
        boolean useUniversalBuffer = true;
        int arrayByteBaseOffset = 16;


        // Initialize the static fields
        ARRAY_BYTE_BASE_OFFSET = arrayByteBaseOffset;

        // Switch MessageBuffer implementation according to the environment
        isUniversalBuffer = useUniversalBuffer;
        String bufferClsName;
        if (isUniversalBuffer) {
            bufferClsName = UNIVERSAL_MESSAGE_BUFFER;
        }
        else {
            // Check the endian of this CPU
            boolean isLittleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
            bufferClsName = DEFAULT_MESSAGE_BUFFER ;
        }

        if (DEFAULT_MESSAGE_BUFFER.equals(bufferClsName)) {
            // No need to use reflection here, we're not using a MessageBuffer subclass.
            mbArrConstructor = null;
        }
        else {
            try {
                // We need to use reflection here to find MessageBuffer implementation classes because
                // importing these classes creates TypeProfile and adds some overhead to method calls.

                // MessageBufferX (default, BE or U) class
                Class<?> bufferCls = Class.forName(bufferClsName);

                // MessageBufferX(byte[]) constructor
                Constructor<?> mbArrCstr = bufferCls.getDeclaredConstructor(byte[].class, int.class, int.class);
                mbArrCstr.setAccessible(true);
                mbArrConstructor = mbArrCstr;

                // MessageBufferX(ByteBuffer) constructor
                Constructor<?> mbBBCstr = bufferCls.getDeclaredConstructor(ByteBuffer.class);
                mbBBCstr.setAccessible(true);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                throw new RuntimeException(e); // No more fallback exists if MessageBuffer constructors are inaccessible
            }
        }
    }

    private static int getJavaVersion()
    {
        String javaVersion = System.getProperty("java.specification.version", "");
        int dotPos = javaVersion.indexOf('.');
        if (dotPos != -1) {
            try {
                int major = Integer.parseInt(javaVersion.substring(0, dotPos));
                int minor = Integer.parseInt(javaVersion.substring(dotPos + 1));
                return major > 1 ? major : minor;
            }
            catch (NumberFormatException e) {
                e.printStackTrace(System.err);
            }
        }
        else {
            try {
                return Integer.parseInt(javaVersion);
            }
            catch (NumberFormatException e) {
                e.printStackTrace(System.err);
            }
        }
        return 6;
    }

    /**
     * Base object for resolving the relative address of the raw byte array.
     * If base == null, the address value is a raw memory address
     */
    protected final Object base;

    /**
     * Head address of the underlying memory. If base is null, the address is a direct memory address, and if not,
     * it is the relative address within an array object (base)
     */
    protected final long address;

    /**
     * Size of the underlying memory
     */
    protected final int size;

    /**
     * Reference is used to hold a reference to an object that holds the underlying memory so that it cannot be
     * released by the garbage collector.
     */
    protected final ByteBuffer reference;

    /**
     * Allocates a new MessageBuffer backed by a byte array.
     *
     * @throws IllegalArgumentException If the capacity is a negative integer
     *
     */
    public static MessageBuffer allocate(int size)
    {
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
        return wrap(new byte[size]);
    }

    /**
     * Wraps a byte array into a MessageBuffer.
     *
     * The new MessageBuffer will be backed by the given byte array. Modifications to the new MessageBuffer will cause the byte array to be modified and vice versa.
     *
     * The new buffer's size will be array.length. hasArray() will return true.
     *
     * @param array the byte array that will gack this MessageBuffer
     * @return a new MessageBuffer that wraps the given byte array
     *
     */
    public static MessageBuffer wrap(byte[] array)
    {
        return newMessageBuffer(array, 0, array.length);
    }

    /**
     * Wraps a byte array into a MessageBuffer.
     *
     * The new MessageBuffer will be backed by the given byte array. Modifications to the new MessageBuffer will cause the byte array to be modified and vice versa.
     *
     * The new buffer's size will be length. hasArray() will return true.
     *
     * @param array the byte array that will gack this MessageBuffer
     * @param offset The offset of the subarray to be used; must be non-negative and no larger than array.length
     * @param length The length of the subarray to be used; must be non-negative and no larger than array.length - offset
     * @return a new MessageBuffer that wraps the given byte array
     *
     */
    public static MessageBuffer wrap(byte[] array, int offset, int length)
    {
        return newMessageBuffer(array, offset, length);
    }


    /**
     * Creates a new MessageBuffer instance backed by a java heap array
     *
     * @param arr
     * @return
     */
    private static MessageBuffer newMessageBuffer(byte[] arr, int off, int len)
    {
        checkNotNull(arr);
        return newInstance(mbArrConstructor, arr, off, len);
    }


    /**
     * Creates a new MessageBuffer instance
     *
     * @param constructor A MessageBuffer constructor
     * @return new MessageBuffer instance
     */
    private static MessageBuffer newInstance(Constructor<?> constructor, Object... args)
    {
        try {
            // We need to use reflection to create MessageBuffer instances in order to prevent TypeProfile generation for getInt method. TypeProfile will be
            // generated to resolve one of the method references when two or more classes overrides the method.
            return (MessageBuffer) constructor.newInstance(args);
        }
        catch (InstantiationException e) {
            // should never happen
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e) {
            // should never happen unless security manager restricts this reflection
            throw new IllegalStateException(e);
        }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                // underlying constructor may throw RuntimeException
                throw (RuntimeException) e.getCause();
            }
            else if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
            // should never happen
            throw new IllegalStateException(e.getCause());
        }
    }

    public static void releaseBuffer(MessageBuffer buffer)
    {
        if (isUniversalBuffer || buffer.hasArray()) {
            // We have nothing to do. Wait until the garbage-collector collects this array object
        }
    }

    /**
     * Create a MessageBuffer instance from an java heap array
     *
     * @param arr
     * @param offset
     * @param length
     */
    MessageBuffer(byte[] arr, int offset, int length)
    {
        this.base = arr;  // non-null is already checked at newMessageBuffer
        this.address = ARRAY_BYTE_BASE_OFFSET + offset;
        this.size = length;
        this.reference = null;
    }

    /**
     * Create a MessageBuffer instance from a given ByteBuffer instance
     *
     * @param bb
     */
    MessageBuffer(ByteBuffer bb)
    {
        if (bb.hasArray()) {
            this.base = bb.array();
            this.address = ARRAY_BYTE_BASE_OFFSET + bb.arrayOffset() + bb.position();
            this.size = bb.remaining();
            this.reference = null;
        }
        else {
            throw new IllegalArgumentException("Only the array-backed ByteBuffer is supported");
        }
    }

    protected MessageBuffer(Object base, long address, int length)
    {
        this.base = base;
        this.address = address;
        this.size = length;
        this.reference = null;
    }

    /**
     * Gets the size of the buffer.
     *
     * MessageBuffer doesn't have limit unlike ByteBuffer. Instead, you can use {@link #slice(int, int)} to get a
     * part of the buffer.
     *
     * @return number of bytes
     */
    public int size()
    {
        return size;
    }

    public abstract MessageBuffer slice(int offset, int length);

    public abstract byte getByte(int index);

    public abstract boolean getBoolean(int index);

    public abstract short getShort(int index);
    /**
     * Read a big-endian int value at the specified index
     *
     * @param index
     * @return
     */
    public abstract int getInt(int index);

    public float getFloat(int index)
    {
        return Float.intBitsToFloat(getInt(index));
    }

    public abstract long getLong(int index);

    public double getDouble(int index)
    {
        return Double.longBitsToDouble(getLong(index));
    }

    public abstract void getBytes(int index, byte[] dst, int dstOffset, int length);

    public void getBytes(int index, int len, ByteBuffer dst)
    {
        if (dst.remaining() < len) {
            throw new BufferOverflowException();
        }
        ByteBuffer src = sliceAsByteBuffer(index, len);
        dst.put(src);
    }

    public abstract void putByte(int index, byte v);

    public abstract void putBoolean(int index, boolean v);

    public abstract void putShort(int index, short v);

    /**
     * Write a big-endian integer value to the memory
     *
     * @param index
     * @param v
     */
    public abstract void putInt(int index, int v);

    public void putFloat(int index, float v)
    {
        putInt(index, Float.floatToRawIntBits(v));
    }

    public abstract void putLong(int index, long l);

    public void putDouble(int index, double v)
    {
        putLong(index, Double.doubleToRawLongBits(v));
    }

    public abstract void putBytes(int index, byte[] src, int srcOffset, int length);

    public abstract void putByteBuffer(int index, ByteBuffer src, int len);

    public abstract void putMessageBuffer(int index, MessageBuffer src, int srcOffset, int len);

    /**
     * Create a ByteBuffer view of the range [index, index+length) of this memory
     *
     * @param index
     * @param length
     * @return
     */
    public ByteBuffer sliceAsByteBuffer(int index, int length)
    {
        return ByteBuffer.wrap((byte[]) base, (int) ((address - ARRAY_BYTE_BASE_OFFSET) + index), length);
    }

    /**
     * Get a ByteBuffer view of this buffer
     *
     * @return
     */
    public ByteBuffer sliceAsByteBuffer()
    {
        return sliceAsByteBuffer(0, size());
    }

    public boolean hasArray()
    {
        return base != null;
    }

    /**
     * Get a copy of this buffer
     *
     * @return
     */
    public abstract byte[] toByteArray();

    public byte[] array()
    {
        return (byte[]) base;
    }

    public int arrayOffset()
    {
        return (int) address - ARRAY_BYTE_BASE_OFFSET;
    }

    /**
     * Copy this buffer contents to another MessageBuffer
     *
     * @param index
     * @param dst
     * @param offset
     * @param length
     */
    public abstract void copyTo(int index, MessageBuffer dst, int offset, int length);

    public String toHexString(int offset, int length)
    {
        StringBuilder s = new StringBuilder();
        for (int i = offset; i < length; ++i) {
            if (i != offset) {
                s.append(" ");
            }
            s.append(String.format("%02x", getByte(i)));
        }
        return s.toString();
    }
}
