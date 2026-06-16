package com.akro.agram;

import android.content.Context;
import com.akro.agram.network.TelegramController;
import org.telegram.messenger.ApplicationLoader;

public class AkroGramApp extends ApplicationLoader {

    private static AkroGramApp instance;

    public static AkroGramApp getInstance() { return instance; }
    public static Context getContext() { return instance.getApplicationContext(); }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate(); // الـ ApplicationLoader بيعمل init للـ tgnet هنا
        TelegramController.getInstance().init(this);
    }
}
