package com.akro.agram;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.akro.agram.network.TelegramController;

public class AkroGramApp extends Application {

    private static final String TAG = "AkroGram/App";
    private static AkroGramApp instance;

    public static AkroGramApp getInstance() { return instance; }
    public static Context getContext() {
        return instance != null ? instance.getApplicationContext() : null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // تهيئة tgnet عبر reflection بدون تشغيل ApplicationLoader كامل
        try {
            org.telegram.messenger.ApplicationLoader.applicationContext = getApplicationContext();
            Class<?> nativeLoader = Class.forName("org.telegram.messenger.NativeLoader");
            java.lang.reflect.Method initNativeLibs = nativeLoader.getDeclaredMethod("initNativeLibs", Context.class);
            initNativeLibs.setAccessible(true);
            initNativeLibs.invoke(null, getApplicationContext());
            Log.i(TAG, "✅ NativeLoader initialized");
        } catch (Throwable t) {
            Log.e(TAG, "❌ NativeLoader failed: " + t.getMessage());
        }

        try {
            TelegramController.getInstance().init(this);
            Log.i(TAG, "✅ TelegramController initialized");
        } catch (Throwable t) {
            Log.e(TAG, "❌ TelegramController init failed: " + t.getMessage());
        }
    }
}
