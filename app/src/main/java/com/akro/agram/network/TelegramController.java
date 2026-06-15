package com.akro.agram.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main controller that bridges Java UI with the tgnet native layer (MTProto).
 *
 * ─── ميزات AkroGram المضافة ────────────────────────────────────────────────
 *  1. Stealth story viewing  → لا يُسجَّل viewStory عند الستيلث
 *  2. Last-seen bypass       → نحاول استرداد آخر ظهور لمن أخفاه حتى عنّا
 * ──────────────────────────────────────────────────────────────────────────
 */
public class TelegramController {

    private static TelegramController instance;
    private Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<AuthListener>    authListeners    = new ArrayList<>();
    private final List<DialogsListener> dialogsListeners = new ArrayList<>();

    // ─── كاش آخر ظهور المسترَد ──────────────────────────────────────────────
    private final Map<Long, LastSeenInfo> lastSeenCache = new HashMap<>();

    // Auth states
    public static final int AUTH_STATE_WAIT_PHONE    = 0;
    public static final int AUTH_STATE_WAIT_CODE     = 1;
    public static final int AUTH_STATE_WAIT_PASSWORD = 2;
    public static final int AUTH_STATE_AUTHORIZED    = 3;

    private int    currentAuthState = AUTH_STATE_WAIT_PHONE;
    private String currentPhone;
    private String phoneCodeHash;

    public static TelegramController getInstance() {
        if (instance == null) {
            synchronized (TelegramController.class) {
                if (instance == null) instance = new TelegramController();
            }
        }
        return instance;
    }

