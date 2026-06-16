package org.telegram.tgnet;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.util.HashMap;

public class ConnectionsManager {

    private static final String TAG = "AkroGram/CM";
    private static final int MAX_ACCOUNT_COUNT = 4;

    public static final int ConnectionStateConnecting        = 1;
    public static final int ConnectionStateWaitingForNetwork = 2;
    public static final int ConnectionStateConnected         = 3;
    public static final int ConnectionStateUpdating          = 4;

    public interface RequestDelegate {
        void run(TLObject response, TLRPC.TL_error error);
    }

    private static volatile ConnectionsManager[] instances = new ConnectionsManager[MAX_ACCOUNT_COUNT];
    private final int instanceNum;
    private static int lastToken = 1;
    private final HashMap<Integer, RequestDelegate> pendingRequests = new HashMap<>();

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

    public void init(Context context, int apiId, String apiHash,
                     boolean enablePush, long savedUserId) {
        try {
            native_setJava(true);
            native_init(
                instanceNum, 1, 176, apiId,
                Build.MODEL,
                Build.VERSION.RELEASE,
                "1.0", "ar", "ar",
                context.getFilesDir().getAbsolutePath(),
                context.getFilesDir().getAbsolutePath() + "/tgnet.log",
                "", "", "", context.getPackageName(),
                0, savedUserId, false,
                enablePush, true, 0, 1
            );
            Log.i(TAG, "✅ native_init OK");
        } catch (Throwable t) {
            Log.e(TAG, "❌ native_init failed: " + t.getMessage(), t);
        }
    }

    public int sendRequest(TLObject object, RequestDelegate cb) {
        int token;
        synchronized (this) { token = lastToken++; }
        try {
            NativeByteBuffer buffer = new NativeByteBuffer(object.getObjectSize());
            object.serializeToStream(buffer);
            pendingRequests.put(token, cb);
            native_sendRequest(instanceNum, buffer.address(), 0,
                Integer.MAX_VALUE, 1, true, token);
        } catch (Throwable t) {
            Log.e(TAG, "sendRequest failed: " + t.getMessage(), t);
            pendingRequests.remove(token);
            if (cb != null) {
                TLRPC.TL_error err = new TLRPC.TL_error();
                err.code = -1;
                err.text = t.getMessage();
                cb.run(null, err);
            }
        }
        return token;
    }

    // ─── Native callbacks ─────────────────────────────────────────────────────

    public static void onRequestComplete(int instanceNum, int token, long ptr,
            int errorCode, String errorText, int networkType,
            long responseTime, long msgId, int dcId) {
        ConnectionsManager cm = getInstance(instanceNum);
        RequestDelegate cb = cm.pendingRequests.remove(token);
        if (cb == null) return;
        if (errorText != null && !errorText.isEmpty()) {
            TLRPC.TL_error err = new TLRPC.TL_error();
            err.code = errorCode;
            err.text = errorText;
            cb.run(null, err);
        } else {
            cb.run(null, null); // TODO: deserialize ptr → TLObject
        }
    }

    public static void onRequestClear(int instanceNum, int token, boolean byTimeout) {
        getInstance(instanceNum).pendingRequests.remove(token);
    }

    public static void onRequestWriteToSocket(int instanceNum, int token) {}
    public static void onRequestQuickAck(int instanceNum, int token) {}
    public static void onUnparsedMessageReceived(long ptr, int instanceNum, long reqMsgId) {}
    public static void onUpdate(int instanceNum) {}
    public static void onSessionCreated(int instanceNum) {}
    public static void onLogout(int instanceNum) {}
    public static void onConnectionStateChanged(int state, int instanceNum) {
        Log.d(TAG, "connectionState=" + state + " instance=" + instanceNum);
    }
    public static void onInternalPushReceived(int instanceNum) {}
    public static void onUpdateConfig(long ptr, int instanceNum) {}
    public static void onBytesSent(int amount, int networkType, int instanceNum) {}
    public static void onBytesReceived(int amount, int networkType, int instanceNum) {}
    public static void onRequestNewServerIpAndPort(int second, int instanceNum) {}
    public static void onProxyError() {}
    public static void getHostByName(String domainName, long ptr) {
        native_onHostNameResolved(domainName, ptr, "");
    }
    public static int  getInitFlags() { return 0; }
    public static void onPremiumFloodWait(int instanceNum, int requestToken, boolean isUpload) {}
    public static void onIntegrityCheckClassic(int instanceNum, int requestToken, String project, String nonce) {
        native_receivedIntegrityCheckClassic(instanceNum, requestToken, nonce, "");
    }
    public static void onCaptchaCheck(int instanceNum, int requestToken, String action, String keyId) {}

    // ─── Convenience wrappers ─────────────────────────────────────────────────

    public int  getConnectionState()                              { return native_getConnectionState(instanceNum); }
    public void setNetworkAvailable(boolean v, int t, boolean s) { native_setNetworkAvailable(instanceNum, v, t, s); }
    public void pauseNetwork()                                    { native_pauseNetwork(instanceNum); }
    public void resumeNetwork(boolean partial)                    { native_resumeNetwork(instanceNum, partial); }
    public void setUserId(long id)                                { native_setUserId(instanceNum, id); }
    public void cleanUp(boolean resetKeys)                        { native_cleanUp(instanceNum, resetKeys); }

    // ─── Native methods ───────────────────────────────────────────────────────

    public static native void    native_setJava(boolean useJavaByteBuffers);
    public static native void    native_init(int instanceNum, int version, int layer, int apiId,
                                     String deviceModel, String systemVersion, String appVersion,
                                     String langCode, String systemLangCode,
                                     String configPath, String logPath,
                                     String regId, String cFingerprint,
                                     String installerId, String packageId,
                                     int timezoneOffset, long userId, boolean userPremium,
                                     boolean enablePushConnection, boolean hasNetwork,
                                     int networkType, int performanceClass);
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
}
