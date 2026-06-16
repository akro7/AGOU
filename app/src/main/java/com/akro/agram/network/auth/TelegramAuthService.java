package com.akro.agram.network.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

public class TelegramAuthService {

    private static final String TAG = "AkroGram/Auth";

    private final Context context;
    private final int apiId;
    private final String apiHash;
    private String phoneCodeHash;
    private String currentPhone;

    private static boolean cmInitialized = false;

    public TelegramAuthService(Context ctx, int apiId, String apiHash) {
        this.context = ctx.getApplicationContext();
        this.apiId   = apiId;
        this.apiHash = apiHash;
    }

    private void ensureConnectionsManager() {
        if (cmInitialized) return;
        try {
            ConnectionsManager.getInstance(0);
            cmInitialized = true;
            Log.i(TAG, "✅ ConnectionsManager initialized");
        } catch (Throwable t) {
            Log.e(TAG, "❌ CM init failed: " + t.getMessage());
        }
    }

    public void restoreState() {
        SharedPreferences prefs = context.getSharedPreferences("akrogram", Context.MODE_PRIVATE);
        phoneCodeHash = prefs.getString("phone_code_hash", null);
        currentPhone  = prefs.getString("current_phone", null);
    }

    public void sendCode(String phone, AuthCallback cb) {
        this.currentPhone = phone;
        ensureConnectionsManager();

        TLRPC.TL_auth_sendCode req = new TLRPC.TL_auth_sendCode();
        req.phone_number = phone;
        req.api_id       = apiId;
        req.api_hash     = apiHash;
        req.settings     = new TLRPC.TL_codeSettings();

        try {
            ConnectionsManager.getInstance(0).sendRequest(req, (response, error) -> {
                if (error != null) {
                    Log.e(TAG, "sendCode error: " + error.code + " " + error.text);
                    cb.onError(error.code, error.text);
                    return;
                }
                TLRPC.TL_auth_sentCode res = (TLRPC.TL_auth_sentCode) response;
                phoneCodeHash = res.phone_code_hash;
                context.getSharedPreferences("akrogram", Context.MODE_PRIVATE)
                    .edit()
                    .putString("phone_code_hash", phoneCodeHash)
                    .putString("current_phone", phone)
                    .apply();
                String type = res.type instanceof TLRPC.TL_auth_sentCodeTypeSms ? "SMS" : "APP";
                cb.onCodeSent(phone, type, res.type.length);
            });
        } catch (Throwable t) {
            Log.e(TAG, "sendRequest failed: " + t.getMessage());
            cb.onError(-1, t.getMessage());
        }
    }

    public void signIn(String code, AuthCallback cb) {
        ensureConnectionsManager();

        TLRPC.TL_auth_signIn req = new TLRPC.TL_auth_signIn();
        req.phone_number    = currentPhone;
        req.phone_code_hash = phoneCodeHash;
        req.phone_code      = code;

        try {
            ConnectionsManager.getInstance(0).sendRequest(req, (response, error) -> {
                if (error != null) {
                    if ("SESSION_PASSWORD_NEEDED".equals(error.text)) {
                        cb.onError(406, error.text);
                    } else {
                        cb.onError(error.code, error.text);
                    }
                    return;
                }
                if (response instanceof TLRPC.TL_auth_authorizationSignUpRequired) {
                    cb.onSignUpRequired();
                    return;
                }
                TLRPC.TL_auth_authorization auth = (TLRPC.TL_auth_authorization) response;
                TLRPC.User user = auth.user;
                context.getSharedPreferences("akrogram", Context.MODE_PRIVATE)
                    .edit().putLong("user_id", user.id).apply();
                cb.onAuthorized(user.id, user.first_name, user.username);
            });
        } catch (Throwable t) {
            Log.e(TAG, "signIn failed: " + t.getMessage());
            cb.onError(-1, t.getMessage());
        }
    }

    public void signUp(String firstName, String lastName, AuthCallback cb) {
        ensureConnectionsManager();

        TLRPC.TL_auth_signUp req = new TLRPC.TL_auth_signUp();
        req.phone_number    = currentPhone;
        req.phone_code_hash = phoneCodeHash;
        req.first_name      = firstName;
        req.last_name       = lastName;

        try {
            ConnectionsManager.getInstance(0).sendRequest(req, (response, error) -> {
                if (error != null) { cb.onError(error.code, error.text); return; }
                TLRPC.TL_auth_authorization auth = (TLRPC.TL_auth_authorization) response;
                TLRPC.User user = auth.user;
                context.getSharedPreferences("akrogram", Context.MODE_PRIVATE)
                    .edit().putLong("user_id", user.id).apply();
                cb.onAuthorized(user.id, user.first_name, user.username);
            });
        } catch (Throwable t) {
            Log.e(TAG, "signUp failed: " + t.getMessage());
            cb.onError(-1, t.getMessage());
        }
    }

    public interface AuthCallback {
        void onCodeSent(String phone, String codeType, int codeLength);
        void onAuthorized(long userId, String firstName, String username);
        void onSignUpRequired();
        void onError(int code, String message);
    }
}
