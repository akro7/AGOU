/*
 * akro_stubs.c
 * تعريفات وهمية للدوال المفقودة (image / video / voip)
 * يتم استخدامها لأن ملفات الـ native الأصلية غير متضمنة في هذا الريبو
 */

#include <jni.h>

void imageOnJNILoad(JavaVM *vm, JNIEnv *env) {
    /* stub — image processing not included */
}

void videoOnJNILoad(JavaVM *vm, JNIEnv *env) {
    /* stub — video processing not included */
}

void tgvoipOnJNILoad(JavaVM *vm, JNIEnv *env) {
    /* stub — tgvoip not included */
}
