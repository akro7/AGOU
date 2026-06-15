package org.telegram.tgnet;

import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Bridge بين الـ Java layer والـ tgnet native (libtgnet.so).
 * الـ native methods دي متسجلة في TgNetWrapper.cpp عن طريق JNI_RegisterNatives.
 *
 * AkroGram — org.telegram.tgnet.ConnectionsManager
 */
public class ConnectionsManager {

    private static final String TAG = "AkroGram/CM";
    private static final int MAX_ACCOUNT_COUNT = 4;

    // ─── Connection states ────────────────────────────────────────────────────
    public static final int ConnectionStateConnecting         = 1;
    public static final int ConnectionStateWaitingForNetwork  = 2;
    public static final int ConnectionStateConnected          = 3;
    public static final int ConnectionStateUpdating           = 4;

    // ─── Singleton instances (one per account) ────────────────────────────────
    private static volatile ConnectionsManager[] instances = new ConnectionsManager[MAX_ACCOUNT_COUNT];
    private final int instanceNum;

    private ConnectionsManager(int num) {
        this.instanceNum = num;
    }

    public static ConnectionsManager getInstance(int num) {
        if (instances[num] == null) {
            synchronized (ConnectionsManager.class) {
                if (instances[num] == null) {
                    instances[num] = new ConnectionsManager(num);
                }
            }
        }
        return instances[num];
    }

    // ─── Init ─────────────────────────────────────────────────────────────────
    /**
     * يهيّئ الـ tgnet layer.
     * يُستدعى مرة واحدة من AkroGramApp (أو TelegramController).
     */
    public void init(Context context, int apiId, String apiHash,
                     boolean enablePush, long savedUserId) {
        try {
            native_setJava(true);
            native_init(
                instanceNum,
                /*version*/       1,
                /*layer*/         176,
                apiId,
                Build.MODEL,
                Build.VERSION.RELEASE,
                "1.0",
                "ar",
                "ar",
                context.getFilesDir().getAbsolutePath(),
                context.getFilesDir().getAbsolutePath() + "/tgnet.log",
                "",        // regId
                "",        // cFingerprint
                "",        // installerId
                context.getPackageName(),
                0,         // timezoneOffset
                savedUserId,
                false,     // userPremium
                enablePush,
                true,      // hasNetwork
                0,         // networkType
                1          // performanceClass
            );
            Log.i(TAG, "✅ tgnet native_init OK (instance=" + instanceNum + ")");
        } catch (Throwable t) {
            Log.e(TAG, "❌ native_init failed: " + t.getMessage(), t);
        }
    }

    // ─── Native methods (registered by TgNetWrapper.cpp) ─────────────────────
    public static native void native_setJava(boolean useJavaByteBuffers);

    public static native void native_init(
        int instanceNum, int version, int layer, int apiId,
        String deviceModel, String systemVersion, String appVersion,
        String langCode, String systemLangCode,
        String configPath, String logPath,
        String regId, String cFingerprint,
        String installerId, String packageId,
        int timezoneOffset, long userId, boolean userPremium,
        boolean enablePushConnection, boolean hasNetwork,
        int networkType, int performanceClass
    );

