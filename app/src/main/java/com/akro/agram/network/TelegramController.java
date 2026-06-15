package com.akro.agram.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.telegram.tgnet.ConnectionsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AkroGram — TelegramController
 *
 * Bridge بين الـ UI layer وبين ConnectionsManager (tgnet native).
 * ما بيعملش native calls مباشرة — بيمرّر كل حاجة عبر ConnectionsManager.
 *
 * ─── ميزات AkroGram ───────────────────────────────────────────────────────
 *  1. Stealth story viewing  — لا يُسجَّل viewStory عند الستيلث
 *  2. Last-seen bypass       — يحاول يجيب آخر ظهور حقيقي
 * ─────────────────────────────────────────────────────────────────────────
 */
public class TelegramController {

    private static final String TAG = "AkroGram/TC";

    // ─── API credentials ──────────────────────────────────────────────────────
    // قيم افتراضية — بيتم override من الـ workflow secrets
    private static final int    API_ID   = 35978619;
    private static final String API_HASH = "7521a35c396ddecada6da6a8687a0324";

    // ─── Singleton ────────────────────────────────────────────────────────────
    private static volatile TelegramController instance;

    public static TelegramController getInstance() {
        if (instance == null) {
            synchronized (TelegramController.class) {
                if (instance == null) instance = new TelegramController();
            }
        }
        return instance;
    }

    // ─── State ────────────────────────────────────────────────────────────────
    private Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<AuthListener>    authListeners    = new ArrayList<>();
    private final List<DialogsListener> dialogsListeners = new ArrayList<>();
    private final Map<Long, LastSeenInfo> lastSeenCache  = new HashMap<>();

    public static final int AUTH_STATE_WAIT_PHONE    = 0;
    public static final int AUTH_STATE_WAIT_CODE     = 1;
    public static final int AUTH_STATE_WAIT_PASSWORD = 2;
    public static final int AUTH_STATE_AUTHORIZED    = 3;

    private int    currentAuthState = AUTH_STATE_WAIT_PHONE;
    private String currentPhone;

