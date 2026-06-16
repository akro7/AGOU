package com.akro.agram;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.akro.agram.network.TelegramController;

public class AkroGramApp extends Application {

    private static final String TAG = "AkroGram/App";
    private static AkroGramApp instance;

    public static AkroGramApp getInstance() { return instance; }
    public static Context getContext() { return instance.getApplicationContext(); }

    @Override
    public void onCreate() {
        super.onCreate(); // Application.onCreate() فقط — مش ApplicationLoader
        instance = this;

        try {
            TelegramController.getInstance().init(this);
            Log.i(TAG, "✅ TelegramController initialized");
        } catch (Throwable t) {
            Log.e(TAG, "❌ Init failed: " + t.getMessage(), t);
        }
    }
}
