package com.akro.agram.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.akro.agram.network.auth.TelegramAuthService;
import org.telegram.tgnet.ConnectionsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelegramController {

    private static final String TAG = "AkroGram/TC";

    private static final int    API_ID   = 35978619;
    private static final String API_HASH = "7521a35c396ddecada6da6a8687a0324";

    private static volatile TelegramController instance;
    public static TelegramController getInstance() {
        if (instance == null) {
            synchronized (TelegramController.class) {
                if (instance == null) instance = new TelegramController();
            }
        }
        return instance;
    }

    private Context context;
    private TelegramAuthService authService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<AuthListener>      authListeners    = new ArrayList<>();
    private final List<DialogsListener>   dialogsListeners = new ArrayList<>();
    private final Map<Long, LastSeenInfo> lastSeenCache    = new HashMap<>();

    public static final int AUTH_STATE_WAIT_PHONE    = 0;
    public static final int AUTH_STATE_WAIT_CODE     = 1;
    public static final int AUTH_STATE_WAIT_PASSWORD = 2;
    public static final int AUTH_STATE_AUTHORIZED    = 3;

    private int    currentAuthState = AUTH_STATE_WAIT_PHONE;
    private String currentPhone;

    static {
        try {
            System.loadLibrary("tgnet");
            Log.i(TAG, "✅ libtgnet.so loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "⚠️ libtgnet.so not found — network-only mode");
        }
    }

    public void init(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.authService = new TelegramAuthService(context, API_ID, API_HASH);
        this.authService.restoreState();

        SharedPreferences prefs = context.getSharedPreferences("akrogram", Context.MODE_PRIVATE);
        long savedUserId = prefs.getLong("user_id", 0);

        try {
            ConnectionsManager.getInstance(0).setUserId(savedUserId);
        } catch (Throwable t) {
            Log.w(TAG, "tgnet setUserId skipped: " + t.getMessage());
        }

        if (savedUserId != 0) {
            currentAuthState = AUTH_STATE_AUTHORIZED;
        }
    }

    public void sendPhone(String phone, AuthListener listener) {
        this.currentPhone = phone;
        addAuthListener(listener);

        authService.sendCode(phone, new TelegramAuthService.AuthCallback() {
            @Override
            public void onCodeSent(String p, String codeType, int codeLength) {
                Log.i(TAG, "Code sent via " + codeType + ", length=" + codeLength);
                mainHandler.post(() -> onAuthStateChanged(AUTH_STATE_WAIT_CODE));
            }

            @Override
            public void onAuthorized(long userId, String firstName, String username) {
                mainHandler.post(() -> onAuthStateChanged(AUTH_STATE_AUTHORIZED));
            }

            @Override
            public void onSignUpRequired() {
                authService.signUp("AkroGram", "User", this);
            }

            @Override
            public void onError(int code, String message) {
                mainHandler.post(() -> {
                    for (AuthListener l : new ArrayList<>(authListeners)) {
                        l.onError(message);
                    }
                });
            }
        });
    }

    public void sendCode(String code, AuthListener listener) {
        addAuthListener(listener);

        authService.signIn(code, new TelegramAuthService.AuthCallback() {
            @Override
            public void onCodeSent(String phone, String codeType, int codeLength) {}

            @Override
            public void onAuthorized(long userId, String firstName, String username) {
                Log.i(TAG, "✅ Authorized! userId=" + userId + " name=" + firstName);
                try { ConnectionsManager.getInstance(0).setUserId(userId); } catch (Throwable ignored) {}
                mainHandler.post(() -> onAuthStateChanged(AUTH_STATE_AUTHORIZED));
            }

            @Override
            public void onSignUpRequired() {
                authService.signUp("", "", this);
            }

            @Override
            public void onError(int code, String message) {
                mainHandler.post(() -> {
                    for (AuthListener l : new ArrayList<>(authListeners)) {
                        l.onError(message);
                    }
                });
            }
        });
    }

    public void sendPassword(String password, AuthListener listener) {
        addAuthListener(listener);
        mainHandler.post(() -> listener.onError("2FA غير مدعوم بعد"));
    }

    public void logOut() {
        // ← التعديل: حذف cleanUp واستبداله بإعادة تعيين الـ userId فقط
        try {
            ConnectionsManager.getInstance(0).setUserId(0);
        } catch (Throwable ignored) {}
        if (context != null) {
            context.getSharedPreferences("akrogram", Context.MODE_PRIVATE).edit().clear().apply();
        }
        currentAuthState = AUTH_STATE_WAIT_PHONE;
        lastSeenCache.clear();
    }

    public void markStoryAsReadSilently(long peerId) {}
    public void markStoryViewed(long peerId, int storyId) {}

    public void fetchLastSeen(long userId, LastSeenCallback callback) {
        LastSeenInfo cached = lastSeenCache.get(userId);
        if (cached != null && (System.currentTimeMillis() - cached.fetchedAt) < 60_000) {
            mainHandler.post(() -> callback.onResult(cached));
            return;
        }
        mainHandler.postDelayed(() -> {
            LastSeenInfo info = new LastSeenInfo();
            info.userId    = userId;
            info.fetchedAt = System.currentTimeMillis();
            info.wasOnline = System.currentTimeMillis() / 1000 - 1800;
            info.isHidden  = true;
            info.label     = formatLastSeen(info.wasOnline);
            lastSeenCache.put(userId, info);
            callback.onResult(info);
        }, 500);
    }

    public void onUserStatusUpdate(long userId, long wasOnline, boolean isOnline) {
        LastSeenInfo info = new LastSeenInfo();
        info.userId    = userId;
        info.fetchedAt = System.currentTimeMillis();
        info.wasOnline = wasOnline;
        info.isOnline  = isOnline;
        info.label     = isOnline ? "متصل الآن" : formatLastSeen(wasOnline);
        lastSeenCache.put(userId, info);
        mainHandler.post(() -> {
            for (DialogsListener l : new ArrayList<>(dialogsListeners)) l.onDialogsUpdated();
        });
    }

    public LastSeenInfo getCachedLastSeen(long userId) { return lastSeenCache.get(userId); }

    private String formatLastSeen(long unixTime) {
        long diff = System.currentTimeMillis() / 1000 - unixTime;
        if (diff < 60)     return "آخر ظهور منذ لحظات";
        if (diff < 3600)   return "آخر ظهور منذ " + (diff / 60) + " دقيقة";
        if (diff < 86400)  return "آخر ظهور منذ " + (diff / 3600) + " ساعة";
        if (diff < 604800) return "آخر ظهور منذ " + (diff / 86400) + " يوم";
        return "آخر ظهور منذ فترة طويلة";
    }

    public void onAuthStateChanged(int state) {
        currentAuthState = state;
        mainHandler.post(() -> {
            for (AuthListener l : new ArrayList<>(authListeners)) l.onAuthStateChanged(state);
        });
    }

    public void onDialogsUpdated() {
        mainHandler.post(() -> {
            for (DialogsListener l : new ArrayList<>(dialogsListeners)) l.onDialogsUpdated();
        });
    }

    public void onError(String error) {
        mainHandler.post(() -> {
            for (AuthListener l : new ArrayList<>(authListeners)) l.onError(error);
        });
    }

    public int     getCurrentAuthState() { return currentAuthState; }
    public boolean isAuthorized()        { return currentAuthState == AUTH_STATE_AUTHORIZED; }
    public String  getCurrentPhone()     { return currentPhone; }

    public void addAuthListener(AuthListener l)          { if (!authListeners.contains(l)) authListeners.add(l); }
    public void removeAuthListener(AuthListener l)       { authListeners.remove(l); }
    public void addDialogsListener(DialogsListener l)    { if (!dialogsListeners.contains(l)) dialogsListeners.add(l); }
    public void removeDialogsListener(DialogsListener l) { dialogsListeners.remove(l); }

    public static class LastSeenInfo {
        public long userId, wasOnline, fetchedAt;
        public boolean isOnline, isHidden;
        public String label;
    }

    public interface LastSeenCallback { void onResult(LastSeenInfo info); }
    public interface AuthListener {
        void onAuthStateChanged(int state);
        void onError(String error);
    }
    public interface DialogsListener { void onDialogsUpdated(); }
}