    // ─── تحميل الـ native library ─────────────────────────────────────────────
    static {
        try {
            System.loadLibrary("tgnet");
            Log.i(TAG, "✅ libtgnet.so loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "❌ libtgnet.so NOT found: " + e.getMessage());
        }
    }

    // ─── Init ─────────────────────────────────────────────────────────────────
    public void init(Context ctx) {
        this.context = ctx.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("akrogram", Context.MODE_PRIVATE);
        long savedUserId = prefs.getLong("user_id", 0);

        // تهيئة الـ tgnet عبر ConnectionsManager
        ConnectionsManager.getInstance(0).init(context, API_ID, API_HASH, true, savedUserId);

        if (savedUserId != 0) {
            currentAuthState = AUTH_STATE_AUTHORIZED;
        }

        Log.i(TAG, "TelegramController initialized, userId=" + savedUserId);
    }

    // ─── Auth ─────────────────────────────────────────────────────────────────
    /**
     * إرسال رقم الهاتف — stub حالياً، هيكون native call لـ TL_auth_sendCode
     */
    public void sendPhone(String phone, AuthListener listener) {
        this.currentPhone = phone;
        addAuthListener(listener);

        // TODO: ربط بـ native_sendRequest(TL_auth_sendCode)
        // حالياً بيحاكي الـ state machine للتجربة
        mainHandler.postDelayed(() -> {
            Log.i(TAG, "sendPhone: " + phone + " → wait code");
            onAuthStateChanged(AUTH_STATE_WAIT_CODE);
        }, 1000);
    }

    public void sendCode(String code, AuthListener listener) {
        addAuthListener(listener);

        // TODO: ربط بـ native_sendRequest(TL_auth_signIn)
        mainHandler.postDelayed(() -> {
            Log.i(TAG, "sendCode: " + code + " → authorized");
            onAuthStateChanged(AUTH_STATE_AUTHORIZED);
        }, 1000);
    }

    public void sendPassword(String password, AuthListener listener) {
        addAuthListener(listener);

        // TODO: ربط بـ native_sendRequest(TL_auth_checkPassword)
        mainHandler.postDelayed(() -> {
            Log.i(TAG, "sendPassword → authorized");
            onAuthStateChanged(AUTH_STATE_AUTHORIZED);
        }, 1000);
    }

    public void logOut() {
        ConnectionsManager.getInstance(0).cleanUp(true);
        if (context != null) {
            context.getSharedPreferences("akrogram", Context.MODE_PRIVATE).edit().clear().apply();
        }
        currentAuthState = AUTH_STATE_WAIT_PHONE;
        lastSeenCache.clear();
        Log.i(TAG, "Logged out");
    }

    // ─── Stealth Story ────────────────────────────────────────────────────────
    /** شوف القصة بدون تسجيل — لا يُرسَل TL_stories_readStories */
    public void markStoryAsReadSilently(long peerId) {
        // عمداً فاضية — ستيلث
        Log.d(TAG, "Stealth story view: peerId=" + peerId);
    }

    public void markStoryViewed(long peerId, int storyId) {
        // TODO: native_sendRequest(TL_stories_readStories(peerId, storyId))
    }

    // ─── Last-seen bypass ─────────────────────────────────────────────────────
    public void fetchLastSeen(long userId, LastSeenCallback callback) {
        LastSeenInfo cached = lastSeenCache.get(userId);
        if (cached != null && (System.currentTimeMillis() - cached.fetchedAt) < 60_000) {
            mainHandler.post(() -> callback.onResult(cached));
            return;
        }

        // TODO: native_sendRequest(TL_users_getFullUser)
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

    /** يُستدعى من native layer عند وصول updateUserStatus */
    public void onUserStatusUpdate(long userId, long wasOnline, boolean isOnline) {
        LastSeenInfo info = new LastSeenInfo();
        info.userId    = userId;
        info.fetchedAt = System.currentTimeMillis();
        info.wasOnline = wasOnline;
        info.isHidden  = false;
        info.isOnline  = isOnline;
        info.label     = isOnline ? "متصل الآن" : formatLastSeen(wasOnline);
        lastSeenCache.put(userId, info);
        mainHandler.post(() -> {
            for (DialogsListener l : dialogsListeners) l.onDialogsUpdated();
        });
    }

    public LastSeenInfo getCachedLastSeen(long userId) {
        return lastSeenCache.get(userId);
    }

    private String formatLastSeen(long unixTime) {
        long diff = System.currentTimeMillis() / 1000 - unixTime;
        if (diff < 60)     return "آخر ظهور منذ لحظات";
        if (diff < 3600)   return "آخر ظهور منذ " + (diff / 60) + " دقيقة";
        if (diff < 86400)  return "آخر ظهور منذ " + (diff / 3600) + " ساعة";
        if (diff < 604800) return "آخر ظهور منذ " + (diff / 86400) + " يوم";
        return "آخر ظهور منذ فترة طويلة";
    }

    // ─── Callbacks من native (عبر ConnectionsManager) ────────────────────────
    public void onAuthStateChanged(final int state) {
        currentAuthState = state;
        if (state == AUTH_STATE_AUTHORIZED && context != null) {
            // حفظ userId
            context.getSharedPreferences("akrogram", Context.MODE_PRIVATE)
                   .edit().putLong("user_id", 1).apply(); // TODO: real user ID
        }
        mainHandler.post(() -> {
            for (AuthListener l : new ArrayList<>(authListeners)) {
                l.onAuthStateChanged(state);
            }
        });
    }

    public void onDialogsUpdated() {
        mainHandler.post(() -> {
            for (DialogsListener l : new ArrayList<>(dialogsListeners)) {
                l.onDialogsUpdated();
            }
        });
    }

    public void onError(final String error) {
        mainHandler.post(() -> {
            for (AuthListener l : new ArrayList<>(authListeners)) {
                l.onError(error);
            }
        });
    }

    // ─── Getters ──────────────────────────────────────────────────────────────
    public int     getCurrentAuthState() { return currentAuthState; }
    public boolean isAuthorized()        { return currentAuthState == AUTH_STATE_AUTHORIZED; }
    public String  getCurrentPhone()     { return currentPhone; }

    public int getConnectionState() {
        return ConnectionsManager.getInstance(0).getConnectionState();
    }

    // ─── Listeners ────────────────────────────────────────────────────────────
    public void addAuthListener(AuthListener l)          { if (!authListeners.contains(l)) authListeners.add(l); }
    public void removeAuthListener(AuthListener l)       { authListeners.remove(l); }
    public void addDialogsListener(DialogsListener l)    { if (!dialogsListeners.contains(l)) dialogsListeners.add(l); }
    public void removeDialogsListener(DialogsListener l) { dialogsListeners.remove(l); }

    // ─── Data classes ─────────────────────────────────────────────────────────
    public static class LastSeenInfo {
        public long    userId;
        public long    wasOnline;
        public boolean isOnline;
        public boolean isHidden;
        public String  label;
        public long    fetchedAt;
    }

    public interface LastSeenCallback   { void onResult(LastSeenInfo info); }
    public interface AuthListener {
        void onAuthStateChanged(int state);
        void onError(String error);
    }
    public interface DialogsListener    { void onDialogsUpdated(); }
}
