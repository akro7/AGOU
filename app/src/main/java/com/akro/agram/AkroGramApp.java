package com.akro.agram;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import com.akro.agram.network.TelegramController;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.tgnet.ConnectionsManager;

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
        ApplicationLoader.applicationContext = getApplicationContext();

        try {
            System.loadLibrary("tgnet");
            Log.i(TAG, "✅ tgnet loaded");
        } catch (Throwable t) {
            Log.e(TAG, "❌ tgnet load failed: " + t.getMessage());
        }

        // تهيئة ConnectionsManager بالبيانات المطلوبة
        try {
            ConnectionsManager.native_init(
                0,                          // account index
                BuildConfig.VERSION_CODE,   // version
                (int)(System.currentTimeMillis() / 1000), // time
                Build.MODEL,               // device
                Build.VERSION.RELEASE,     // os
                "AkroGram",               // app name
                "1.0",                    // app version
                getFilesDir().getAbsolutePath(), // files dir
                getCacheDir().getAbsolutePath(), // cache dir
                null,                     // log path
                "en",                     // lang code
                "en",                     // system lang
                "",                       // config path
                Build.MODEL,              // vendor name
                false,                    // enable push
                35978619,                 // API_ID
                false                     // test backend
            );
            Log.i(TAG, "✅ ConnectionsManager initialized");
        } catch (Throwable t) {
            Log.e(TAG, "❌ CM native_init failed: " + t.getMessage());
        }

        try {
            TelegramController.getInstance().init(this);
        } catch (Throwable t) {
            Log.e(TAG, "❌ TC init failed: " + t.getMessage());
        }
    }
}
