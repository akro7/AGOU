package org.telegram.tgnet;

import java.nio.ByteBuffer;

public class NativeByteBuffer {

    private long address;

    public static native long native_getFreeBuffer(int length);
    public static native int  native_limit(long address);
    public static native int  native_position(long address);
    public static native void native_reuse(long address);
    public static native ByteBuffer native_getJavaByteBuffer(long address);

    public NativeByteBuffer(int size) {
        address = native_getFreeBuffer(size);
    }

    public int limit() {
        return native_limit(address);
    }

    public int position() {
        return native_position(address);
    }

    public void reuse() {
        native_reuse(address);
    }

    public ByteBuffer getJavaByteBuffer() {
        return native_getJavaByteBuffer(address);
    }

    public long address() {
        return address;
    }
}
