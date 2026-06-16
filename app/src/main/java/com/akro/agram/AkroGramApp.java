package com.akro.agram;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.akro.agram.network.TelegramController;
import org.telegram.messenger.ApplicationLoader;

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

        // تهيئة applicationContext اللي بيحتاجه ConnectionsManager
        ApplicationLoader.applicationContext = getApplicationContext();

        // تحميل الـ native library يدوياً
        try {
            System.loadLibrary("tgnet");
            Log.i(TAG, "✅ libtgnet.so loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "❌ libtgnet.so failed: " + e.getMessage());
        }

        // تهيئة TelegramController
        try {
            TelegramController.getInstance().init(this);
            Log.i(TAG, "✅ TelegramController initialized");
        } catch (Throwable t) {
            Log.e(TAG, "❌ TelegramController init failed: " + t.getMessage());
        }
    }
}