    public static native long    native_getCurrentTimeMillis(int instanceNum);
    public static native int     native_getCurrentTime(int instanceNum);
    public static native int     native_getCurrentPingTime(int instanceNum);
    public static native int     native_getCurrentDatacenterId(int instanceNum);
    public static native long    native_getCurrentAuthKeyId(int instanceNum);
    public static native int     native_isTestBackend(int instanceNum);
    public static native int     native_getTimeDifference(int instanceNum);
    public static native void    native_sendRequest(int instanceNum, long object, int flags, int datacenterId, int connectionType, boolean immediate, int token);
    public static native void    native_cancelRequest(int instanceNum, int token, boolean notifyServer);
    public static native void    native_cleanUp(int instanceNum, boolean resetKeys);
    public static native void    native_cancelRequestsForGuid(int instanceNum, int guid);
    public static native void    native_bindRequestToGuid(int instanceNum, int requestToken, int guid);
    public static native void    native_applyDatacenterAddress(int instanceNum, int datacenterId, String ipAddress, int port);
    public static native void    native_setProxySettings(int instanceNum, String address, int port, String username, String password, String secret);
    public static native int     native_getConnectionState(int instanceNum);
    public static native void    native_setUserId(int instanceNum, long id);
    public static native void    native_setLangCode(int instanceNum, String langCode);
    public static native void    native_setRegId(int instanceNum, String regId);
    public static native void    native_setSystemLangCode(int instanceNum, String langCode);
    public static native void    native_switchBackend(int instanceNum, boolean restart);
    public static native void    native_pauseNetwork(int instanceNum);
    public static native void    native_resumeNetwork(int instanceNum, boolean partial);
    public static native void    native_updateDcSettings(int instanceNum);
    public static native void    native_moveDatacenter(int instanceNum, int datacenterId);
    public static native void    native_setIpStrategy(int instanceNum, byte value);
    public static native void    native_setNetworkAvailable(int instanceNum, boolean value, int networkType, boolean slow);
    public static native void    native_setPushConnectionEnabled(int instanceNum, boolean value);
    public static native void    native_applyDnsConfig(int instanceNum, long address, String phone, int date);
    public static native long    native_checkProxy(int instanceNum, String address, int port, String username, String password, String secret, RequestTimeDelegate requestTimeFunc);
    public static native void    native_onHostNameResolved(String host, long address, String ip);
    public static native void    native_discardConnection(int instanceNum, int datacenterId, int connectionType);
    public static native void    native_failNotRunningRequest(int instanceNum, int token);
    public static native void    native_receivedIntegrityCheckClassic(int instanceNum, int requestToken, String nonce, String token);
    public static native void    native_receivedCaptchaResult(int instanceNum, int[] requestTokens, String token);
    public static native boolean native_isGoodPrime(byte[] prime, int g);

    // ─── Callbacks from native (called by tgnet C++ via JNI) ─────────────────
    public static void onRequestClear(int instanceNum, int token, boolean byTimeout) {}
    public static void onRequestComplete(int instanceNum, int token, long ptr, int errorCode, String errorText, int networkType, long responseTime, long msgId, int dcId) {}
    public static void onRequestWriteToSocket(int instanceNum, int token) {}
    public static void onRequestQuickAck(int instanceNum, int token) {}
    public static void onUnparsedMessageReceived(long ptr, int instanceNum, long reqMsgId) {}
    public static void onUpdate(int instanceNum) {}
    public static void onSessionCreated(int instanceNum) {}
    public static void onLogout(int instanceNum) {}
    public static void onConnectionStateChanged(int state, int instanceNum) {
        Log.d(TAG, "onConnectionStateChanged state=" + state + " instance=" + instanceNum);
    }
    public static void onInternalPushReceived(int instanceNum) {}
    public static void onUpdateConfig(long ptr, int instanceNum) {}
    public static void onBytesSent(int amount, int networkType, int instanceNum) {}
    public static void onBytesReceived(int amount, int networkType, int instanceNum) {}
    public static void onRequestNewServerIpAndPort(int second, int instanceNum) {}
    public static void onProxyError() {}
    public static void getHostByName(String domainName, long ptr) {}
    public static int  getInitFlags() { return 0; }
    public static void onPremiumFloodWait(int instanceNum, int requestToken, boolean isUpload) {}
    public static void onIntegrityCheckClassic(int instanceNum, int requestToken, String project, String nonce) {}
    public static void onCaptchaCheck(int instanceNum, int requestToken, String action, String keyId) {}

    // ─── Convenience wrappers ─────────────────────────────────────────────────
    public int getConnectionState() {
        return native_getConnectionState(instanceNum);
    }

    public void setNetworkAvailable(boolean available, int networkType, boolean slow) {
        native_setNetworkAvailable(instanceNum, available, networkType, slow);
    }

    public void pauseNetwork()                     { native_pauseNetwork(instanceNum); }
    public void resumeNetwork(boolean partial)     { native_resumeNetwork(instanceNum, partial); }
    public void setUserId(long id)                 { native_setUserId(instanceNum, id); }
    public void cleanUp(boolean resetKeys)         { native_cleanUp(instanceNum, resetKeys); }
}
