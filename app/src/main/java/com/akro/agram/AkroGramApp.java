package com.akro.agram;

import android.app.Application;
import android.content.Context;

import com.akro.agram.network.TelegramController;

public class AkroGramApp extends Application {

    private static AkroGramApp instance;

    public static AkroGramApp getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        TelegramController.getInstance().init(this);
    }
}