    static {
        try {
            System.loadLibrary("tgnet");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    // ─── Native JNI methods ──────────────────────────────────────────────────
    private native void    native_init(Context context, String filesDir, int version,
                                       String apiId, String apiHash, String deviceModel,
                                       String systemVersion, String appVersion, String lang,
                                       String langPack, String systemLang, int userId,
                                       boolean enablePushConnection);
    private native void    native_sendRequest(long requestToken, int requestClass);
    private native void    native_cancelRequest(long token, boolean notifyServer);
    private native void    native_cleanUp(boolean resetNetwork);
    private native boolean native_isConnected();
    private native long    native_getCurrentTime();

    public native void native_setAuthorizationPhone(String phone);
    public native void native_checkPhoneCode(String code);
    public native void native_checkPassword(String password);
    public native void native_logOut();

    // ─── آخر ظهور المختفين ──────────────────────────────────────────────────
    /**
     * يُرسل طلب MTProto: contacts.getStatuses أو users.getFullUser
     * ويحاول يجيب آخر ظهور حقيقي حتى لو الشخص أخفاه عنك.
     *
     * الطريقة: Telegram بيبعت was_online حتى لو privacy=nobody
     * في بعض حالات الاشتراك المميز (Premium) أو عبر تحليل updateUserStatus.
     *
     * TODO: اربط native_sendRequest بـ TL_contacts_getStatuses
     */
    public void fetchLastSeen(long userId, LastSeenCallback callback) {
        // أولاً نشيك الكاش
        LastSeenInfo cached = lastSeenCache.get(userId);
        if (cached != null && (System.currentTimeMillis() - cached.fetchedAt) < 60_000) {
            mainHandler.post(() -> callback.onResult(cached));
            return;
        }

        // TODO: native call → TL_users_getFullUser أو TL_contacts_getStatuses
        // في الوقت الحالي نرجع mock يوضح المنطق
        mainHandler.postDelayed(() -> {
            LastSeenInfo info = new LastSeenInfo();
            info.userId    = userId;
            info.fetchedAt = System.currentTimeMillis();
            // القيم دي هتيجي من native callback بعد ربط JNI
            info.wasOnline = System.currentTimeMillis() / 1000 - 1800; // 30 دقيقة فات
            info.isHidden  = true;  // صاحبه أخفى الـ last-seen
            info.label     = formatLastSeen(info.wasOnline);
            lastSeenCache.put(userId, info);
            callback.onResult(info);
        }, 500);
    }

    /**
     * يُستدعى من native layer عند وصول updateUserStatus للمستخدم
     * (حتى لو privacy=nobody، تيليجرام بيبعت الـ update للمتصلين معه)
     */
    public void onUserStatusUpdate(long userId, long wasOnline, boolean isOnline) {
        LastSeenInfo info = new LastSeenInfo();
        info.userId    = userId;
        info.fetchedAt = System.currentTimeMillis();
        info.wasOnline = wasOnline;
        info.isHidden  = false; // وصلنا بياناته فعلاً
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
        if (diff < 60)       return "آخر ظهور منذ لحظات";
        if (diff < 3600)     return "آخر ظهور منذ " + (diff / 60) + " دقيقة";
        if (diff < 86400)    return "آخر ظهور منذ " + (diff / 3600) + " ساعة";
        if (diff < 604800)   return "آخر ظهور منذ " + (diff / 86400) + " يوم";
        return "آخر ظهور منذ فترة طويلة";
    }

    // ─── Stealth Story ────────────────────────────────────────────────────────
    /**
     * شاهد القصة بدون تسجيل (لا يُرسَل stories.readStories)
     */
    public void markStoryAsReadSilently(long peerId) {
        // لا نفعل شيء — نتجاهل الـ native call عمداً
    }

    public void markStoryViewed(long peerId, int storyId) {
        // TODO: native_sendRequest(TL_stories_readStories(peerId, storyId))
    }

    // ─── Init ─────────────────────────────────────────────────────────────────
    public void init(Context ctx) {
        this.context = ctx.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("akrogram", Context.MODE_PRIVATE);
        int savedUserId = prefs.getInt("user_id", 0);
        try {
            native_init(
                context,
                context.getFilesDir().getAbsolutePath(),
                1,
                "35978619",    // ← ضع API_ID من my.telegram.org
                "7521a35c396ddecada6da6a8687a0324",  // ← ضع API_HASH من my.telegram.org
                android.os.Build.MODEL,
                android.os.Build.VERSION.RELEASE,
                "1.0",
                "ar",
                "android",
                "ar",
                savedUserId,
                true
            );
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ─── Auth ─────────────────────────────────────────────────────────────────
    public void sendPhone(String phone, AuthListener listener) {
        this.currentPhone = phone;
        addAuthListener(listener);
        try { native_setAuthorizationPhone(phone); }
        catch (Exception e) { mainHandler.post(() -> listener.onError(e.getMessage())); }
    }

    public void sendCode(String code, AuthListener listener) {
        addAuthListener(listener);
        try { native_checkPhoneCode(code); }
        catch (Exception e) { mainHandler.post(() -> listener.onError(e.getMessage())); }
    }

    public void sendPassword(String password, AuthListener listener) {
        addAuthListener(listener);
        try { native_checkPassword(password); }
        catch (Exception e) { mainHandler.post(() -> listener.onError(e.getMessage())); }
    }

    public void logOut() {
        try { native_logOut(); } catch (Exception e) { e.printStackTrace(); }
        context.getSharedPreferences("akrogram", Context.MODE_PRIVATE).edit().clear().apply();
        currentAuthState = AUTH_STATE_WAIT_PHONE;
        lastSeenCache.clear();
    }

    // ─── Native callbacks ─────────────────────────────────────────────────────
    public void onAuthStateChanged(final int state) {
        currentAuthState = state;
        mainHandler.post(() -> { for (AuthListener l : authListeners) l.onAuthStateChanged(state); });
    }

    public void onDialogsUpdated() {
        mainHandler.post(() -> { for (DialogsListener l : dialogsListeners) l.onDialogsUpdated(); });
    }

    public void onError(final String error) {
        mainHandler.post(() -> { for (AuthListener l : authListeners) l.onError(error); });
    }

    // ─── Getters ──────────────────────────────────────────────────────────────
    public int     getCurrentAuthState() { return currentAuthState; }
    public boolean isAuthorized()        { return currentAuthState == AUTH_STATE_AUTHORIZED; }
    public String  getCurrentPhone()     { return currentPhone; }

    // ─── Listeners ────────────────────────────────────────────────────────────
    public void addAuthListener(AuthListener l)       { if (!authListeners.contains(l)) authListeners.add(l); }
    public void removeAuthListener(AuthListener l)    { authListeners.remove(l); }
    public void addDialogsListener(DialogsListener l) { if (!dialogsListeners.contains(l)) dialogsListeners.add(l); }
    public void removeDialogsListener(DialogsListener l) { dialogsListeners.remove(l); }

    // ─── Data classes ─────────────────────────────────────────────────────────
    public static class LastSeenInfo {
        public long    userId;
        public long    wasOnline;   // unix timestamp
        public boolean isOnline;
        public boolean isHidden;    // هل المستخدم أخفى الـ last-seen عنك
        public String  label;       // النص الجاهز للعرض
        public long    fetchedAt;   // وقت الجلب (للكاش)
    }

    public interface LastSeenCallback { void onResult(LastSeenInfo info); }

    public interface AuthListener {
        void onAuthStateChanged(int state);
        void onError(String error);
    }

    public interface DialogsListener { void onDialogsUpdated(); }
}
