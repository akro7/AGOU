package org.telegram.messenger;

import java.nio.ByteBuffer;

/**
 * Stub للـ Utilities class — الـ native methods متعرّفة في jni.c
 */
public class Utilities {

    public static native void   aesIgeEncryption(ByteBuffer buffer, byte[] key, byte[] iv, boolean encrypt, int offset, int length);
    public static native void   aesIgeEncryptionByteArray(byte[] buffer, byte[] key, byte[] iv, boolean encrypt, int offset, int length);
    public static native int    pbkdf2(byte[] password, byte[] salt, byte[] dst, int iterations);
    public static native void   aesCtrDecryption(ByteBuffer buffer, byte[] key, byte[] iv, int offset, int length);
    public static native void   aesCtrDecryptionByteArray(byte[] buffer, byte[] key, byte[] iv, int offset, long length, int fileOffset);
    public static native void   aesCbcEncryptionByteArray(byte[] buffer, byte[] key, byte[] iv, int offset, int length, int fileOffset, int encrypt);
    public static native void   aesCbcEncryption(ByteBuffer buffer, byte[] key, byte[] iv, int offset, int length, int encrypt);
    public static native long   getDirSize(String path, int docType, boolean subdirs);
    public static native long   getLastUsageFileTime(String path);
    public static native void   clearDir(String path, int docType, long time, boolean subdirs);
}
