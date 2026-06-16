package com.akro.agram;

import android.content.Context;
import android.util.Log;
import com.akro.agram.network.TelegramController;
import org.telegram.messenger.ApplicationLoader;

public class AkroGramApp extends ApplicationLoader {

    private static final String TAG = "AkroGram/App";
    private static AkroGramApp instance;

    public static AkroGramApp getInstance() { return instance; }
    public static Context getContext() {
        return instance != null ? instance.getApplicationContext() : null;
    }

    @Override
    public void onCreate() {
        instance = this;
        
        // ضروري بدون try/catch عشان يهيأ libtgnet.so
        super.onCreate();

        // بعد ما الـ tgnet اتهيأ، شغّل الـ controller بتاعنا
        try {
            TelegramController.getInstance().init(this);
            Log.i(TAG, "✅ TelegramController initialized");
        } catch (Throwable t) {
            Log.e(TAG, "❌ TelegramController init failed: " + t.getMessage());
        }
    }
}
